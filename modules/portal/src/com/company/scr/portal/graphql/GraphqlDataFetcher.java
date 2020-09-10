package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class GraphqlDataFetcher {

    private final Logger log = LoggerFactory.getLogger(GraphqlDataFetcher.class);

    @Inject
    protected DataManager dataManager;

    public DataFetcher<List<Car>> getCars() {
        return environment -> {
            LoadContext<Car> lc = new LoadContext<>(Car.class);
            lc.setView("car-gql");

            log.warn("loadList {}", lc);
            List<Car> cars = dataManager.loadList(lc);
            return cars;
        };
    }

}