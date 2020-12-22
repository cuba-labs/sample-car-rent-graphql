package com.company.scr.service;

import com.company.scr.graphql.GraphQLConstants;
import com.company.scr.graphql.GraphQLNamingUtils;
import com.company.scr.graphql.GraphQLTypes;
import com.company.scr.graphql.JavaScalars;
import com.haulmont.chile.core.datatypes.impl.EnumClass;
import com.haulmont.cuba.core.entity.Entity;
import graphql.Scalars;
import graphql.schema.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.*;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphQLSchemaBuilder extends GraphQLInputTypesBuilder {

    private final Logger log = LoggerFactory.getLogger(GraphQLSchemaBuilder.class);

    private final Map<EntityType, GraphQLObjectType> entityCache = new HashMap<>();
    private final Map<Class, GraphQLType> classCache = new HashMap<>();
    private final Map<Class, GraphQLInputType> inputClassCache = new HashMap<>();

    /**
     * Initialises the builder with the given {@link EntityManager} from which we immediately start to scan for
     * entities to include in the GraphQL schema.
     *
     * @param entityManager The manager containing the data models to include in the final GraphQL schema.
     */
    public GraphQLSchemaBuilder(EntityManager entityManager, List<Class<? extends Entity>> classes) {
        populateStandardAttributeMappers();

        // enums
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(EnumClass.class));
        // todo now we search enums only in 'company' package
        provider.findCandidateComponents(ENUM_CLASS_BASE_PACKAGE)
                .forEach(this::buildEnumType);
        super.additionalTypes(new HashSet<>(enumsCache.values()));

        // jpa model
        entityManager.getMetamodel().getEntities().stream()
                .filter(this::isNotIgnored)
                .forEach(this::buildObjectType);
        super.additionalTypes(new HashSet<>(entityCache.values()));

        // add input types to schema
        super.additionalTypes(new HashSet<>(inputClassCache.values()));

        // filter
        super.additionalType(GraphQLTypes.Condition);
        super.additionalType(GraphQLTypes.GroupConditionType);
        super.additionalType(GraphQLTypes.GroupCondition);

        // build query and add to schema
        super.query(buildQuerySection(classes));

        // build mutation and add to schema
        super.mutation(buildMutationSection(classes));
    }

    /**
     * Attribute mappers are used when we serialize java classes for gql query output.
     */
    private void populateStandardAttributeMappers() {
        attributeMappers.add(createStandardAttributeMapper(UUID.class, JavaScalars.GraphQLUUID));
        attributeMappers.add(createStandardAttributeMapper(Long.class, JavaScalars.GraphQLLong));
        attributeMappers.add(createStandardAttributeMapper(BigDecimal.class, JavaScalars.GraphQLBigDecimal));
        attributeMappers.add(createStandardAttributeMapper(Date.class, JavaScalars.GraphQLDate));
        attributeMappers.add(createStandardAttributeMapper(LocalDateTime.class, JavaScalars.GraphQLLocalDateTime));
        attributeMappers.add(createStandardAttributeMapper(LocalDateTime.class, JavaScalars.GraphQLVoid));
    }

    private AttributeMapper createStandardAttributeMapper(final Class<?> assignableClass, final GraphQLInputType type) {
        return (javaType) -> {
            if (assignableClass.isAssignableFrom(javaType))
                return Optional.of(type);
            return Optional.empty();
        };
    }

    GraphQLObjectType buildMutationSection(List<Class<? extends Entity>> classes) {
        GraphQLObjectType.Builder qtBuilder = GraphQLObjectType.newObject().name("Mutation");
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        classes.forEach(aClass -> {
            GraphQLInputType inputType = inputClassCache.get(aClass);
            GraphQLOutputType outType = (GraphQLOutputType) classCache.get(aClass);

            GraphQLArgument createEntityArgument = GraphQLArgument.newArgument()
                    .name(GraphQLNamingUtils.uncapitalizedSimpleName(aClass))
                    .type(GraphQLNonNull.nonNull(inputType))
                    .build();

            // mutation createCar(car: scr_Car)
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name("create" + aClass.getSimpleName())
                            .type(outType)
                            .argument(createEntityArgument)
                            .build());

            GraphQLArgument deleteEntityArgument = GraphQLArgument.newArgument()
                    .name("id")
                    .type(GraphQLNonNull.nonNull(new GraphQLTypeReference("UUID")))
                    .build();

            // mutation deleteCar(id: UUID)
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name("delete" + aClass.getSimpleName())
                            .type(JavaScalars.GraphQLVoid)
                            .argument(deleteEntityArgument)
                            .build());
        });

        qtBuilder.fields(fields);
        return qtBuilder.build();
    }

    GraphQLObjectType buildQuerySection(List<Class<? extends Entity>> classes) {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("Query")
                .description("All encompassing schema for this JPA environment");

        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        classes.forEach(aClass -> {
            GraphQLType type = classCache.get(aClass);

            // query 'cars(filter, limit, offset, sortBy, sortOrder)'
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name(GraphQLNamingUtils.composeListQueryName(aClass))
                            .type(new GraphQLList(type))
                            .argument(GraphQLArgument.newArgument()
                                    .name(GraphQLConstants.FILTER).type(GraphQLTypes.GroupCondition))
                            .argument(arg(GraphQLConstants.LIMIT, "Int"))
                            .argument(arg(GraphQLConstants.OFFSET, "Int"))
                            .argument(arg(GraphQLConstants.SORT, "String"))
                            .build());

            // query 'carById(id)'
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name(GraphQLNamingUtils.composeByIdQueryName(aClass))
                            .type(new GraphQLTypeReference("scr_" + aClass.getSimpleName()))
                            .argument(GraphQLArgument.newArgument().name("id").type(Scalars.GraphQLString))
                            .build());

            // query 'countCars()'
            fields.add(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name(GraphQLNamingUtils.composeCountQueryName(aClass))
                            .type(JavaScalars.GraphQLLong)
                            .build());

        });

        queryType.fields(fields);
        return queryType.build();
    }

    private void buildEnumType(BeanDefinition beanDefinition)  {
        String enumClassName = beanDefinition.getBeanClassName();
        String enumName = StringUtils.substringAfterLast(enumClassName, ".");
        try {
            Class<?> enumClass = Class.forName(enumClassName);
            EnumClass[] enumValues = (EnumClass[]) enumClass.getDeclaredMethod("values").invoke(null);
            log.warn("buildEnumType: values {}", enumClassName, Arrays.toString(enumValues));

            List<GraphQLEnumValueDefinition> enumValueDefs = Arrays.stream(enumValues).map(eV -> {
                Object id = eV.getId();
                String name = ((Enum) eV).name();
                return GraphQLEnumValueDefinition.newEnumValueDefinition().name(name).value(id).build();
            }).collect(Collectors.toList());

            GraphQLEnumType enumType = GraphQLEnumType.newEnum()
                    .name(enumName)
                    .values(enumValueDefs)
                    .build();

            enumsCache.put(enumClass, enumType);

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new UnsupportedOperationException(
                "Enum type build failed - can't find enum class " + enumClassName);
        }
    }

    void buildObjectType(EntityType<?> entityType) {
        GraphQLObjectType outAnswer = GraphQLObjectType.newObject()
                .name(entityType.getName().replaceAll("\\$", "_"))
                .fields(entityType.getAttributes().stream().filter(this::isNotIgnored).flatMap(this::getObjectField)
                        .collect(Collectors.toList())).build();

        GraphQLInputObjectType inputType = buildInputType(entityType);

        entityCache.put(entityType, outAnswer);
        classCache.put(entityType.getJavaType(), outAnswer);
        inputClassCache.put(entityType.getJavaType(), inputType);
    }

    private Stream<GraphQLFieldDefinition> getObjectField(Attribute attribute) {
        return getAttributeType(attribute)
                .filter(type -> type instanceof GraphQLOutputType)
                .map(type -> {
                    String name = attribute.getName();
                    return GraphQLFieldDefinition.newFieldDefinition()
                            .name(name)
                            .description(getSchemaDocumentation(attribute.getJavaMember()))
                            .type((GraphQLOutputType) type)
                            .build();
                });
    }

//    private Stream<Attribute> findBasicAttributes(Collection<Attribute> attributes) {
//        return attributes.stream().filter(this::isNotIgnored).filter(it -> it.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC);
//    }

    private Stream<GraphQLType> getAttributeType(Attribute attribute) {
        Class attrJavaType = attribute.getDeclaringType().getJavaType();
        if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
            try {
                // enums - we need to check getters and setters, but not field types
                PropertyDescriptor pd = new PropertyDescriptor(attribute.getName(), attrJavaType);
                Class<?> propertyType = pd.getPropertyType();
                // todo now we are working with enums located in 'company' package only
                if (EnumClass.class.isAssignableFrom(propertyType)
                        && attrJavaType.getCanonicalName().contains(ENUM_CLASS_BASE_PACKAGE)) {
                    return Stream.of(getEnumAttributeType(propertyType));
                }

                // other simple types
                return Stream.of(getBasicAttributeType(attribute.getJavaType()));

            // todo refactor, do not swallow exception
            } catch (UnsupportedOperationException | IntrospectionException e) {
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
            return Stream.of(new GraphQLList(getBasicAttributeType(foreignType.getJavaType())));
        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
            EmbeddableType<?> embeddableType = (EmbeddableType<?>) ((SingularAttribute<?, ?>) attribute).getType();
            return Stream.of(new GraphQLTypeReference(embeddableType.getJavaType().getSimpleName()));
        }

//        // todo temporary map all unsupported to String
        Class classType = attrJavaType;
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
        else if (Boolean.class.isAssignableFrom(javaType) || boolean.class.isAssignableFrom(javaType)) {
            return Scalars.GraphQLBoolean;
        }

        throw new UnsupportedOperationException(
                "Class could not be mapped to GraphQL: '" + javaType.getClass().getTypeName() + "'");
    }

    private boolean isNotIgnored(EntityType entityType) {
        if (convertType(entityType.getName()).startsWith("sys_")) {
            return false;
        }
        return true;
    }

    /**
     * Shortcut for query argument builder
     * @param name argument name
     * @param typeRef argument type reference as string
     * @return argument
     */
    private static GraphQLArgument.Builder arg(String name, String typeRef) {
        return GraphQLArgument.newArgument()
                .name(name).type(GraphQLTypeReference.typeRef(typeRef));
    }

}