package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.NotFoundException;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fi.vm.yti.security.AuthorizationException.check;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static fi.vm.yti.terminology.api.util.JsonUtils.findSingle;
import static fi.vm.yti.terminology.api.util.JsonUtils.requireSingle;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Service
public class FrontendTermedService {

    private static final String USER_PASSWORD = "user";

    private final TermedRequester termedRequester;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final String namespaceRoot;

    @Autowired
    public FrontendTermedService(TermedRequester termedRequester,
                                 AuthenticatedUserProvider userProvider,
                                 AuthorizationManager authorizationManager,
                                 @Value("${namespace.root}") String namespaceRoot) {
        this.termedRequester = termedRequester;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
        this.namespaceRoot = namespaceRoot;
    }

    boolean isNamespaceInUse(String prefix) {

        String namespace = formatNamespace(prefix);

        for (Graph graph : getGraphs()) {
            if (prefix.equals(graph.getCode()) || namespace.equals(graph.getUri())) {
                return true;
            }
        }

        return false;
    }

    @NotNull JsonNode getVocabulary(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");

        params.add("where",
                "graph.id:" + graphId +
                        " AND (type.id:" + Vocabulary +
                        " OR type.id:" + TerminologicalVocabulary + ")");

        params.add("max", "-1");

        return requireSingle(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    @NotNull JsonNode getVocabularyList() {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("select", "references.publisher");
        params.add("select", "references.inGroup");

        params.add("where",
                "type.id:" + Vocabulary +
                        " OR type.id:" + TerminologicalVocabulary);

        params.add("max", "-1");


        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    UUID createVocabulary(UUID templateGraphId, String prefix, GenericNode vocabularyNode) {

        check(authorizationManager.canCreateVocabulary(vocabularyNode));

        List<MetaNode> templateMetaNodes = getTypes(templateGraphId);
        List<Attribute> prefLabel = vocabularyNode.getProperties().get("prefLabel");

        UUID graphId = createGraph(prefix, prefLabel);
        List<MetaNode> graphMetaNodes = mapToList(templateMetaNodes, node -> node.copyToGraph(graphId));

        updateTypes(graphId, graphMetaNodes);
        updateAndDeleteInternalNodes(new GenericDeleteAndSave(emptyList(), singletonList(vocabularyNode.copyToGraph(graphId))));

        return graphId;
    }

    void deleteVocabulary(UUID graphId) {

        check(authorizationManager.canDeleteVocabulary(graphId));

        removeNodes(true, false, getAllNodeIdentifiers(graphId));
        removeTypes(graphId, getTypes(graphId));
        deleteGraph(graphId);
    }

    @NotNull JsonNode getConcept(UUID graphId, UUID conceptId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("select", "referrers.*");
        params.add("select", "referrers.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "id:" + conceptId);
        params.add("max", "-1");

        JsonNode response = termedRequester.exchange("/node-trees", GET, params, JsonNode.class);
        JsonNode concept = findSingle(response);

        if (concept == null) {
            throw new NotFoundException(graphId, conceptId);
        }

        return concept;
    }

    @NotNull JsonNode getCollection(UUID graphId, UUID collectionId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "id:" + collectionId);
        params.add("max", "-1");

        JsonNode response = termedRequester.exchange("/node-trees", GET, params, JsonNode.class);
        JsonNode collection = findSingle(response);

        if (collection == null) {
            throw new NotFoundException(graphId, collectionId);
        }

        return collection;
    }

    @NotNull JsonNode getCollectionList(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.prefLabel");
        params.add("select", "properties.status");
        params.add("select", "lastModifiedDate");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + "Collection");
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    @NotNull JsonNode getNodeListWithoutReferencesOrReferrers(NodeType nodeType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("where", "type.id:" + nodeType);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }


    void bulkChange(GenericDeleteAndSave deleteAndSave) {

        check(authorizationManager.canModifyNodes(deleteAndSave.getSave()));
        check(authorizationManager.canRemoveNodes(deleteAndSave.getDelete()));

        updateAndDeleteInternalNodes(deleteAndSave);
    }

    void removeNodes(boolean sync, boolean disconnect, List<Identifier> identifiers) {

        check(authorizationManager.canRemoveNodes(identifiers));

        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("disconnect", Boolean.toString(disconnect));
        params.add("sync", Boolean.toString(sync));

        String username = ensureTermedUser();

        termedRequester.exchange("/nodes", HttpMethod.DELETE, params, String.class, identifiers, username, USER_PASSWORD);
    }

    @NotNull List<MetaNode> getTypes(UUID graphId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = graphId != null ? "/graphs/" + graphId + "/types" : "/types";

        return requireNonNull(termedRequester.exchange(path, GET, params, new ParameterizedTypeReference<List<MetaNode>>() {}));
    }

    @NotNull List<Graph> getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/graphs", GET, params, new ParameterizedTypeReference<List<Graph>>() {}));
    }

    @NotNull Graph getGraph(UUID graphId) {
        return requireNonNull(termedRequester.exchange("/graphs/" + graphId, GET, Parameters.empty(), Graph.class));
    }

    private @NotNull List<Identifier> getAllNodeIdentifiers(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "graph.id:" + graphId);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, new ParameterizedTypeReference<List<Identifier>>() {}));
    }

    private UUID createGraph(String prefix, List<Attribute> prefLabel) {

        UUID graphId = UUID.randomUUID();
        String code = prefix;
        String uri = formatNamespace(prefix);
        List<String> roles = emptyList();
        Map<String, List<Permission>> permissions = emptyMap();
        Map<String, List<Attribute>> properties = singletonMap("prefLabel", prefLabel);

        Graph graph = new Graph(graphId, code, uri, roles, permissions, properties);

        termedRequester.exchange("/graphs", POST, Parameters.empty(), String.class, graph);

        return graphId;
    }

    private void updateAndDeleteInternalNodes(GenericDeleteAndSave deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        String username = ensureTermedUser();

        this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave, username, USER_PASSWORD);
    }

    private void deleteGraph(UUID graphId) {
        termedRequester.exchange("/graphs/" + graphId, HttpMethod.DELETE, Parameters.empty(), String.class);
    }

    private void updateTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", POST, params, String.class, metaNodes);
    }

    private void removeTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", HttpMethod.DELETE, params, String.class, metaNodes);
    }

    private String ensureTermedUser() {

        YtiUser user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("Logged in user needed for the operation");
        }

        if (findTermedUser(user) == null) {
            createTermedUser(user);
        }

        return user.getEmail();
    }

    private @Nullable TermedUser findTermedUser(YtiUser user) {
        Parameters params = Parameters.single("username", user.getEmail());
        return termedRequester.exchange("/users", GET, params, TermedUser.class);
    }

    private void createTermedUser(YtiUser user) {
        Parameters params = Parameters.single("sync", "true");
        TermedUser termedUser = new TermedUser(user.getUsername(), USER_PASSWORD, "ADMIN");
        termedRequester.exchange("/users", POST, params, String.class, termedUser);
    }

    private String formatNamespace(@NotNull String prefix) {
        return this.namespaceRoot + prefix + '/';
    }
}
