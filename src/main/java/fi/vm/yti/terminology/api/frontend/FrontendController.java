package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.*;

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

    @RequestMapping(value = "/authenticated-user", method = GET)
    YtiUser getUser() {
        return userProvider.getUser();
    }

    @RequestMapping(value = "/vocabulary", method = GET)
    JsonNode getVocabulary(@RequestParam String graphId,
                           @RequestParam String vocabularyType) {
        return termedService.getVocabulary(graphId, vocabularyType);
    }

    @RequestMapping(value = "/vocabularies", method = GET)
    JsonNode getVocabularyList(@RequestParam String vocabularyType) {
        return termedService.getVocabularyList(vocabularyType);
    }

    @RequestMapping(value = "/concept", method = GET)
    @Nullable JsonNode getConcept(@RequestParam String graphId,
                                  @RequestParam String conceptId) {
        return termedService.getConcept(graphId, conceptId);
    }

    @RequestMapping(value = "/collection", method = GET)
    JsonNode getCollection(@RequestParam String graphId,
                           @RequestParam String collectionId) {
        return termedService.getCollection(graphId, collectionId);
    }

    @RequestMapping(value = "/collections", method = GET)
    JsonNode getCollectionList(@RequestParam String graphId) {
        return termedService.getCollectionList(graphId);
    }

    @RequestMapping(value = "/organizations", method = GET)
    JsonNode getOrganizationList() {
        return termedService.getNodeListWithoutReferencesOrReferrers("Organization");
    }

    @RequestMapping(value = "/groups", method = GET)
    JsonNode getGroupList() {
        return termedService.getNodeListWithoutReferencesOrReferrers("Group");
    }

    @RequestMapping(value = "/modify", method = POST)
    void updateAndDeleteInternalNodes(@RequestBody JsonNode deleteAndSave) {
        termedService.updateAndDeleteInternalNodes(deleteAndSave);
    }

    @RequestMapping(value = "/remove", method = DELETE)
    void removeNodes(@RequestParam boolean sync,
                     @RequestParam boolean disconnect,
                     @RequestBody JsonNode identifiers) {
        termedService.removeNodes(sync, disconnect, identifiers);
    }

    @RequestMapping(value = "/nodes", method = GET)
    JsonNode getAllNodeIdentifiers(@RequestParam String graphId) {
        return termedService.getAllNodeIdentifiers(graphId);
    }

    @RequestMapping(value = "/types", method = GET)
    JsonNode getTypes(@RequestParam(required = false) String graphId) {
        return termedService.getTypes(graphId);
    }

    @RequestMapping(value = "/types", method = POST)
    void updateTypes(@RequestParam String graphId,
                     @RequestBody JsonNode metaNodes) {
        termedService.updateTypes(graphId, metaNodes);
    }

    @RequestMapping(value = "/types", method = DELETE)
    void removeTypes(@RequestParam String graphId,
                     @RequestBody JsonNode identifiers) {
        termedService.removeTypes(graphId, identifiers);
    }

    @RequestMapping(value = "/graphs", method = GET)
    JsonNode getGraphs() {
        return termedService.getGraphs();
    }

    @RequestMapping(value = "/graph", method = POST)
    void createGraph(@RequestBody JsonNode graph) {
        termedService.createGraph(graph);
    }

    @RequestMapping(value = "/graph", method = DELETE)
    void deleteGraph(@RequestParam String graphId) {
        termedService.deleteGraph(graphId);
    }

    @RequestMapping(value = "/searchConcept", method = POST)
    String searchConcept(@RequestBody JsonNode query) {
        return elasticSearchService.searchConcept(query);
    }
}
