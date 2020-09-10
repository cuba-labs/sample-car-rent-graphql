package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import graphql.schema.GraphQLFieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataFetcherUtils {

    private final static Logger log = LoggerFactory.getLogger(DataFetcherUtils.class);

    public static <E extends Entity> View buildView(Class<E> entityClass, graphql.schema.DataFetchingEnvironment environment) {
        Map<String, GraphQLFieldDefinition> definitions = environment.getSelectionSet().getDefinitions();
        log.warn("definitions {}", definitions.keySet());

        List<String> properties = definitions.keySet().stream()
                .map(def -> def.replaceAll("/", "."))
                .collect(Collectors.toList());

        log.warn("properties {}", properties);

        return ViewBuilder
                .of(entityClass)
                .addView("_local")
                .addAll(properties.toArray(new String[] {}))
                .build();
    }


}
