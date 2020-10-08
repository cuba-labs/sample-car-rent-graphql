package com.company.scr.portal.graphql;

import com.google.gson.Gson;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.serialization.EntitySerializationAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.EntitySet;
import com.haulmont.cuba.core.global.Metadata;
import graphql.schema.DataFetcher;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EntityMutationResolver {

    private final Logger log = LoggerFactory.getLogger(EntityMutationResolver.class);

    @Inject
    protected DataManager dataManager;
    @Inject
    private EntitySerializationAPI entitySerializationAPI;
    @Inject
    private Metadata metadata;
    @Inject
    private MutationViewBuilder mutationViewBuilder;

    public DataFetcher<Entity> createEntity(Class<Entity> entityClass) {
        return environment -> {

            Map<String, String> input = environment.getArgument(entityClass.getSimpleName().toLowerCase());
            log.warn("createEntity: input {}", input);

            String entityJson = new JSONObject(input).toString();
            log.warn("createEntity: json {}", entityJson);

            MetaClass metaClass = metadata.getClass(entityClass);

            Entity entity = entitySerializationAPI.entityFromJson(entityJson, metaClass);
            CommitContext ctx = new CommitContext(entity);

            EntityImportView entityImportView = mutationViewBuilder.buildFromJson(entityJson, metaClass);
            log.warn("createEntity: entityImportView {}", new Gson().toJson(printEntityView(entityImportView)));

            entityImportView.getProperties().forEach(prop -> {
                MetaProperty metaProperty = metaClass.getPropertyNN(prop.getName());
                if (metaProperty.getRange().isClass()) {
                    switch (metaProperty.getRange().getCardinality()) {
                        case MANY_TO_MANY:
                            importManyToManyCollectionAttribute(entity, metaProperty, prop.getView(), ctx);
                            break;
                        case ONE_TO_MANY:
                            importOneToManyCollectionAttribute(entity, metaProperty, prop.getView(), ctx);
                            break;
                        default:
                            // ONE_TO_ONE, MANY_TO_ONE
                            importReference(entity.getValue(metaProperty.getName()), prop.getView(), ctx);
                    }
                }
            });

            EntitySet entities = dataManager.commit(ctx);
            return entities.get(entity.getClass(), (UUID) entity.getId());
        };
    }

    private void importManyToManyCollectionAttribute(Entity entity, MetaProperty metaProperty, EntityImportView view, CommitContext ctx) {
        log.warn("importManyToManyCollectionAttribute for property {}:{} view {}", entity.getMetaClass().getName(), metaProperty.getName(), printEntityView(view));
        Collection<Entity> collectionValue = entity.getValue(metaProperty.getName());
        if (collectionValue != null) {
            collectionValue.forEach(colEntity -> importReference(colEntity, view, ctx));
        }
    }

    private void importOneToManyCollectionAttribute(Entity entity, MetaProperty metaProperty, EntityImportView view, CommitContext ctx) {
        log.warn("importOneToManyCollectionAttribute for property {}:{} view {}", entity.getMetaClass().getName(), metaProperty.getName(), printEntityView(view));
        Collection<Entity> collectionValue = entity.getValue(metaProperty.getName());
        if (collectionValue != null) {
            collectionValue.forEach(colEntity -> importReference(colEntity, view, ctx));
        }
    }

    private void importReference(Entity refEntity, EntityImportView entityImportView, CommitContext ctx) {
        ctx.addInstanceToCommit(refEntity);

        if (entityImportView != null) {
            entityImportView.getProperties().forEach(prop -> {

                MetaClass metaClass = metadata.getClass(refEntity.getClass());
                MetaProperty metaProperty = metaClass.getPropertyNN(prop.getName());

                if (metaProperty.getRange().isClass()) {
                    switch (metaProperty.getRange().getCardinality()) {
                        case MANY_TO_MANY:
                            importManyToManyCollectionAttribute(refEntity, metaProperty, prop.getView(), ctx);
                            break;
                        case ONE_TO_MANY:
                            importOneToManyCollectionAttribute(refEntity, metaProperty, prop.getView(), ctx);
                            break;
                        default:
                            importReference(refEntity.getValue(metaProperty.getName()), prop.getView(), ctx);
                    }
                }
            });
        }
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
