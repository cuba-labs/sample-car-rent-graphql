package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.app.serialization.EntitySerializationAPI;
import com.haulmont.cuba.core.entity.Entity;
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

    public <E extends Entity> DataFetcher<E> createEntity(Class<E> entityClass) {
        return environment -> {

            Map<String, String> input = environment.getArgument(entityClass.getSimpleName().toLowerCase());
            log.warn("createEntity input {}", input);

            String entityJson = new JSONObject(input).toString();
            log.warn("createEntity json {}", entityJson);

            E entity = entitySerializationAPI.entityFromJson(entityJson, metadata.getClass(entityClass));
            return dataManager.commit(entity, DataFetcherUtils.buildView(entityClass, environment));
        };
    }
}
