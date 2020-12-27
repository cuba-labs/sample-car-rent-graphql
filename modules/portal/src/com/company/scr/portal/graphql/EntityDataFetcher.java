package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.UUID;

@Component
public class EntityDataFetcher {

    private final Logger log = LoggerFactory.getLogger(EntityDataFetcher.class);

    @Inject
    protected DataManager dataManager;

    public <E extends Entity> DataFetcher<E> loadEntity(Class<E> entityClass) {
        return environment -> {

            String id = environment.getArgument("id");
            log.warn("id {}", id);

            LoadContext<E> lc = new LoadContext<>(entityClass);
            // todo support not only UUID types of id
            lc.setId(UUID.fromString(id));
            lc.setView(DataFetcherUtils.buildView(entityClass, environment));

            log.warn("loadEntity {}", lc);
            return dataManager.load(lc);
        };
    }

}