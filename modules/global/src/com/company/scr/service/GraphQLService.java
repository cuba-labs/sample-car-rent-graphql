package com.company.scr.service;

import com.haulmont.cuba.core.entity.Entity;

import java.util.List;

public interface GraphQLService {
    String NAME = "scr_GraphqlSchemaService";

    String loadSchema(List<Class<? extends Entity>> classes);
}