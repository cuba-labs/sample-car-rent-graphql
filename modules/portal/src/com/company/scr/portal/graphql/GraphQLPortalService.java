package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.company.scr.entity.Garage;
import com.company.scr.entity.test.CompositionO2OTestEntity;
import com.company.scr.entity.test.DatatypesTestEntity;
import com.company.scr.entity.test.DatatypesTestEntity2;
import com.company.scr.entity.test.DatatypesTestEntity3;
import com.company.scr.service.GraphQLService;
import com.company.scr.service.JavaScalars;
import com.haulmont.cuba.security.entity.User;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
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
                .scalar(JavaScalars.GraphQLDate)
                .scalar(Scalars.GraphQLLong)
                .scalar(Scalars.GraphQLBigDecimal);
        GraphQLSchemaUtils.assignDataFetchers(rwBuilder, collectionDataFetcher, entityDataFetcher, entityMutationResolver, classes);

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

}