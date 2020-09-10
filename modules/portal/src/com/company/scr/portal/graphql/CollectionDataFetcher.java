package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class CollectionDataFetcher {

    private final Logger log = LoggerFactory.getLogger(CollectionDataFetcher.class);

    @Inject
    protected DataManager dataManager;

    public <E extends Entity> DataFetcher<List<E>> loadEntities(Class<E> entityClass) {
        return environment -> {

            LoadContext<E> lc = new LoadContext<>(entityClass);
            lc.setView(DataFetcherUtils.buildView(entityClass,environment));

            log.warn("loadList {}", lc);
            return dataManager.loadList(lc);
        };
    }

}