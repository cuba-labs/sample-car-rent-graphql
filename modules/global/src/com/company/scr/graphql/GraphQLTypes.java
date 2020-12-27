package com.company.scr.graphql;

import graphql.schema.*;

public class GraphQLTypes {

    public static final GraphQLTypeReference TYPE_REFERENCE_STRING = GraphQLTypeReference.typeRef("String");

    public static GraphQLInputObjectType Condition = GraphQLInputObjectType.newInputObject()
            .name("Condition")
            .field(inpStringField("property"))
            .field(inpStringField("operator"))
            .field(inpStringField("value"))
            .build();

    public static GraphQLEnumType GroupConditionType = GraphQLEnumType.newEnum()
            .name("GroupConditionType").value("AND").value("OR").build();

    public static GraphQLInputObjectType GroupCondition = GraphQLInputObjectType.newInputObject()
            .name("GroupCondition")
            .field(field("conditions", GraphQLList.list(Condition)))
            .field(field("groupConditions", GraphQLList.list(GraphQLTypeReference.typeRef("GroupCondition"))))
            .field(field("group", GroupConditionType))
            .build();

    /**
     * Shortcut for type field builder
     *
     * @param fieldName field name
     * @return field
     */
    public static GraphQLInputObjectField.Builder field(String fieldName, GraphQLInputType type) {
        return GraphQLInputObjectField.newInputObjectField().name(fieldName).type(type);
    }

    /**
     * Shortcut for input type string field builder
     *
     * @param fieldName field name
     * @return field
     */
    public static GraphQLInputObjectField.Builder inpStringField(String fieldName) {
        return field(fieldName, TYPE_REFERENCE_STRING);
    }

    /**
     * Shortcut for output type string field builder
     *
     * @param fieldName field name
     * @return field
     */
    public static GraphQLFieldDefinition.Builder outStringField(String fieldName) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(fieldName)
                .type(TYPE_REFERENCE_STRING);
    }

}
