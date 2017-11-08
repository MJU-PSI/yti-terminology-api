package fi.vm.yti.terminology.api.index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/reindex")
public class ReindexController {

    private final IndexElasticSearchService elasticSearchService;

    @Autowired
    public ReindexController(IndexElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping(method = GET)
    public String reindex() {
        this.elasticSearchService.reindex();
        return "OK!";
    }
}
