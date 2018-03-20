package fi.vm.yti.terminology.api.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static fi.vm.yti.terminology.api.migration.DomainIndex.SCHEMA_GRAPH_ID;
import static fi.vm.yti.terminology.api.util.CollectionUtils.requireSingle;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.*;

@Service
public class MigrationService {

    private final TermedRequester termedRequester;
    private final ObjectMapper objectMapper;

    @Autowired
    MigrationService(TermedRequester termedRequester,
                     ObjectMapper objectMapper) {
        this.termedRequester = termedRequester;
        this.objectMapper = objectMapper;
    }

    public void createGraph(Graph graph) {
        termedRequester.exchange("/graphs", POST, Parameters.empty(), String.class, graph);
    }

    public void updateAndDeleteInternalNodes(GenericDeleteAndSave deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave);
    }

    public void updateTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", POST, params, String.class, metaNodes);
    }

    public void removeTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", DELETE, params, String.class, metaNodes);
    }

    public void updateNodesWithJson(Resource resource) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        try {
            JsonNode json = objectMapper.readTree(resource.getInputStream());
            this.termedRequester.exchange("/nodes", POST, params, String.class, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSchemaInitialized() {
        return termedRequester.exchange("/graphs/" + SCHEMA_GRAPH_ID, GET, Parameters.empty(), Graph.class) != null;
    }

    public GenericNode getNode(TypeId domain, UUID id) {

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
        params.add("select", "referrers.*");
        params.add("where", "graph.id:" + domain.getGraphId());
        params.add("where", "type.id:" + domain.getId().name());
        params.add("where", "id:" + id);

        return requireSingle(requireNonNull(termedRequester.exchange("/node-trees", GET, params, new ParameterizedTypeReference<List<GenericNode>>() {})));
    }
}
