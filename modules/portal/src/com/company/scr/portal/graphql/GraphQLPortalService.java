package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.company.scr.entity.Garage;
import com.company.scr.service.GraphQLService;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import org.crygier.graphql.JavaScalars;
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
    GraphQLService graphQLService;

    private GraphQL graphQL;

    private GraphQLSchema graphQLSchema;

    private void initGql() {

        Class[] classes = {Car.class, Garage.class};

        String schemaInput = graphQLService.loadSchema(Arrays.asList(classes));
//        log.warn("loadSchema: {}", schemaInput);
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaInput);

        // Provide API (gql queries) for entities listed below

        RuntimeWiring.Builder rwBuilder = RuntimeWiring.newRuntimeWiring();
        rwBuilder.scalar(JavaScalars.GraphQLUUID)
                .scalar(JavaScalars.GraphQLDate)
                .scalar(JavaScalars.GraphQLLocalDate)
                .scalar(JavaScalars.GraphQLLocalDateTime)
                .scalar(Scalars.GraphQLLong)
                .scalar(Scalars.GraphQLBigDecimal);
        GraphQLSchemaUtils.assignDataFetchers(rwBuilder, collectionDataFetcher, entityDataFetcher, classes);

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