package com.company.scr.portal.graphql;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.Range;
import com.haulmont.cuba.core.app.importexport.*;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Security;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component(MutationViewBuilder.NAME)
public class MutationViewBuilder {

    @Inject
    protected MetadataTools metadataTools;
    @Inject
    protected Metadata metadata;
    @Inject
    protected Security security;

    public static final String NAME = "cuba_MutationViewBuilder";

    public EntityImportView buildFromJson(String json, MetaClass metaClass) {
        JsonParser jsonParser = new JsonParser();
        JsonElement rootElement = jsonParser.parse(json);
        if (!rootElement.isJsonObject()) {
            throw new RuntimeException("Passed json is not a JSON object");
        }
        return buildFromJsonObject(rootElement.getAsJsonObject(), metaClass);
    }

    protected EntityImportView buildFromJsonObject(JsonObject jsonObject, MetaClass metaClass) {
        EntityImportView view = new EntityImportView(metaClass.getJavaClass());

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String propertyName = entry.getKey();
            MetaProperty metaProperty = metaClass.getProperty(propertyName);
            if (metaProperty != null) {
                Range propertyRange = metaProperty.getRange();
                Class<?> propertyType = metaProperty.getJavaType();
                if (propertyRange.isDatatype() || propertyRange.isEnum()) {
                    if (security.isEntityAttrUpdatePermitted(metaClass, propertyName))
                        view.addLocalProperty(propertyName);
                } else if (propertyRange.isClass()) {
                    if (Entity.class.isAssignableFrom(propertyType)) {
                        if (metadataTools.isEmbedded(metaProperty)) {
                            MetaClass propertyMetaClass = metadata.getClass(propertyType);
                            JsonElement propertyJsonObject = entry.getValue();
                            if (!propertyJsonObject.isJsonObject()) {
                                throw new RuntimeException("JsonObject was expected for property " + propertyName);
                            }
                            if (security.isEntityAttrUpdatePermitted(metaClass, propertyName)) {
                                EntityImportView propertyImportView = buildFromJsonObject(propertyJsonObject.getAsJsonObject(), propertyMetaClass);
                                view.addEmbeddedProperty(propertyName, propertyImportView);
                            }
                        } else {
                            MetaClass propertyMetaClass = metadata.getClass(propertyType);
                            if (metaProperty.getType() == MetaProperty.Type.COMPOSITION) {
                                JsonElement propertyJsonObject = entry.getValue();
                                if (security.isEntityAttrUpdatePermitted(metaClass, propertyName)) {
                                    if (propertyJsonObject.isJsonNull()) {
                                        //in case of null we must add such import behavior to update the reference with null value later
                                        if (metaProperty.getRange().getCardinality() == Range.Cardinality.MANY_TO_ONE) {
                                            view.addManyToOneProperty(propertyName, ReferenceImportBehaviour.IGNORE_MISSING);
                                        } else {
                                            view.addOneToOneProperty(propertyName, ReferenceImportBehaviour.IGNORE_MISSING);
                                        }
                                    } else {
                                        if (!propertyJsonObject.isJsonObject()) {
                                            throw new RuntimeException("JsonObject was expected for property " + propertyName);
                                        }
                                        EntityImportView propertyImportView = buildFromJsonObject(propertyJsonObject.getAsJsonObject(), propertyMetaClass);
                                        if (metaProperty.getRange().getCardinality() == Range.Cardinality.MANY_TO_ONE) {
                                            view.addManyToOneProperty(propertyName, propertyImportView);
                                        } else {
                                            view.addOneToOneProperty(propertyName, propertyImportView);
                                        }
                                    }
                                }
                            } else {
                                if (security.isEntityAttrUpdatePermitted(metaClass, propertyName))
                                    if (metaProperty.getRange().getCardinality() == Range.Cardinality.MANY_TO_ONE) {
                                        view.addManyToOneProperty(propertyName, ReferenceImportBehaviour.ERROR_ON_MISSING);
                                    } else {
                                        view.addOneToOneProperty(propertyName, ReferenceImportBehaviour.ERROR_ON_MISSING);
                                    }
                            }
                        }
                    } else if (Collection.class.isAssignableFrom(propertyType)) {
                        MetaClass propertyMetaClass = metaProperty.getRange().asClass();
                        switch (metaProperty.getRange().getCardinality()) {
                            case MANY_TO_MANY:
                                if (security.isEntityAttrUpdatePermitted(metaClass, propertyName))
                                    view.addManyToManyProperty(propertyName, ReferenceImportBehaviour.ERROR_ON_MISSING, CollectionImportPolicy.REMOVE_ABSENT_ITEMS);
                                break;
                            case ONE_TO_MANY:
//                                if (metaProperty.getType() == MetaProperty.Type.COMPOSITION) {
                                    JsonElement compositionJsonArray = entry.getValue();
                                    if (!compositionJsonArray.isJsonArray()) {
                                        throw new RuntimeException("JsonArray was expected for property " + propertyName);
                                    }
                                    EntityImportView propertyImportView = buildFromJsonArray(compositionJsonArray.getAsJsonArray(), propertyMetaClass);
                                    if (security.isEntityAttrUpdatePermitted(metaClass, propertyName))
                                        view.addOneToManyProperty(propertyName, propertyImportView, CollectionImportPolicy.REMOVE_ABSENT_ITEMS);
//                                }
                                break;
                            default:
                                // ignore other options
                                break;
                        }
                    }
                }
            }
        }

        return view;
    }

    protected EntityImportView buildFromJsonArray(JsonArray jsonArray, MetaClass metaClass) {
        List<EntityImportView> viewsForArrayElements = new ArrayList<>();
        for (JsonElement arrayElement : jsonArray.getAsJsonArray()) {
            EntityImportView viewForArrayElement = buildFromJsonObject(arrayElement.getAsJsonObject(), metaClass);
            viewsForArrayElements.add(viewForArrayElement);
        }
        EntityImportView resultView = viewsForArrayElements.isEmpty() ?
                new EntityImportView(metaClass.getJavaClass()) :
                viewsForArrayElements.get(0);
        if (viewsForArrayElements.size() > 1) {
            for (int i = 1; i < viewsForArrayElements.size(); i++) {
                resultView = mergeViews(resultView, viewsForArrayElements.get(i));
            }
        }
        return resultView;
    }

    /**
     * Recursively merges two views. The result view will contain all fields that are defined either in view1 or in
     * view2.
     */
    protected EntityImportView mergeViews(@Nullable EntityImportView view1, @Nullable EntityImportView view2) {
        if (view1 == null) return view2;
        if (view2 == null) return view1;
        EntityImportView mergedView = new EntityImportView(view1.getEntityClass());

        for (EntityImportViewProperty p1 : view1.getProperties()) {
            EntityImportViewProperty newProperty = new EntityImportViewProperty(p1.getName());
            newProperty.setReferenceImportBehaviour(p1.getReferenceImportBehaviour());
            newProperty.setCollectionImportPolicy(p1.getCollectionImportPolicy());
            EntityImportViewProperty p2 = view2.getProperty(p1.getName());
            if (p2 == null) {
                newProperty.setView(p1.getView());
            } else {
                newProperty.setView(mergeViews(p1.getView(), p2.getView()));
            }
            mergedView.addProperty(newProperty);
        }

        //add properties that exist in p2 but not in p1
        for (EntityImportViewProperty p2 : view2.getProperties()) {
            if (view1.getProperty(p2.getName()) == null) {
                EntityImportViewProperty newProperty = new EntityImportViewProperty(p2.getName());
                newProperty.setView(p2.getView());
                newProperty.setReferenceImportBehaviour(p2.getReferenceImportBehaviour());
                newProperty.setCollectionImportPolicy(p2.getCollectionImportPolicy());
                mergedView.addProperty(newProperty);
            }
        }

        return mergedView;
    }
}
