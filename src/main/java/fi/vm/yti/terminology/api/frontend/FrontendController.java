package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/frontend")
public class FrontendController {

    private final FrontendTermedService termedService;
    private final FrontendElasticSearchService elasticSearchService;
    private final AuthenticatedUserProvider userProvider;

    public FrontendController(FrontendTermedService termedService,
                              FrontendElasticSearchService elasticSearchService,
                              AuthenticatedUserProvider userProvider) {
        this.termedService = termedService;
        this.elasticSearchService = elasticSearchService;
        this.userProvider = userProvider;
    }

    @RequestMapping(value = "/authenticated-user", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    YtiUser getUser() {
        return userProvider.getUser();
    }

    @RequestMapping("/vocabulary")
    JsonNode getVocabulary(@RequestParam String graphId,
                           @RequestParam String vocabularyType) {
        return termedService.getVocabulary(graphId, vocabularyType);
    }

    @RequestMapping("/vocabularies")
    JsonNode getVocabularyList(@RequestParam String vocabularyType) {
        return termedService.getVocabularyList(vocabularyType);
    }

    @RequestMapping("/concept")
    @Nullable JsonNode getConcept(@RequestParam String graphId,
                                  @RequestParam String conceptId) {
        return termedService.getConcept(graphId, conceptId);
    }

    @RequestMapping("/collection")
    JsonNode getCollection(@RequestParam String graphId,
                           @RequestParam String collectionId) {
        return termedService.getCollection(graphId, collectionId);
    }

    @RequestMapping("/collections")
    JsonNode getCollectionList(@RequestParam String graphId) {
        return termedService.getCollectionList(graphId);
    }

    @RequestMapping("/organizations")
    JsonNode getOrganizationList() {
        return termedService.getNodeListWithoutReferencesOrReferrers("Organization");
    }

    @RequestMapping("/groups")
    JsonNode getGroupList() {
        return termedService.getNodeListWithoutReferencesOrReferrers("Group");
    }

    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    void updateAndDeleteInternalNodes(@RequestBody JsonNode deleteAndSave) {
        termedService.updateAndDeleteInternalNodes(deleteAndSave);
    }

    @RequestMapping(value = "/remove", method = RequestMethod.DELETE)
    void removeNodes(@RequestParam boolean sync,
                     @RequestParam boolean disconnect,
                     @RequestBody JsonNode identifiers) {
        termedService.removeNodes(sync, disconnect, identifiers);
    }

    @RequestMapping("/nodes")
    JsonNode getAllNodeIdentifiers(@RequestParam String graphId) {
        return termedService.getAllNodeIdentifiers(graphId);
    }

    @RequestMapping(value = "/types", method = RequestMethod.GET)
    JsonNode getTypes(@RequestParam(required = false) String graphId) {
        return termedService.getTypes(graphId);
    }

    @RequestMapping(value = "/types", method = RequestMethod.POST)
    void updateTypes(@RequestParam String graphId,
                     @RequestBody JsonNode metaNodes) {
        termedService.updateTypes(graphId, metaNodes);
    }

    @RequestMapping(value = "/types", method = RequestMethod.DELETE)
    void removeTypes(@RequestParam String graphId,
                     @RequestBody JsonNode identifiers) {
        termedService.removeTypes(graphId, identifiers);
    }

    @RequestMapping("/graphs")
    JsonNode getGraphs() {
        return termedService.getGraphs();
    }

    @RequestMapping(value = "/graph", method = RequestMethod.POST)
    void createGraph(@RequestBody JsonNode graph) {
        termedService.createGraph(graph);
    }

    @RequestMapping(value = "/graph", method = RequestMethod.DELETE)
    void deleteGraph(@RequestParam String graphId) {
        termedService.deleteGraph(graphId);
    }

    @RequestMapping(value = "/searchConcept", method = RequestMethod.POST)
    String searchConcept(@RequestBody JsonNode query) {
        return elasticSearchService.searchConcept(query);
    }
}
