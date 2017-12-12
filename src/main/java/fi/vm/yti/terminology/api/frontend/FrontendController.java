package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.model.termed.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static fi.vm.yti.terminology.api.model.termed.NodeType.Group;
import static fi.vm.yti.terminology.api.model.termed.NodeType.Organization;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/frontend")
public class FrontendController {

    private final FrontendTermedService termedService;
    private final FrontendElasticSearchService elasticSearchService;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final String namespaceRoot;

    public FrontendController(FrontendTermedService termedService,
                              FrontendElasticSearchService elasticSearchService,
                              FrontendGroupManagementService groupManagementService,
                              AuthenticatedUserProvider userProvider,
                              @Value("${namespace.root}") String namespaceRoot) {
        this.termedService = termedService;
        this.elasticSearchService = elasticSearchService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.namespaceRoot = namespaceRoot;
    }

    @RequestMapping(value = "/fakeableUsers", method = GET, produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUser> getFakeableUsers() {
        return groupManagementService.getUsers();
    }

    @RequestMapping(value = "/namespaceInUse", method = GET, produces = APPLICATION_JSON_VALUE)
    boolean isNamespaceInUse(@RequestParam String prefix) {
        return termedService.isNamespaceInUse(prefix);
    }

    @RequestMapping(value = "/namespaceRoot", method = GET, produces = APPLICATION_JSON_VALUE)
    String getNamespaceRoot() {
        return namespaceRoot;
    }

    @RequestMapping(value = "/authenticated-user", method = GET, produces = APPLICATION_JSON_VALUE)
    YtiUser getUser() {
        return userProvider.getUser();
    }

    @RequestMapping(value = "/requests", method = GET, produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUserRequest> getUserRequests() {
        return groupManagementService.getUserRequests();
    }

    @RequestMapping(value = "/request", method = POST)
    void sendRequest(@RequestParam UUID organizationId) {
        groupManagementService.sendRequest(organizationId);
    }

    @RequestMapping(value = "/vocabulary", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabulary(@RequestParam UUID graphId) {
        return termedService.getVocabulary(graphId);
    }

    @RequestMapping(value = "/vocabularies", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabularyList() {
        return termedService.getVocabularyList();
    }

    @RequestMapping(value = "/vocabulary", method = POST)
    UUID createVocabulary(@RequestParam UUID templateGraphId,
                          @RequestParam String prefix,
                          @RequestBody GenericNode vocabularyNode) {
        return termedService.createVocabulary(templateGraphId, prefix, vocabularyNode);
    }

    @RequestMapping(value = "/vocabulary", method = DELETE)
    void deleteVocabulary(@RequestParam UUID graphId) {
        termedService.deleteVocabulary(graphId);
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
        return termedService.getNodeListWithoutReferencesOrReferrers(Organization);
    }

    @RequestMapping(value = "/groups", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getGroupList() {
        return termedService.getNodeListWithoutReferencesOrReferrers(Group);
    }

    @RequestMapping(value = "/modify", method = POST)
    void updateAndDeleteInternalNodes(@RequestBody GenericDeleteAndSave deleteAndSave) {
        termedService.bulkChange(deleteAndSave);
    }

    @RequestMapping(value = "/remove", method = DELETE)
    void removeNodes(@RequestParam boolean sync,
                     @RequestParam boolean disconnect,
                     @RequestBody List<Identifier> identifiers) {
        termedService.removeNodes(sync, disconnect, identifiers);
    }

    @RequestMapping(value = "/types", method = GET, produces = APPLICATION_JSON_VALUE)
    List<MetaNode> getTypes(@RequestParam(required = false) UUID graphId) {
        return termedService.getTypes(graphId);
    }

    @RequestMapping(value = "/graphs", method = GET, produces = APPLICATION_JSON_VALUE)
    List<Graph> getGraphs() {
        return termedService.getGraphs();
    }

    @RequestMapping(value = "/graphs/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    Graph getGraph(@PathVariable("id") UUID graphId) {
        return termedService.getGraph(graphId);
    }

    @RequestMapping(value = "/searchConcept", method = POST, produces = APPLICATION_JSON_VALUE)
    String searchConcept(@RequestBody JsonNode query) {
        return elasticSearchService.searchConcept(query);
    }
}
