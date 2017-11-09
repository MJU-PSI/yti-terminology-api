package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.NotFoundException;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static fi.vm.yti.terminology.api.util.JsonUtils.findSingle;
import static fi.vm.yti.terminology.api.util.JsonUtils.requireSingle;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Service
public class FrontendTermedService {

    private static final String USER_PASSWORD = "user";

    private final TermedRequester termedRequester;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public FrontendTermedService(TermedRequester termedRequester,
                                 AuthenticatedUserProvider userProvider) {
        this.termedRequester = termedRequester;
        this.userProvider = userProvider;
    }

    boolean isNamespaceInUse(String prefix, String namespace) {

        for (Graph graph : getGraphs()) {
            if (prefix.equals(graph.getCode()) || namespace.equals(graph.getUri())) {
                return true;
            }
        }

        return false;
    }

    @NotNull JsonNode getVocabulary(UUID graphId, VocabularyType vocabularyType) {

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
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + vocabularyType);
        params.add("max", "-1");

        return requireSingle(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    @NotNull JsonNode getVocabularyList(VocabularyType vocabularyType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("select", "references.publisher");
        params.add("select", "references.inGroup");
        params.add("where", "type.id:" + vocabularyType);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
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

        return requireSingle(response);
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

    void updateAndDeleteInternalNodes(DeleteAndSave deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        String username = ensureTermedUser();

        this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave, username, USER_PASSWORD);
    }

    void removeNodes(boolean sync, boolean disconnect, List<Identifier> identifiers) {

        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("disconnect", Boolean.toString(disconnect));
        params.add("sync", Boolean.toString(sync));

        String username = ensureTermedUser();

        termedRequester.exchange("/nodes", HttpMethod.DELETE, params, String.class, identifiers, username, USER_PASSWORD);
    }

    @NotNull JsonNode getAllNodeIdentifiers(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "graph.id:" + graphId);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
    }

    @NotNull List<MetaNode> getTypes(UUID graphId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = graphId != null ? "/graphs/" + graphId + "/types" : "/types";

        return requireNonNull(termedRequester.exchange(path, GET, params, new ParameterizedTypeReference<List<MetaNode>>() {}));
    }

    void updateTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", POST, params, String.class, metaNodes);
    }

    void removeTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", HttpMethod.DELETE, params, String.class, metaNodes);
    }

    @NotNull List<Graph> getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/graphs", GET, params, new ParameterizedTypeReference<List<Graph>>() {}));
    }

    void createGraph(Graph graph) {
        termedRequester.exchange("/graphs", POST, Parameters.empty(), String.class, graph);
    }

    void deleteGraph(UUID graphId) {
        termedRequester.exchange("/graphs/" + graphId, HttpMethod.DELETE, Parameters.empty(), String.class);
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
}
