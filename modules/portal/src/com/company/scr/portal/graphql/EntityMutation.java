package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.haulmont.cuba.core.app.serialization.EntitySerializationAPI;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import graphql.schema.DataFetcher;
import org.json.JSONObject;
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
    @Inject
    private EntitySerializationAPI entitySerializationAPI;
    @Inject
    private Metadata metadata;

    public DataFetcher<Car> createEntity() {
        return environment -> {

            Map<String, String> input = environment.getArgument("car");
            log.warn("createEntity input {}", input);

            String carJson = new JSONObject(input).toString();
            log.warn("createEntity carJson {}", carJson);

            Car car = entitySerializationAPI.entityFromJson(carJson, metadata.getClass(Car.class));
            dataManager.commit(car);

            return car;
        };
    }
}
