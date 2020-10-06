package com.company.scr.portal.graphql;

import com.google.gson.Gson;
import com.haulmont.addon.restapi.api.exception.RestAPIException;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.importexport.EntityImportException;
import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.EntityImportViewBuilderAPI;
import com.haulmont.cuba.core.app.serialization.EntitySerializationAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import graphql.schema.DataFetcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

@Component
public class EntityMutation {


    private final Logger log = LoggerFactory.getLogger(EntityMutation.class);

    @Inject
    private EntitySerializationAPI entitySerializationAPI;
    @Inject
    private Metadata metadata;
    @Inject
    private EntityImportViewBuilderAPI entityImportViewBuilderAPI;
    @Inject
    private EntityImportExportService entityImportExportService;

    public DataFetcher<Entity> createEntity(Class<Entity> entityClass) {
        return environment -> {

            Map<String, String> input = environment.getArgument(entityClass.getSimpleName().toLowerCase());
            log.warn("createEntity input {}", input);

            String entityJson = new JSONObject(input).toString();
            log.warn("createEntity json {}", entityJson);

            MetaClass metaClass = metadata.getClass(entityClass);
            return createEntityFromJson(metaClass, entityJson);
        };
    }

    protected Entity createEntityFromJson(MetaClass metaClass, String entityJson) {
        Entity entity;
        try {
            entity = entitySerializationAPI.entityFromJson(entityJson, metaClass);
        } catch (Exception e) {
            throw new RestAPIException("Cannot deserialize an entity from JSON", "", HttpStatus.BAD_REQUEST, e);
        }

        EntityImportView entityImportView = entityImportViewBuilderAPI.buildFromJson(entityJson, metaClass);

        log.warn("createEntityFromJson: entityImportView \n{}", new Gson().toJson(printEntityView(entityImportView)));

        Collection<Entity> importedEntities;
        try {
            importedEntities = entityImportExportService.importEntities(Collections.singletonList(entity), entityImportView, true);
        } catch (EntityImportException e) {
            throw new RestAPIException("Entity creation failed", e.getMessage(), HttpStatus.BAD_REQUEST, e);
        }

        //if many entities were created (because of @Composition references) we must find the main entity
        return getMainEntity(importedEntities, metaClass);
    }

    /**
     * Finds entity with given metaClass.
     */
    protected Entity getMainEntity(Collection<Entity> importedEntities, MetaClass metaClass) {
        Entity mainEntity = null;
        if (importedEntities.size() > 1) {
            Optional<Entity> first = importedEntities.stream().filter(e -> e.getMetaClass().equals(metaClass)).findFirst();
            if (first.isPresent()) mainEntity = first.get();
        } else {
            mainEntity = importedEntities.iterator().next();
        }
        return mainEntity;
    }

    protected Object printEntityView(EntityImportView view) {
        if (view == null) {
            return "";
        }

        Map<String, Object> map = new HashMap<>();
        view.getProperties().forEach(prop -> map.put(prop.getName(), printEntityView(prop.getView())));
        return map;
    }
}
