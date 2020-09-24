package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.company.scr.entity.CarType;
import com.haulmont.cuba.core.global.DataManager;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class EntityMutation {


    private final Logger log = LoggerFactory.getLogger(EntityMutation.class);

    @Inject
    protected DataManager dataManager;

    public DataFetcher<Car> createEntity() {
        return environment -> {

            Map input = environment.getArgument("car");
            log.warn("createEntity input {}", input);

            Car car = dataManager.create(Car.class);
            car.setManufacturer((String) input.get("manufacturer"));
            car.setCarType(CarType.SEDAN);
            dataManager.commit(car);

            return car;
        };
    }
}
