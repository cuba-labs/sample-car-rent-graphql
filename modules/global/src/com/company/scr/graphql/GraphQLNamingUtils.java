package com.company.scr.graphql;

import com.haulmont.cuba.core.app.importexport.EntityImportView;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class GraphQLNamingUtils {

    @NotNull
    public static String composeListQueryName(Class aClass) {
        return uncapitalizedSimpleName(aClass) + "List";
    }

    @NotNull
    public static String composeCountQueryName(Class aClass) {
        return uncapitalizedSimpleName(aClass) + "Count";
    }

    @NotNull
    public static String composeByIdQueryName(Class aClass) {
        return uncapitalizedSimpleName(aClass) + "ById";
    }

    @NotNull
    public static String uncapitalizedSimpleName(Class aClass) {
        return StringUtils.uncapitalize(aClass.getSimpleName());
    }

    public static Object printEntityView(EntityImportView view) {
        if (view == null) {
            return "";
        }

        Map<String, Object> map = new HashMap<>();
        view.getProperties().forEach(prop -> map.put(prop.getName(), printEntityView(prop.getView())));
        return map;
    }

}
