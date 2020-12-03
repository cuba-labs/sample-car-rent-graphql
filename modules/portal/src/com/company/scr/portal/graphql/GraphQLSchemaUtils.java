package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.entity.Entity;
import graphql.schema.idl.RuntimeWiring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GraphQLSchemaUtils {

    public static void assignDataFetchers(RuntimeWiring.Builder rwBuilder,
                                                   CollectionDataFetcher collectionDataFetcher,
                                                   EntityDataFetcher entityDataFetcher,
                                                   EntityMutationResolver entityMutationResolver,
                                                   Class<Entity>... entityClasses) {

        Arrays.stream(entityClasses).forEach(aClass -> {
            String className = className(aClass);

            rwBuilder.type("Query", typeWiring -> typeWiring
                    .dataFetcher(className + "s", collectionDataFetcher.loadEntities(aClass))
                    .dataFetcher(className + "ById", entityDataFetcher.loadEntity(aClass))
                    .dataFetcher("count" + aClass.getSimpleName() + "s", collectionDataFetcher.countEntities(aClass))
            );

            rwBuilder.type("Mutation", typeWiring -> typeWiring
                    .dataFetcher("create" + aClass.getSimpleName(), entityMutationResolver.createEntity(aClass))
            );

            rwBuilder.type("Mutation", typeWiring -> typeWiring
                    .dataFetcher("delete" + aClass.getSimpleName(), entityMutationResolver.deleteEntity(aClass))
            );

        });
    }

    private static String className(Class aClass) {
        return aClass.getSimpleName().toLowerCase();
    }

    public static Object printEntityView(EntityImportView view) {
        if (view == null) {
            return "";
        }

        Map<String, Object> map = new HashMap<>();
        view.getProperties().forEach(prop -> map.put(prop.getName(), printEntityView(prop.getView())));
        return map;
    }

}
