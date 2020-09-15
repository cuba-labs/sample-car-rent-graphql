package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.entity.Entity;
import graphql.schema.idl.RuntimeWiring;

import java.util.Arrays;

public class GraphQLSchemaUtils {

    public static void assignDataFetchers(RuntimeWiring.Builder rwBuilder,
                                                   CollectionDataFetcher collectionDataFetcher,
                                                   EntityDataFetcher entityDataFetcher,
                                                   Class<? extends Entity>... entityClasses) {

        Arrays.stream(entityClasses).forEach(aClass -> {
            String className = className(aClass);

            rwBuilder.type("Query", typeWiring -> typeWiring
                    .dataFetcher(className + "s", collectionDataFetcher.loadEntities(aClass))
                    .dataFetcher(className + "ById", entityDataFetcher.loadEntity(aClass))
            );
        });
    }

    private static String className(Class aClass) {
        return "scr_" + aClass.getSimpleName();
    }

}
