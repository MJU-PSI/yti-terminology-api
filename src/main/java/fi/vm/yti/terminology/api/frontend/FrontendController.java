package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.model.termed.TermedGraph;
import fi.vm.yti.terminology.api.model.termed.VocabularyType;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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

    @RequestMapping(value = "/namespaceInUse", method = GET, produces = APPLICATION_JSON_VALUE)
    boolean isNamespaceInUse(@RequestParam String prefix,
                             @RequestParam String namespace) {
        return termedService.isNamespaceInUse(prefix, namespace);
    }

    @RequestMapping(value = "/authenticated-user", method = GET, produces = APPLICATION_JSON_VALUE)
    YtiUser getUser() {
        return userProvider.getUser();
    }

    @RequestMapping(value = "/vocabulary", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabulary(@RequestParam UUID graphId,
                           @RequestParam VocabularyType vocabularyType) {
        return termedService.getVocabulary(graphId, vocabularyType);
    }

    @RequestMapping(value = "/vocabularies", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabularyList(@RequestParam VocabularyType vocabularyType) {
        return termedService.getVocabularyList(vocabularyType);
    }

    @RequestMapping(value = "/concept", method = GET, produces = APPLICATION_JSON_VALUE)
    @Nullable JsonNode getConcept(@RequestParam UUID graphId,
                                  @RequestParam UUID conceptId) {
        return termedService.getConcept(graphId, conceptId);
    }

    @RequestMapping(value = "/collection", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getCollection(@RequestParam UUID graphId,
                           @RequestParam UUID collectionId) {
        return termedService.getCollection(graphId, collectionId);
    }

    @RequestMapping(value = "/collections", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getCollectionList(@RequestParam UUID graphId) {
        return termedService.getCollectionList(graphId);
    }

    @RequestMapping(value = "/organizations", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getOrganizationList() {
        return termedService.getNodeListWithoutReferencesOrReferrers("Organization");
    }

    @RequestMapping(value = "/groups", method = GET, produces = APPLICATION_JSON_VALUE)
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

    @RequestMapping(value = "/nodes", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getAllNodeIdentifiers(@RequestParam UUID graphId) {
        return termedService.getAllNodeIdentifiers(graphId);
    }

    @RequestMapping(value = "/types", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getTypes(@RequestParam(required = false) UUID graphId) {
        return termedService.getTypes(graphId);
    }

    @RequestMapping(value = "/types", method = POST)
    void updateTypes(@RequestParam UUID graphId,
                     @RequestBody JsonNode metaNodes) {
        termedService.updateTypes(graphId, metaNodes);
    }

    @RequestMapping(value = "/types", method = DELETE)
    void removeTypes(@RequestParam UUID graphId,
                     @RequestBody JsonNode identifiers) {
        termedService.removeTypes(graphId, identifiers);
    }

    @RequestMapping(value = "/graphs", method = GET, produces = APPLICATION_JSON_VALUE)
    List<TermedGraph> getGraphs() {
        return termedService.getGraphs();
    }

    @RequestMapping(value = "/graph", method = POST)
    void createGraph(@RequestBody JsonNode graph) {
        termedService.createGraph(graph);
    }

    @RequestMapping(value = "/graph", method = DELETE)
    void deleteGraph(@RequestParam UUID graphId) {
        termedService.deleteGraph(graphId);
    }

    @RequestMapping(value = "/searchConcept", method = POST, produces = APPLICATION_JSON_VALUE)
    String searchConcept(@RequestBody JsonNode query) {
        return elasticSearchService.searchConcept(query);
    }
}
