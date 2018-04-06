package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.model.termed.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final String groupManagementUrl;
    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);

    public FrontendController(FrontendTermedService termedService,
                              FrontendElasticSearchService elasticSearchService,
                              FrontendGroupManagementService groupManagementService,
                              AuthenticatedUserProvider userProvider,
                              @Value("${namespace.root}") String namespaceRoot,
                              @Value("${groupmanagement.public.url}") String groupManagementUrl) {
        this.termedService = termedService;
        this.elasticSearchService = elasticSearchService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.namespaceRoot = namespaceRoot;
        this.groupManagementUrl = groupManagementUrl;
    }

    @RequestMapping(value = "/groupManagementUrl", method = GET, produces = APPLICATION_JSON_VALUE)
    String getGroupManagementUrl() {
        logger.info("GET /groupManagementUrl requested");
        return groupManagementUrl;
    }

    @RequestMapping(value = "/fakeableUsers", method = GET, produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUser> getFakeableUsers() {
        logger.info("GET /fakeableUsers requested");
        return groupManagementService.getUsers();
    }

    @RequestMapping(value = "/namespaceInUse", method = GET, produces = APPLICATION_JSON_VALUE)
    boolean isNamespaceInUse(@RequestParam String prefix) {
        logger.info("GET /namespaceInUse requested with prefix: " + prefix);
        return termedService.isNamespaceInUse(prefix);
    }

    @RequestMapping(value = "/namespaceRoot", method = GET, produces = APPLICATION_JSON_VALUE)
    String getNamespaceRoot() {
        logger.info("GET /namespaceRoot requested");
        return namespaceRoot;
    }

    @RequestMapping(value = "/authenticated-user", method = GET, produces = APPLICATION_JSON_VALUE)
    YtiUser getUser() {
        logger.info("GET /authenticated-user requested");
        return userProvider.getUser();
    }

    @RequestMapping(value = "/requests", method = GET, produces = APPLICATION_JSON_VALUE)
    List<GroupManagementUserRequest> getUserRequests() {
        logger.info("GET /requests requested");
        return groupManagementService.getUserRequests();
    }

    @RequestMapping(value = "/request", method = POST)
    void sendRequest(@RequestParam UUID organizationId) {
        logger.info("POST /request requested with organizationID: " + organizationId.toString());
        groupManagementService.sendRequest(organizationId);
    }

    @RequestMapping(value = "/vocabulary", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabulary(@RequestParam UUID graphId) {
        logger.info("GET /vocabulary requested with graphId: " + graphId.toString());
        return termedService.getVocabulary(graphId);
    }

    @RequestMapping(value = "/vocabularies", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getVocabularyList() {
        logger.info("GET /vocabularies requested");
        return termedService.getVocabularyList();
    }

    @RequestMapping(value = "/vocabulary", method = POST)
    UUID createVocabulary(@RequestParam UUID templateGraphId,
                          @RequestParam String prefix,
                          @RequestBody GenericNode vocabularyNode) {
        logger.info("POST /vocabulary requested with params: templateGraphId: " +
                    templateGraphId.toString() + ", prefix: " + prefix + ", vocabularyNode.id: " + vocabularyNode.getId().toString());
        return termedService.createVocabulary(templateGraphId, prefix, vocabularyNode);
    }

    @RequestMapping(value = "/vocabulary", method = DELETE)
    void deleteVocabulary(@RequestParam UUID graphId) {
        logger.info("DELETE /vocabulary requested with graphId: " + graphId.toString());
        termedService.deleteVocabulary(graphId);
    }

    @RequestMapping(value = "/concept", method = GET, produces = APPLICATION_JSON_VALUE)
    @Nullable JsonNode getConcept(@RequestParam UUID graphId,
                                  @RequestParam UUID conceptId) {
        logger.info("GET /concept requested with params: graphId: " + graphId.toString() + ", conceptId: " + conceptId.toString());
        return termedService.getConcept(graphId, conceptId);
    }

    @RequestMapping(value = "/collection", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getCollection(@RequestParam UUID graphId,
                           @RequestParam UUID collectionId) {
        logger.info("GET /collection requested with params: graphId: " + graphId.toString() + ", collectionId: " + collectionId.toString());
        return termedService.getCollection(graphId, collectionId);
    }

    @RequestMapping(value = "/collections", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getCollectionList(@RequestParam UUID graphId) {
        logger.info("GET /collections requested with graphId: " + graphId.toString());
        return termedService.getCollectionList(graphId);
    }

    @RequestMapping(value = "/organizations", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getOrganizationList() {
        logger.info("GET /organizations requested");
        return termedService.getNodeListWithoutReferencesOrReferrers(Organization);
    }

    @RequestMapping(value = "/groups", method = GET, produces = APPLICATION_JSON_VALUE)
    JsonNode getGroupList() {
        logger.info("GET /groups requested");
        return termedService.getNodeListWithoutReferencesOrReferrers(Group);
    }

    @RequestMapping(value = "/modify", method = POST)
    void updateAndDeleteInternalNodes(@RequestBody GenericDeleteAndSave deleteAndSave) {
        logger.info("POST /modify requested with deleteAndSave: delete ids: ");
        for (int i = 0; i < deleteAndSave.getDelete().size(); i++) {
            logger.info(deleteAndSave.getDelete().get(i).getId().toString());
        }
        logger.info("and save ids: ");
        for (int i = 0; i < deleteAndSave.getSave().size(); i++) {
            logger.info(deleteAndSave.getSave().get(i).getId().toString());
        }

        termedService.bulkChange(deleteAndSave);
    }

    @RequestMapping(value = "/remove", method = DELETE)
    void removeNodes(@RequestParam boolean sync,
                     @RequestParam boolean disconnect,
                     @RequestBody List<Identifier> identifiers) {
        logger.info("DELETE /remove requested with params: sync: " + sync + ", disconnect: " + disconnect + ", identifier ids: ");
        for (final Identifier ident : identifiers) {
            logger.info(ident.getId().toString());
        }
        termedService.removeNodes(sync, disconnect, identifiers);
    }

    @RequestMapping(value = "/types", method = GET, produces = APPLICATION_JSON_VALUE)
    List<MetaNode> getTypes(@RequestParam(required = false) UUID graphId) {
        if (graphId != null) {
            logger.info("GET /types requested with graphId: " + graphId.toString());
        } else {
            logger.info("GET /types requested without graphId");
        }
        return termedService.getTypes(graphId);
    }

    @RequestMapping(value = "/graphs", method = GET, produces = APPLICATION_JSON_VALUE)
    List<Graph> getGraphs() {
        logger.info("GET /graphs requested");
        return termedService.getGraphs();
    }

    @RequestMapping(value = "/graphs/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    Graph getGraph(@PathVariable("id") UUID graphId) {
        logger.info("GET /graphs/{id} requested with graphId: " + graphId.toString());
        return termedService.getGraph(graphId);
    }

    @RequestMapping(value = "/searchConcept", method = POST, produces = APPLICATION_JSON_VALUE)
    String searchConcept(@RequestBody JsonNode query) {
        logger.info("POST /searchConcept requested with query: " + query.toString());
        return elasticSearchService.searchConcept(query);
    }
}
