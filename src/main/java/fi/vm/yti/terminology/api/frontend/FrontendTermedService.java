package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.NotFoundException;
import fi.vm.yti.terminology.api.util.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static fi.vm.yti.terminology.api.util.JsonUtils.findSingle;
import static fi.vm.yti.terminology.api.util.JsonUtils.requireSingle;

@Service
public class FrontendTermedService {

    private final TermedRequester termedRequester;

    @Autowired
    public FrontendTermedService(TermedRequester termedRequester) {
        this.termedRequester = termedRequester;
    }

    JsonNode getVocabulary(String graphId, String vocabularyType) {

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

        return requireSingle(termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class).getBody());
    }

    JsonNode getVocabularyList(String vocabularyType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("select", "references.publisher");
        params.add("select", "references.inGroup");
        params.add("where", "type.id:" + vocabularyType);
        params.add("max", "-1");

        return termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class).getBody();
    }

    JsonNode getConcept(String graphId, String conceptId) {

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

        ResponseEntity<JsonNode> response = termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class);

        JsonNode concept = findSingle(response.getBody());

        if (concept == null) {
            throw new NotFoundException(graphId, conceptId);
        }

        return concept;
    }

    JsonNode getCollection(String graphId, String collectionId) {

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

        ResponseEntity<JsonNode> response = termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class);

        return requireSingle(response.getBody());
    }

    JsonNode getCollectionList(String graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.prefLabel");
        params.add("select", "properties.status");
        params.add("select", "lastModifiedDate");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + "Collection");
        params.add("max", "-1");

        return termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class).getBody();
    }

    JsonNode getNodeListWithoutReferencesOrReferrers(String nodeType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("where", "type.id:" + nodeType);
        params.add("max", "-1");

        return termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class).getBody();
    }

    // TODO: better typing for easy authorization
    void updateAndDeleteInternalNodes(JsonNode deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        // TODO user authenticated user credentials
        this.termedRequester.exchange("/nodes", HttpMethod.POST, params, String.class, deleteAndSave);
    }

    // TODO: better typing for easy authorization
    void removeNodes(boolean sync, JsonNode identifiers) {

        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("sync", Boolean.toString(sync));

        // TODO user authenticated user credentials
        this.termedRequester.exchange("/nodes", HttpMethod.DELETE, params, String.class, identifiers);
    }

    JsonNode getAllNodeIdentifiers(String graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "graph.id:" + graphId);
        params.add("max", "-1");

        return termedRequester.exchange("/node-trees", HttpMethod.GET, params, JsonNode.class).getBody();
    }

    JsonNode getTypes(String graphId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = graphId != null ? "/graphs/" + graphId + "/types" : "/types";

        return termedRequester.exchange(path, HttpMethod.GET, params, JsonNode.class).getBody();
    }

    // TODO: better typing for easy authorization
    void updateTypes(String graphId, JsonNode metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        this.termedRequester.exchange("/graphs/" + graphId + "/types", HttpMethod.POST, params, String.class, metaNodes);
    }

    // TODO: better typing for easy authorization
    void removeTypes(String graphId, JsonNode identifiers) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        this.termedRequester.exchange("/graphs/" + graphId + "/types", HttpMethod.DELETE, params, String.class, identifiers);
    }

    JsonNode getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        return termedRequester.exchange("/graphs", HttpMethod.GET, params, JsonNode.class).getBody();
    }

    // TODO: better typing for easy authorization
    void createGraph(JsonNode graph) {
        termedRequester.exchange("/graphs", HttpMethod.POST, Parameters.empty(), String.class, graph);
    }

    void deleteGraph(String graphId) {
        termedRequester.exchange("/graphs/" + graphId, HttpMethod.DELETE, Parameters.empty(), String.class);
    }
}
