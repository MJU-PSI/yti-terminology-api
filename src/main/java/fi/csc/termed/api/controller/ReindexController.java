package fi.csc.termed.api.controller;

import fi.csc.termed.api.service.ElasticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReindexController {

    private final ElasticSearchService elasticSearchService;

    @Autowired
    public ReindexController(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping("/reindex")
    public String reindex() {
        this.elasticSearchService.reindex();
        return "OK!";
    }
}
