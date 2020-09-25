package com.company.scr.service;

import graphql.Scalars;
import graphql.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.metamodel.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class GraphQLInputTypesBuilder extends GraphQLSchema.Builder {

    private final Logger log = LoggerFactory.getLogger(GraphQLInputTypesBuilder.class);

    protected final List<AttributeMapper> attributeMappers = new ArrayList<>();


    protected Stream<GraphQLInputObjectField> getInputObjectField(Attribute attribute) {

        return getAttributeType(attribute)
                .filter(Objects::nonNull)
                .map(type -> {
                    String name = attribute.getName();
                    return GraphQLInputObjectField.newInputObjectField()
                            .name(name)
                            .description(getSchemaDocumentation(attribute.getJavaMember()))
                            .type(type)
                            .build();
                });
    }

    private Stream<GraphQLInputType> getAttributeType(Attribute attribute) {
        if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
            try {
                return Stream.of(getBasicAttributeType(attribute.getJavaType()));
            } catch (UnsupportedOperationException e) {
                //fall through to the exception below
                //which is more useful because it also contains the declaring member
            }
//        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_MANY || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_MANY) {
//            EntityType foreignType = (EntityType) ((PluralAttribute) attribute).getElementType();
//            return Stream.of(new GraphQLList(new GraphQLTypeReference(convertToInputType(foreignType.getName()))));
//        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE) {
//            EntityType foreignType = (EntityType) ((SingularAttribute) attribute).getType();
//            String typeName = convertToInputType(foreignType.getName());
//            GraphQLTypeReference typeReference = new GraphQLTypeReference(typeName);
//            return Stream.of(typeReference);
//        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {
//            Type foreignType = ((PluralAttribute) attribute).getElementType();
//            return Stream.of(new GraphQLList(getTypeFromJavaType(foreignType.getJavaType())));
//        } else if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
//            EmbeddableType<?> embeddableType = (EmbeddableType<?>) ((SingularAttribute<?, ?>) attribute).getType();
//            return Stream.of(new GraphQLTypeReference(embeddableType.getJavaType().getSimpleName()));
        }

        // todo not sure how we will support relations in mutation
        if (attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_MANY
                || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_MANY
                || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE
                || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE) {
            return Stream.of((GraphQLInputType) null);
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

    private GraphQLInputType getBasicAttributeType(Class javaType) {
        // First check our 'standard' and 'customized' Attribute Mappers.  Use them if possible
        Optional<AttributeMapper> customMapper = attributeMappers.stream()
                .filter(it -> it.getBasicAttributeType(javaType).isPresent())
                .findFirst();

        if (customMapper.isPresent()) {
                return (GraphQLInputType) (customMapper.get().getBasicAttributeType(javaType).get());
        } else if (String.class.isAssignableFrom(javaType))
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
        else if (javaType.isEnum())
            // todo map enums to String for now
            return Scalars.GraphQLString;
//            return getTypeFromJavaType(javaType);
        else if (BigDecimal.class.isAssignableFrom(javaType)) {
            return Scalars.GraphQLBigDecimal;
        }

        throw new UnsupportedOperationException(
                "Class could not be mapped to GraphQL: '" + javaType.getClass().getTypeName() + "'");
    }

    protected static String convertType(String name) {
        return name.replaceAll("\\$", "_");
    }

    protected String getSchemaDocumentation(Object o) {
        return null;
    }

}
