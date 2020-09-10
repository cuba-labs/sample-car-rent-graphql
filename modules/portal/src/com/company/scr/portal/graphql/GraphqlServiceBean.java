package com.company.scr.portal.graphql;

import com.company.scr.entity.Car;
import com.company.scr.entity.Garage;
import com.company.scr.service.GraphqlSchemaService;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;



@Component
public class GraphqlServiceBean {

    private final Logger log = LoggerFactory.getLogger(GraphqlServiceBean.class);

    @Inject
    CollectionDataFetcher collectionDataFetcher;
    @Inject
    EntityDataFetcher entityDataFetcher;
    @Inject
    GraphqlSchemaService graphqlSchemaService;

    private GraphQL graphQL;

    private void initGql() {
        String schemaInput = graphqlSchemaService.loadSchema();
        log.warn("loadSchema: {}", schemaInput);
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaInput);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("cars", collectionDataFetcher.loadEntities(Car.class))
                        .dataFetcher("carById", entityDataFetcher.loadEntity(Car.class))
                        .dataFetcher("garages", collectionDataFetcher.loadEntities(Garage.class))
                        .dataFetcher("garageById", entityDataFetcher.loadEntity(Garage.class))
                )
//                .type("User", typeWiring -> typeWiring
//                        .dataFetcher("roles", graphQLDataFetcher.getUserRoles()))
//                .type("Role", typeWiring -> typeWiring
//                        .dataFetcher("users", graphQLDataFetcher.getRoleUsers()))
                .build();

        GraphQLSchema graphQLSchema =
                new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

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

}