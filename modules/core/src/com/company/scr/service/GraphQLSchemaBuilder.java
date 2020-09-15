package com.company.scr.service;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.ReferenceToEntity;
import graphql.Scalars;
import graphql.schema.*;
import org.crygier.graphql.IdentityCoercing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphQLSchemaBuilder extends GraphQLSchema.Builder {

    private final Logger log = LoggerFactory.getLogger(GraphQLSchemaBuilder.class);

    private final EntityManager entityManager;
    private final Map<EntityType, GraphQLObjectType> entityCache = new HashMap<>();
    private final Map<Class, GraphQLType> classCache = new HashMap<>();

    private final List<AttributeMapper> attributeMappers = new ArrayList<>();

    /**
     * Initialises the builder with the given {@link EntityManager} from which we immediately start to scan for
     * entities to include in the GraphQL schema.
     *
     * @param entityManager The manager containing the data models to include in the final GraphQL schema.
     */
    public GraphQLSchemaBuilder(EntityManager entityManager, List<Class<? extends Entity>> classes) {
        this.entityManager = entityManager;
        populateStandardAttributeMappers();

        super.query(getQueryType(classes));

        // add types to schema
        entityCache.forEach((entityType, graphQLObjectType) -> super.additionalType(graphQLObjectType));
        super.additionalTypes(new HashSet<>(entityCache.values()));
    }

    private void populateStandardAttributeMappers() {
        attributeMappers.add(createStandardAttributeMapper(UUID.class, JavaScalars.GraphQLUUID));
        attributeMappers.add(createStandardAttributeMapper(Date.class, JavaScalars.GraphQLDate));
        attributeMappers.add(createStandardAttributeMapper(LocalDateTime.class, JavaScalars.GraphQLLocalDateTime));
        attributeMappers.add(createStandardAttributeMapper(Instant.class, JavaScalars.GraphQLInstant));
        attributeMappers.add(createStandardAttributeMapper(LocalDate.class, JavaScalars.GraphQLLocalDate));
    }

    private AttributeMapper createStandardAttributeMapper(final Class<?> assignableClass, final GraphQLType type) {
        return (javaType) -> {
            if (assignableClass.isAssignableFrom(javaType))
                return Optional.of(type);
            return Optional.empty();
        };
    }


    GraphQLObjectType getQueryType(List<Class<? extends Entity>> classes) {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("Query").description("All encompassing schema for this JPA environment");

        // todo rework
        List<GraphQLFieldDefinition> fieldsList = entityManager.getMetamodel().getEntities().stream()
                .filter(this::isNotIgnored)
                .map(this::getQueryFieldDefinition)
                .collect(Collectors.toList());

        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        classes.forEach(aClass -> {
            EntityType entityType = entityManager.getMetamodel().getEntities().stream().filter(et -> et.getJavaType().equals(aClass)).findAny().get();
            GraphQLType type = entityCache.get(entityType);

            // query scr_Cars
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name("scr_" + aClass.getSimpleName() + "s")
                            .type(new GraphQLList(type))
                            .build());

            // query scr_CarById(id)
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name("scr_" + aClass.getSimpleName() + "ById")
                            .type(new GraphQLTypeReference("scr_" + aClass.getSimpleName()))
                            .argument(GraphQLArgument.newArgument().name("id").type(Scalars.GraphQLString).build())
                            .build());

        });

        queryType.fields(fields);
        return queryType.build();
    }

    GraphQLFieldDefinition getQueryFieldDefinition(EntityType<?> entityType) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(convertType(entityType.getName()))
//                .description(getSchemaDocumentation(entityType.getJavaType()))
                .type(new GraphQLList(getObjectType(entityType)))
//                .dataFetcher(new JpaDataFetcher(entityManager, entityType))
//                .argument(entityType.getAttributes().stream().filter(this::isValidInput).filter(this::isNotIgnored).flatMap(this::getArgument).collect(Collectors.toList()))
                .build();
    }

    private boolean isValidInput(Attribute attribute) {
        return attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC ||
                attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ELEMENT_COLLECTION ||
                attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED;
    }

    private Stream<GraphQLArgument> getArgument(Attribute attribute) {
        return getAttributeType(attribute)
                .filter(type -> type instanceof GraphQLInputType)
                .filter(type -> attribute.getPersistentAttributeType() != Attribute.PersistentAttributeType.EMBEDDED ||
                        (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED && type instanceof GraphQLScalarType))
                .map(type -> {
                    String name = attribute.getName();

                    return GraphQLArgument.newArgument()
                            .name(name)
                            .type((GraphQLInputType) type)
                            .build();
                });
    }

    GraphQLObjectType getObjectType(EntityType<?> entityType) {
        if (entityCache.containsKey(entityType))
            return entityCache.get(entityType);

        GraphQLObjectType answer = GraphQLObjectType.newObject()
                .name(entityType.getName().replaceAll("\\$", "_"))
//                .description(getSchemaDocumentation(entityType.getJavaType()))
                .fields(entityType.getAttributes().stream().filter(this::isNotIgnored).flatMap(this::getObjectField).collect(Collectors.toList()))
                .build();

        entityCache.put(entityType, answer);

        return answer;
    }

    private Stream<GraphQLFieldDefinition> getObjectField(Attribute attribute) {
        return getAttributeType(attribute)
                .filter(type -> type instanceof GraphQLOutputType)
                .map(type -> {
                    List<GraphQLArgument> arguments = new ArrayList<>();
                    arguments.add(GraphQLArgument.newArgument().name("orderBy").type(orderByDirectionEnum).build());            // Always add the orderBy argument

                    // Get the fields that can be queried on (i.e. Simple Types, no Sub-Objects)
                    if (attribute instanceof SingularAttribute
                            && attribute.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC) {
                        ManagedType foreignType = (ManagedType) ((SingularAttribute) attribute).getType();

                        Stream<Attribute> attributes = findBasicAttributes(foreignType.getAttributes());

                        attributes.forEach(it -> {
                            arguments.add(GraphQLArgument.newArgument()
                                    .name(it.getName())
                                    .type((GraphQLInputType) getAttributeType(it).findFirst().get())
                                    .build());
                        });
                    }

                    String name = attribute.getName();


                    return GraphQLFieldDefinition.newFieldDefinition()
                            .name(name)
                            .description(getSchemaDocumentation(attribute.getJavaMember()))
                            .type((GraphQLOutputType) type)
                            .argument(arguments)
                            .build();
                });
    }

    private Stream<Attribute> findBasicAttributes(Collection<Attribute> attributes) {
        return attributes.stream().filter(this::isNotIgnored).filter(it -> it.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC);
    }

    private Stream<GraphQLType> getAttributeType(Attribute attribute) {
        if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
            try {
                return Stream.of(getBasicAttributeType(attribute.getJavaType()));
            } catch (UnsupportedOperationException e) {
                //fall through to the exception below
                //which is more useful because it also contains the declaring member
            }
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_MANY || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_MANY) {
            EntityType foreignType = (EntityType) ((PluralAttribute) attribute).getElementType();
            return Stream.of(new GraphQLList(new GraphQLTypeReference(convertType(foreignType.getName()))));
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE) {
            EntityType foreignType = (EntityType) ((SingularAttribute) attribute).getType();
            String typeName = convertType(foreignType.getName());
            GraphQLTypeReference typeReference = new GraphQLTypeReference(typeName);
            return Stream.of(typeReference);
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {
            Type foreignType = ((PluralAttribute) attribute).getElementType();
            return Stream.of(new GraphQLList(getTypeFromJavaType(foreignType.getJavaType())));
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
            EmbeddableType<?> embeddableType = (EmbeddableType<?>) ((SingularAttribute<?, ?>) attribute).getType();
            return Stream.of(new GraphQLTypeReference(embeddableType.getJavaType().getSimpleName()));
        }

//        // todo temporary map all unsupported to String
        Class classType = attribute.getDeclaringType().getJavaType();
        Class attrType = attribute.getJavaType();
        log.warn("attribute {} from class {} has unsupported type {}, temporary map to String", attribute.getName(), classType, attrType);
        return Stream.of(new GraphQLTypeReference("String"));

//        final String declaringType = attribute.getDeclaringType().getJavaType().getName(); // fully qualified name of the entity class
//        final String declaringMember = attribute.getJavaMember().getName(); // field name in the entity class
//
//        throw new UnsupportedOperationException(
//                "Attribute could not be mapped to GraphQL: field '" + declaringMember + "' of entity class '" + declaringType + "'");
    }

    private GraphQLType getBasicAttributeType(Class javaType) {
        // First check our 'standard' and 'customized' Attribute Mappers.  Use them if possible
        Optional<AttributeMapper> customMapper = attributeMappers.stream()
                .filter(it -> it.getBasicAttributeType(javaType).isPresent())
                .findFirst();

        if (customMapper.isPresent())
            return customMapper.get().getBasicAttributeType(javaType).get();
        else if (String.class.isAssignableFrom(javaType))
            return Scalars.GraphQLString;
        else if (Integer.class.isAssignableFrom(javaType) || int.class.isAssignableFrom(javaType))
            return Scalars.GraphQLInt;
        else if (Short.class.isAssignableFrom(javaType) || short.class.isAssignableFrom(javaType))
            return Scalars.GraphQLShort;
        else if (Float.class.isAssignableFrom(javaType) || float.class.isAssignableFrom(javaType)
                || Double.class.isAssignableFrom(javaType) || double.class.isAssignableFrom(javaType))
            return Scalars.GraphQLFloat;
        else if (Long.class.isAssignableFrom(javaType) || long.class.isAssignableFrom(javaType))
            return Scalars.GraphQLLong;
        else if (Boolean.class.isAssignableFrom(javaType) || boolean.class.isAssignableFrom(javaType))
            return Scalars.GraphQLBoolean;
        else if (javaType.isEnum()) {
            return getTypeFromJavaType(javaType);
        } else if (BigDecimal.class.isAssignableFrom(javaType)) {
            return Scalars.GraphQLBigDecimal;
        }

        throw new UnsupportedOperationException(
                "Class could not be mapped to GraphQL: '" + javaType.getClass().getTypeName() + "'");
    }

    private GraphQLType getTypeFromJavaType(Class clazz) {
        if (clazz.isEnum()) {
            if (classCache.containsKey(clazz))
                return classCache.get(clazz);

            GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(clazz.getSimpleName());
            int ordinal = 0;
            for (Enum enumValue : ((Class<Enum>) clazz).getEnumConstants())
                enumBuilder.value(enumValue.name(), ordinal++);

            GraphQLType answer = enumBuilder.build();
            setIdentityCoercing(answer);

            classCache.put(clazz, answer);

            return answer;
        }

        return getBasicAttributeType(clazz);
    }

    /**
     * A bit of a hack, since JPA will deserialize our Enum's for us...we don't want GraphQL doing it.
     *
     * @param type
     */
    private void setIdentityCoercing(GraphQLType type) {
        try {
            Field coercing = type.getClass().getDeclaredField("coercing");
            coercing.setAccessible(true);
            coercing.set(type, new IdentityCoercing());
        } catch (Exception e) {
            log.error("Unable to set coercing for " + type, e);
        }
    }


    private boolean isNotIgnored(Attribute attribute) {
        Class javaType = attribute.getJavaType();
        String attrName = attribute.getName();
//        if (attrName.equals("content") || attrName.equals("tokenBytes") || attrName.equals("authenticationBytes")) {
//            log.warn("isNotIgnored return false for attribute {}:{}", attrName, javaType);
//            return false;
//        }
//
        if (
//                LocalTime.class.isAssignableFrom(javaType) ||
//                || OffsetDateTime.class.isAssignableFrom(javaType) ||
                ReferenceToEntity.class.isAssignableFrom(javaType)) {
            log.warn("isNotIgnored return false for attribute {}:{}", attrName, javaType);
            return false;
        }
        return true; // isNotIgnored(entityType.getJavaType());
    }

    private boolean isNotIgnored(EntityType entityType) {
//        log.warn("isNotIgnored entity type {}", entityType.getName());
        return true; //isNotIgnored(entityType.getJavaType());
    }

    private String getSchemaDocumentation(Object o) {
        return null;
    }

    private static final GraphQLEnumType orderByDirectionEnum =
            GraphQLEnumType.newEnum()
                    .name("OrderByDirection")
                    .description("Describes the direction (Ascending / Descending) to sort a field.")
                    .value("ASC", 0, "Ascending")
                    .value("DESC", 1, "Descending")
                    .build();

    private static String convertType(String name) {
        return name.replaceAll("\\$", "_");
    }
}