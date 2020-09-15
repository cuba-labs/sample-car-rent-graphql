package com.company.scr.service;

import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.Resources;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(GraphqlEntityTypesService.NAME)
public class GraphqlEntityTypesServiceBean implements GraphqlEntityTypesService {

    private final Logger log = LoggerFactory.getLogger(GraphqlEntityTypesServiceBean.class);

    @Inject
    Resources resources;
    @Inject
    private Persistence persistence;


    String entitySchema() {
        return persistence.callInTransaction(em -> {
            GraphQLSchema schema = new GraphQLSchemaBuilder(em.getDelegate()).build();
            return new SchemaPrinter().print(schema);
        });
    }

    @Override
    public String loadSchema() {
        return entitySchema();
//        return resources.getResourceAsString("com/company/scr/schema.graphql");
    }


}