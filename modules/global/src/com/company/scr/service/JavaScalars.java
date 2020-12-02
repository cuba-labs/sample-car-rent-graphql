package com.company.scr.service;

import graphql.language.StringValue;
import graphql.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.UUID;

public class JavaScalars {

    static final Logger log = LoggerFactory.getLogger(JavaScalars.class);

    public static final SimpleDateFormat CUBA_SERIALIZATION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public static GraphQLScalarType GraphQLDate = new DateScalar();
    public static GraphQLScalarType GraphQLLocalDateTime = new LocalDateTimeScalar();

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

    public static GraphQLScalarType GraphQLVoid = new GraphQLScalarType("Void", "Void type", new Coercing() {
        @Override
        public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
            return null;
        }

        @Override
        public Object parseValue(Object input) throws CoercingParseValueException {
            return null;
        }

        @Override
        public Object parseLiteral(Object input) throws CoercingParseLiteralException {
            return null;
        }
    });
}
