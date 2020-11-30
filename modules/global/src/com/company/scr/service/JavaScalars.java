package com.company.scr.service;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class JavaScalars {

    static final Logger log = LoggerFactory.getLogger(JavaScalars.class);

    public static GraphQLScalarType GraphQLDate = new DateScalar();

    public static GraphQLScalarType GraphQLInstant = new GraphQLScalarType("Instant", "Date type", new Coercing<Instant, Long>() {

        @Override
        public Long serialize(Object input) {
            if (input instanceof Instant) {
                return ((Instant) input).getEpochSecond();
            }
            throw new CoercingSerializeException(
                    "Expected type 'Instant' but was '" + input.getClass().getSimpleName() + "'.");
        }

        @Override
        public Instant parseValue(Object input) {
            if (input instanceof Long) {
                return Instant.ofEpochSecond((Long) input);
            } else if (input instanceof Integer) {
                return Instant.ofEpochSecond((Integer) input);
            }
            throw new CoercingSerializeException(
                    "Expected type 'Long' or 'Integer' but was '" + input.getClass().getSimpleName() + "'.");
        }

        @Override
        public Instant parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return Instant.ofEpochSecond(((IntValue) input).getValue().longValue());
            }
            return null;
        }

    });

    public static GraphQLScalarType GraphQLUUID = new GraphQLScalarType("UUID", "UUID type", new Coercing() {

        @Override
        public Object serialize(Object input) {
            if (input instanceof UUID) {
                return  input;
            }
            return null;
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof String) {
                return parseStringToUUID((String) input);
            }
            return null;
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof StringValue) {
                return parseStringToUUID(((StringValue) input).getValue());
            }
            return null;
        }

        private UUID parseStringToUUID(String input) {
            try {
                return UUID.fromString(input);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to parse UUID from input: " + input, e);
                return null;
            }
        }
    });
}
