package com.company.scr.portal.graphql;

import com.company.scr.graphql.GraphQLConstants;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.haulmont.addon.restapi.api.service.filter.RestFilterParseException;
import com.haulmont.addon.restapi.api.service.filter.RestFilterParseResult;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.MetadataTools;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

@Component
public class CollectionDataFetcher {

    private final Logger log = LoggerFactory.getLogger(CollectionDataFetcher.class);

    @Inject
    protected DataManager dataManager;
    @Inject
    private GraphQLRestFilterParser restFilterParser;
    @Inject
    private Metadata metadata;
    @Inject
    private MetadataTools metadataTools;


    public <E extends Entity> DataFetcher<List<E>> loadEntities(Class<E> entityClass) {
        return environment -> {

            LoadContext<E> lc = new LoadContext<>(entityClass);
            lc.setView(DataFetcherUtils.buildView(entityClass, environment));

            String sort = environment.getArgument(GraphQLConstants.SORT);

            Integer limit = environment.getArgument(GraphQLConstants.LIMIT);
            Integer offset = environment.getArgument(GraphQLConstants.OFFSET);

            MetaClass metaClass = metadata.getClass(entityClass);
            String queryString = "select e from " + metaClass.getName() + " e";

            Map<String, Object> queryParameters = null;
            // todo implement graphql type mapper that leave filter as plain string (not convert to Map) or returns RestFilterParseResult
            Object filterArg = environment.getArgument(GraphQLConstants.FILTER);
            if (filterArg != null) {
                RestFilterParseResult filterParseResult;
                try {
                    filterParseResult = restFilterParser.parse(new Gson().toJson(filterArg), metaClass);
                } catch (RestFilterParseException e) {
                    throw new UnsupportedOperationException("Cannot parse entities filter" + e.getMessage(), e);
                }

                String jpqlWhere = filterParseResult.getJpqlWhere();
                queryParameters = filterParseResult.getQueryParameters();

                if (jpqlWhere != null) {
                    queryString += " where " + jpqlWhere.replace("{E}", "e");
                }
            }

            LoadContext.Query query = LoadContext.createQuery(addOrderBy(queryString, sort, metaClass));

            if (queryParameters != null) {
                query.setParameters(queryParameters);
            }

            lc.setQuery(query
                    .setFirstResult(offset != null ? offset : 0)
                    .setMaxResults(limit != null ? limit : 0));

            log.warn("loadList {}", lc);
            return dataManager.loadList(lc);
        };
    }

    public DataFetcher countEntities(Class entityClass) {
        return environment -> {
            LoadContext<? extends Entity> lc = new LoadContext<>(entityClass);
            return dataManager.getCount(lc);
        };
    }

    // todo code below (#addOrderBy #getEntityPropertySortExpression) is copypasted from EntitiesControllerManager

    protected String addOrderBy(String queryString, @Nullable String sort, MetaClass metaClass) {
        if (Strings.isNullOrEmpty(sort)) {
            return queryString;
        }
        StringBuilder orderBy = new StringBuilder(queryString).append(" order by ");
        Iterable<String> iterableColumns = Splitter.on(",").trimResults().omitEmptyStrings().split(sort);
        for (String column : iterableColumns) {
            String order = "";
            if (column.startsWith("-") || column.startsWith("+")) {
                order = column.substring(0, 1);
                column = column.substring(1);
            }
            MetaPropertyPath propertyPath = metaClass.getPropertyPath(column);
            if (propertyPath != null) {
                switch (order) {
                    case "-":
                        order = " desc, ";
                        break;
                    case "+":
                    default:
                        order = " asc, ";
                        break;
                }
                MetaProperty metaProperty = propertyPath.getMetaProperty();
                if (metaProperty.getRange().isClass()) {
                    if (!metaProperty.getRange().getCardinality().isMany()) {
                        for (String exp : getEntityPropertySortExpression(propertyPath)) {
                            orderBy.append(exp).append(order);
                        }
                    }
                } else {
                    orderBy.append("e.").append(column).append(order);
                }
            }
        }
        return orderBy.substring(0, orderBy.length() - 2);
    }

    protected List<String> getEntityPropertySortExpression(MetaPropertyPath metaPropertyPath) {
        Collection<MetaProperty> properties = metadataTools.getNamePatternProperties(
                metaPropertyPath.getMetaProperty().getRange().asClass());
        if (!properties.isEmpty()) {
            List<String> sortExpressions = new ArrayList<>(properties.size());
            for (MetaProperty metaProperty : properties) {
                if (metadataTools.isPersistent(metaProperty)) {
                    MetaPropertyPath childPropertyPath = new MetaPropertyPath(metaPropertyPath, metaProperty);
                    if (metaProperty.getRange().isClass()) {
                        if (!metaProperty.getRange().getCardinality().isMany()) {
                            sortExpressions.addAll(getEntityPropertySortExpression(childPropertyPath));
                        }
                    } else {
                        sortExpressions.add(String.format("e.%s", childPropertyPath.toString()));
                    }
                }
            }
            return sortExpressions;
        } else {
            return Collections.singletonList(String.format("e.%s", metaPropertyPath.toString()));
        }
    }

}