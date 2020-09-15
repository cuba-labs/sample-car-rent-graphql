package com.company.scr.portal.controllers;

import com.company.scr.portal.graphql.GraphqlServiceBean;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.portal.security.PortalSessionProvider;
import com.haulmont.cuba.security.entity.User;
import graphql.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Map;


@Controller
public class PortalController {

    private final Logger log = LoggerFactory.getLogger(PortalController.class);

    @Inject
    protected DataService dataService;
    @Inject
    private GraphqlServiceBean graphqlServiceBean;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        if (PortalSessionProvider.getUserSession().isAuthenticated()) {
            LoadContext l = new LoadContext(User.class);
            l.setQueryString("select u from sec$User u");
            model.addAttribute("users", dataService.loadList(l));
        }
        return "index";
    }

    @RequestMapping(value = "/graphiql", method = RequestMethod.GET)
    public String graphiql() {
        return "graphiql";
    }

    @PostMapping("/graphiql-inner-query")
    public ResponseEntity<Object> graphiqlInnerQuery(@RequestBody Map<String, String> requestBody) {
        ExecutionResult result = graphqlServiceBean.executeGraphQL(requestBody.get("query"));
        ResponseEntity<Object> body = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(result);
        log.info("graphiqlInnerQuery return {}", body);
        return body;
    }

    @PostMapping("/graphql")
    public ResponseEntity<Object> graphql(@RequestBody String query) {
        ExecutionResult result = graphqlServiceBean.executeGraphQL(query);
        ResponseEntity<Object> body = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(result);
        log.info("graphql return {}", body);
        return body;
    }

    @GetMapping(value = "/graphql/schema")
    public ResponseEntity<String> schema() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(graphqlServiceBean.getSchema());
    }

}
