package com.company.scr.portal.graphql;

import com.company.scr.entity.ScrUser;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@Component
public class GraphqlDataFetcher {

    @Inject
    protected DataManager dataManager;

    public DataFetcher<List<ScrUser>> getUsers() {
        return environment -> {
            FluentLoader<ScrUser, UUID> load = dataManager.load(ScrUser.class);
            return load.list();
        };
    }

}