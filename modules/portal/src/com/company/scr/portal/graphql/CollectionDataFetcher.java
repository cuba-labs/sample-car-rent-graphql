package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CollectionDataFetcher {

    private final Logger log = LoggerFactory.getLogger(CollectionDataFetcher.class);

    @Inject
    protected DataManager dataManager;

    public <E extends Entity> DataFetcher<List<E>> loadEntities(Class<E> entityClass) {
        return environment -> {

            // prepare view
            Map<String, GraphQLFieldDefinition> definitions = environment.getSelectionSet().getDefinitions();
            log.warn("definitions {}", definitions.keySet());

            List<String> properties = definitions.keySet().stream()
                    .map(def -> def.replaceAll("/", "."))
                    .collect(Collectors.toList());

            log.warn("properties {}", properties);

            View view = ViewBuilder
                    .of(entityClass)
                    .addView("_local")
                    .addAll(properties.toArray(new String[] {}))
                    .build();

            LoadContext<E> lc = new LoadContext<>(entityClass);
            lc.setView(view);

            log.warn("loadList {}", lc);
            return dataManager.loadList(lc);
        };
    }

}