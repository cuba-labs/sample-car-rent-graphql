package com.company.scr.service;

import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Resources;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(GraphQLService.NAME)
public class GraphQLServiceBean implements GraphQLService {

    private final Logger log = LoggerFactory.getLogger(GraphQLServiceBean.class);

    @Inject
    Resources resources;
    @Inject
    private Persistence persistence;


    String entitySchema(List<Class<? extends Entity>> classes) {

        return persistence.callInTransaction(em -> {
            GraphQLSchema schema = new GraphQLSchemaBuilder(em.getDelegate(), classes).build();
            return new SchemaPrinter().print(schema);
        });
    }

    @Override
    public String loadSchema(List<Class<? extends Entity>> classes) {
        return entitySchema(classes);
//        return resources.getResourceAsString("com/company/scr/schema.graphql");
    }


}