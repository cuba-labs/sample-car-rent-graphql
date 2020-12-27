package com.company.scr.portal.graphql;

import com.company.scr.graphql.GraphQLConstants;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewBuilder;
import graphql.schema.GraphQLFieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataFetcherUtils {

    private final static Logger log = LoggerFactory.getLogger(DataFetcherUtils.class);

    public static <E extends Entity> View buildView(Class<E> entityClass, graphql.schema.DataFetchingEnvironment environment) {
        Map<String, GraphQLFieldDefinition> definitions = environment.getSelectionSet().getDefinitions();
        log.warn("definitions {}", definitions.keySet());

        List<String> properties = definitions.keySet().stream()
                .map(def -> def.replaceAll("/", "."))
                // remove '__typename' from fetch plan
                .filter(prop -> !prop.equals(GraphQLConstants.SYS_ATTR_TYPENAME))
                // todo fetch failed, if we need to return instanceName in nested entity,
                //  but fetch plan does not contains attrs of nested entities required to compose instanceName
                //  i.e. for garage.car.instanceName we need to request garage.car.manufacturer and garage.car.model attrs,
                //  which are required for composing Car instanceName
                // remove 'instanceName' and '*.instanceName' attrs from fetch plan - no such attr in entity
                .filter(propertyNotMatch(GraphQLConstants.SYS_ATTR_INSTANCE_NAME))
                .collect(Collectors.toList());

        log.warn("properties {}", properties);

        return ViewBuilder
                .of(entityClass)
                .addView("_local")
                .addAll(properties.toArray(new String[] {}))
                .build();
    }

    /**
     * @param property property to check
     * @return true if property NOT match 'someProperty' and '*.someProperty'
     */
    private static Predicate<String> propertyNotMatch(String property) {
        return prop -> !prop.equals(property) && !prop.matches(".*\\." + property);
    }


}
