package com.company.scr;

import com.company.scr.entity.Car;
import com.company.scr.entity.Garage;
import com.company.scr.service.GraphQLSchemaBuilder;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.entity.Entity;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GraphQLSchemaBuilderTest {

    @ClassRule
    public static ScrTestContainer cont = ScrTestContainer.Common.INSTANCE;

    private Persistence persistence;

    @Before
    public void setUp() {
        persistence = cont.persistence();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testBuildGraphQLSchema() throws IOException {
        List<Class<? extends Entity>> classes = Arrays.asList(Car.class, Garage.class);

        String schemaStr = persistence.callInTransaction(em -> {
            GraphQLSchema schema = new GraphQLSchemaBuilder(em.getDelegate(), classes)
                    .build();
            return new SchemaPrinter().print(schema);
        });
        File schemaFile = new File("test/schema-current.graphql");
        schemaFile.createNewFile();
        FileWriter writer = new FileWriter(schemaFile);
        writer.write(schemaStr);
        writer.close();
    }

}
