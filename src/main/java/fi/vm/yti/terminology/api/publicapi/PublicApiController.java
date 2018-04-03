package fi.vm.yti.terminology.api.publicapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/publicapi")
public class PublicApiController {

    private final PublicApiTermedService termedService;
    private final PublicApiElasticSearchService publicApiElasticSearchService;

    public PublicApiController(PublicApiTermedService termedService,
                               PublicApiElasticSearchService publicApiElasticSearchService) {
        this.termedService = termedService;
        this.publicApiElasticSearchService = publicApiElasticSearchService;
    }

    @RequestMapping(value = "/vocabularies", method = GET, produces = APPLICATION_JSON_VALUE)
    List<PublicApiVocabulary> getVocabularyList() {
        return termedService.getVocabularyList();
    }

    @RequestMapping(value = "/searchconcept", method = POST, produces = APPLICATION_JSON_VALUE)
    String searchConcept(@RequestBody JsonNode query) {
        return publicApiElasticSearchService.searchConcept(query);
    }
}
