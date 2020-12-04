package com.company.scr.service;

import graphql.schema.GraphQLEnumType;

public class GraphQLTypes {

    public static GraphQLEnumType SortOrder = GraphQLEnumType.newEnum()
            .name("SortOrder").value("ASC").value("DESC").build();
}
