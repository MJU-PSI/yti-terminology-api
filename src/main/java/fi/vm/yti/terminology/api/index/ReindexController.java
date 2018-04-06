package fi.vm.yti.terminology.api.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/reindex")
public class ReindexController {

    private final IndexElasticSearchService elasticSearchService;

    private static final Logger logger = LoggerFactory.getLogger(ReindexController.class);

    @Autowired
    public ReindexController(IndexElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping(method = GET, produces = TEXT_PLAIN_VALUE)
    public String reindex() {
        logger.info("GET /reindex");
        this.elasticSearchService.reindex();
        return "OK!";
    }
}
