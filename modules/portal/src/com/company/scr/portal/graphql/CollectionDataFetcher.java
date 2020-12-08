package com.company.scr.portal.graphql;

import com.company.scr.service.GraphQLConstants;
import com.google.gson.Gson;
import com.haulmont.addon.restapi.api.service.filter.RestFilterParseException;
import com.haulmont.addon.restapi.api.service.filter.RestFilterParseResult;
import com.haulmont.addon.restapi.api.service.filter.RestFilterParser;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Sort;
import graphql.schema.DataFetcher;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Component
public class CollectionDataFetcher {

    private final Logger log = LoggerFactory.getLogger(CollectionDataFetcher.class);

    @Inject
    protected DataManager dataManager;
    @Inject
    private RestFilterParser restFilterParser;
    @Inject
    private Metadata metadata;

    public <E extends Entity> DataFetcher<List<E>> loadEntities(Class<E> entityClass) {
        return environment -> {

            LoadContext<E> lc = new LoadContext<>(entityClass);
            lc.setView(DataFetcherUtils.buildView(entityClass, environment));

            String sortOrder = environment.getArgument(GraphQLConstants.SORT_ORDER);
            String sortBy = environment.getArgument(GraphQLConstants.SORT_BY);

            Integer limit = environment.getArgument(GraphQLConstants.LIMIT);
            Integer offset = environment.getArgument(GraphQLConstants.OFFSET);

            MetaClass metadataClass = metadata.getClass(entityClass);
            String queryString = "select e from " + metadataClass.getName() + " e";

            Map<String, Object> queryParameters = null;
            // todo implement graphql type mapper that leave filter as plain string (not convert to Map) or returns RestFilterParseResult
            Object filterArg = environment.getArgument(GraphQLConstants.FILTER);
            if (filterArg != null) {
                RestFilterParseResult filterParseResult;
                try {
                    filterParseResult = restFilterParser.parse(new Gson().toJson(filterArg), metadataClass);
                } catch (RestFilterParseException e) {
                    throw new UnsupportedOperationException("Cannot parse entities filter" + e.getMessage(), e);
                }

                String jpqlWhere = filterParseResult.getJpqlWhere();
                queryParameters = filterParseResult.getQueryParameters();

                if (jpqlWhere != null) {
                    queryString += " where " + jpqlWhere.replace("{E}", "e");
                }
            }

            LoadContext.Query query = LoadContext.createQuery(queryString);
            if (queryParameters != null) {
                query.setParameters(queryParameters);
            }

            if (StringUtils.isNotBlank(sortBy)) {
                Sort sort = StringUtils.isNotBlank(sortOrder)
                        ? Sort.by(Sort.Direction.valueOf(sortOrder), sortBy)
                        : Sort.by(sortBy);
                query.setSort(sort);
            }
            lc.setQuery(query
                    .setFirstResult(offset != null ? offset : 0)
                    .setMaxResults(limit != null ? limit : 0));

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