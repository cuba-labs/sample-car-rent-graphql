package com.company.scr.portal.graphql;

import com.haulmont.cuba.core.global.Resources;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.InputStream;



@Component
public class GraphqlServiceBean {

    private final Logger log = LoggerFactory.getLogger(GraphqlServiceBean.class);

    @Inject
    Resources resources;
    @Inject
    GraphqlDataFetcher graphqlDataFetcher;

    private GraphQL graphQL;

    @PostConstruct
    private void loadSchema() {

        InputStream schemaIS = resources.getResourceAsStream("com/company/scr/schema.graphql");

        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaIS);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                                .dataFetcher("users", graphqlDataFetcher.getUsers())
//                        .dataFetcher("roles", graphQLDataFetcher.getRoles())
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
        log.info("executeGraphQL: query {}", query);
        ExecutionResult result = graphQL.execute(query);
        log.info("executeGraphQL result {}", result);
        return result;

    }

}