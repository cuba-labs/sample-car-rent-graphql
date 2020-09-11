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

            String className = aClass.getSimpleName();
            String classLowerCase = className.toLowerCase();
            TypeName entityType = new TypeName(className);

            // cars: [Car]
            fieldDefinitions.add(new FieldDefinition(classLowerCase + "s", new ListType(entityType)));

            FieldDefinition byIdQuery = FieldDefinition.newFieldDefinition()
                    .name(classLowerCase + "ById").type(entityType)
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

    public static RuntimeWiring assignDataFetchers(CollectionDataFetcher collectionDataFetcher,
                                                   EntityDataFetcher entityDataFetcher,
                                                   Class<? extends Entity>... entityClasses) {

        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
        Arrays.stream(entityClasses).forEach(aClass -> {
            String classLowerCase = aClass.getSimpleName().toLowerCase();
            runtimeWiringBuilder.type("Query", typeWiring -> typeWiring
                    .dataFetcher(classLowerCase + "s", collectionDataFetcher.loadEntities(aClass))
                    .dataFetcher(classLowerCase + "ById", entityDataFetcher.loadEntity(aClass))
            );
        });

        return runtimeWiringBuilder.build();
    }

}
