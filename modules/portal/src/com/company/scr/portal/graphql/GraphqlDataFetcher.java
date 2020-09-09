package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
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

    public DataFetcher<List<Car>> getCars() {
        return environment -> {
            FluentLoader<Car, UUID> load = dataManager.load(Car.class);
            return load.list();
        };
    }

}