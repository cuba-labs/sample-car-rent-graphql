package com.company.scr.portal.graphql;

import com.company.scr.graphql.GraphQLNamingUtils;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.serialization.EntitySerializationAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import graphql.schema.DataFetcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

@Component
public class EntityMutationResolver {

    private final Logger log = LoggerFactory.getLogger(EntityMutationResolver.class);

    @Inject
    private EntitySerializationAPI entitySerializationAPI;
    @Inject
    private Metadata metadata;
    @Inject
    private MutationViewBuilder mutationViewBuilder;
    @Inject
    private EntityImportExportService entityImportExportService;
    @Inject
    protected DataManager dataManager;

    public DataFetcher<Entity> createEntity(Class<Entity> entityClass) {
        return environment -> {

            Map<String, String> input = environment.getArgument(GraphQLNamingUtils.uncapitalizedSimpleName(entityClass));
            log.warn("createEntity: input {}", input);

            String entityJson = new JSONObject(input).toString();
            log.warn("createEntity: json {}", entityJson);

            MetaClass metaClass = metadata.getClass(entityClass);

            Entity entity = entitySerializationAPI.entityFromJson(entityJson, metaClass);
            EntityImportView entityImportView = mutationViewBuilder.buildFromJson(entityJson, metaClass);
            log.warn("createEntity: view {}", entityImportView);
            Collection<Entity> entities = entityImportExportService.importEntities(Collections.singletonList(entity), entityImportView);
            return entities.stream().filter(e -> e.getId().equals(entity.getId())).findAny().get();
        };
    }

    public DataFetcher deleteEntity(Class<Entity> entityClass) {
        return environment -> {
            // todo support not only UUID types of id
            UUID id = UUID.fromString(environment.getArgument("id"));
            log.warn("deleteEntity: id {}", id);
            Id entityId = Id.of(id, entityClass);
            dataManager.remove(entityId);
            return null;
        };
    }

}
