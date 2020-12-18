package com.company.scr.graphql;

import graphql.schema.*;

public class GraphQLTypes {

    public static GraphQLInputObjectType Condition = GraphQLInputObjectType.newInputObject()
            .name("Condition")
            .field(stringField("property"))
            .field(stringField("operator"))
            .field(stringField("value"))
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
     * Shortcut for type string field builder
     *
     * @param fieldName field name
     * @return field
     */
    public static GraphQLInputObjectField.Builder stringField(String fieldName) {
        return field(fieldName, GraphQLTypeReference.typeRef("String"));
    }

}
