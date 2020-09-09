package com.company.scr.service;

import com.haulmont.cuba.core.global.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(GraphqlSchemaService.NAME)
public class GraphqlSchemaServiceBean implements GraphqlSchemaService {

    private final Logger log = LoggerFactory.getLogger(GraphqlSchemaServiceBean.class);

    @Inject
    Resources resources;

    @Override
    public String loadSchema() {
        return resources.getResourceAsString("com/company/scr/schema.graphql");
    }


}