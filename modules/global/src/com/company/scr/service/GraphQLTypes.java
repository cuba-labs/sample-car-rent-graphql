package com.company.scr.service;

import graphql.schema.*;

public class GraphQLTypes {

    public static GraphQLEnumType SortOrder = GraphQLEnumType.newEnum()
            .name("SortOrder").value("ASC").value("DESC").build();

    public static GraphQLInputObjectType Condition = GraphQLInputObjectType.newInputObject()
            .name("Condition")
            .field(stringField("property"))
            .field(stringField("operator"))
            .field(stringField("value"))
            .build();

    public static GraphQLInputObjectType Filter = GraphQLInputObjectType.newInputObject()
            .name("Filter")
            .field(GraphQLInputObjectField.newInputObjectField()
                    .name("conditions")
                    .type(GraphQLList.list(Condition)))
            .build();

    /**
     * Shortcut for type string field builder
     *
     * @param fieldName field name
     * @return field
     */
    public static GraphQLInputObjectField.Builder stringField(String fieldName) {
        return GraphQLInputObjectField.newInputObjectField().name(fieldName).type(GraphQLTypeReference.typeRef("String"));
    }
}
