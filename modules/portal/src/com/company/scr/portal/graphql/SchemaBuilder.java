package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.entity.Entity;
import graphql.language.*;
import graphql.schema.idl.RuntimeWiring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchemaBuilder {

    static <T extends Entity> ObjectTypeDefinition buildQuery(Class<? extends T>... entityClasses) {

        List<FieldDefinition> fieldDefinitions = new ArrayList<>();

        Arrays.stream(entityClasses).forEach(aClass -> {

            String className = className(aClass);
//            String classLowerCase = className.toLowerCase();
            TypeName entityType = new TypeName(className);

            // cars: [Car]
            fieldDefinitions.add(new FieldDefinition(className + "s", new ListType(entityType)));

            FieldDefinition byIdQuery = FieldDefinition.newFieldDefinition()
                    .name(className + "ById").type(entityType)
                    .inputValueDefinition(new InputValueDefinition("id", new TypeName("String")))
                    .build();
            // carById(id: String): Car
            fieldDefinitions.add(byIdQuery);
        });

        ObjectTypeDefinition.Builder queryTypeBuilder = ObjectTypeDefinition
                .newObjectTypeDefinition()
                .name("Query")
                .fieldDefinitions(fieldDefinitions);

        return queryTypeBuilder.build();
    }

    public static void assignDataFetchers(RuntimeWiring.Builder rwBuilder,
                                                   CollectionDataFetcher collectionDataFetcher,
                                                   EntityDataFetcher entityDataFetcher,
                                                   Class<? extends Entity>... entityClasses) {

        Arrays.stream(entityClasses).forEach(aClass -> {
//            String classLowerCase = aClass.getSimpleName().toLowerCase();
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
