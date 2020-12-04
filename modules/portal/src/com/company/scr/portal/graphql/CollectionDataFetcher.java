package com.company.scr.portal.graphql;

import com.company.scr.service.GraphQLConstants;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Sort;
import graphql.schema.DataFetcher;
import org.junit.platform.commons.util.StringUtils;
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

            String sortOrder = environment.getArgument(GraphQLConstants.SORT_ORDER);
            String sortBy = environment.getArgument(GraphQLConstants.SORT_BY);

            if (StringUtils.isNotBlank(sortBy)) {
                Sort sort = StringUtils.isNotBlank(sortOrder)
                        ? Sort.by(Sort.Direction.valueOf(sortOrder), sortBy)
                        : Sort.by(sortBy);

                lc.setQuery(LoadContext.createQuery("").setSort(sort));
            }

            log.warn("loadList {} order by {} {}", lc, sortBy, sortOrder);
            return dataManager.loadList(lc);
        };
    }

    public DataFetcher countEntities(Class entityClass) {
        return environment -> {
            LoadContext<? extends Entity> lc = new LoadContext<>(entityClass);
            return dataManager.getCount(lc);
        };
    }

}