package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.company.scr.entity.Garage;
import com.company.scr.entity.test.CompositionO2OTestEntity;
import com.company.scr.entity.test.DatatypesTestEntity;
import com.company.scr.entity.test.DatatypesTestEntity2;
import com.company.scr.entity.test.DatatypesTestEntity3;
import com.company.scr.graphql.GraphQLNamingUtils;
import com.company.scr.graphql.JavaScalars;
import com.company.scr.service.GraphQLService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.security.entity.User;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;


@Component
public class GraphQLPortalService {

    private final Logger log = LoggerFactory.getLogger(GraphQLPortalService.class);

    @Inject
    CollectionDataFetcher collectionDataFetcher;
    @Inject
    EntityDataFetcher entityDataFetcher;
    @Inject
    EntityMutationResolver entityMutationResolver;
    @Inject
    GraphQLService graphQLService;

    private GraphQL graphQL;

    private GraphQLSchema graphQLSchema;

    private void initGql() {

        Class[] classes = {Car.class, Garage.class, CompositionO2OTestEntity.class, DatatypesTestEntity.class,
        DatatypesTestEntity2.class, DatatypesTestEntity3.class, User.class};

        String schemaInput = graphQLService.loadSchema(Arrays.asList(classes));
//        log.warn("loadSchema: {}", schemaInput);
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaInput);

        // Scalars here are used for convert mutation input into server data

        RuntimeWiring.Builder rwBuilder = RuntimeWiring.newRuntimeWiring();
        rwBuilder.scalar(JavaScalars.GraphQLUUID)
                .scalar(JavaScalars.GraphQLLong)
                .scalar(JavaScalars.GraphQLBigDecimal)
                .scalar(JavaScalars.GraphQLDate)
                .scalar(JavaScalars.GraphQLLocalDateTime)
                .scalar(JavaScalars.GraphQLVoid);
        assignDataFetchers(rwBuilder, collectionDataFetcher, entityDataFetcher, entityMutationResolver, classes);

        graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, rwBuilder.build());
//        log.warn("graphQLSchema {}", new SchemaPrinter().print(graphQLSchema));

        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    public ExecutionResult executeGraphQL(String query) {
        if (graphQL == null) {
            initGql();
        }

        log.info("executeGraphQL: query {}", query);
        ExecutionResult result = graphQL.execute(query);
        log.info("executeGraphQL result {}", result);
        return result;
    }

    public String getSchema() {
        if (graphQLSchema == null) {
            initGql();
        }
        return new SchemaPrinter().print(graphQLSchema);
    }

    protected static void assignDataFetchers(RuntimeWiring.Builder rwBuilder,
                                          CollectionDataFetcher collectionDataFetcher,
                                          EntityDataFetcher entityDataFetcher,
                                          EntityMutationResolver entityMutationResolver,
                                          Class<Entity>... entityClasses) {

        Arrays.stream(entityClasses).forEach(aClass -> {
            rwBuilder.type("Query", typeWiring -> typeWiring
                    .dataFetcher(GraphQLNamingUtils.composeListQueryName(aClass), collectionDataFetcher.loadEntities(aClass))
                    .dataFetcher(GraphQLNamingUtils.composeByIdQueryName(aClass), entityDataFetcher.loadEntity(aClass))
                    .dataFetcher(GraphQLNamingUtils.composeCountQueryName(aClass), collectionDataFetcher.countEntities(aClass))
            );

            rwBuilder.type("Mutation", typeWiring -> typeWiring
                    .dataFetcher("create" + aClass.getSimpleName(), entityMutationResolver.createEntity(aClass))
            );

            rwBuilder.type("Mutation", typeWiring -> typeWiring
                    .dataFetcher("delete" + aClass.getSimpleName(), entityMutationResolver.deleteEntity(aClass))
            );

        });
    }

}