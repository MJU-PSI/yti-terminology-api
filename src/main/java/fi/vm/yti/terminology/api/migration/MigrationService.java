package fi.vm.yti.terminology.api.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fi.vm.yti.terminology.api.migration.DomainIndex.SCHEMA_GRAPH_ID;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.prefLabel;
import static fi.vm.yti.terminology.api.util.CollectionUtils.filterToList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.*;

@Service
/*
 *  NOTE: usage of node-trees api is not probably a good idea here because termed index might not have been initialized yet when running migrations
 */
public class MigrationService {

    private static Logger log = LoggerFactory.getLogger(MigrationService.class);
    private final TermedRequester termedRequester;
    private final ObjectMapper objectMapper;

    @Autowired
    MigrationService(TermedRequester termedRequester,
                     ObjectMapper objectMapper) {
        this.termedRequester = termedRequester;
        this.objectMapper = objectMapper;
    }

    public void changeMetaLabel(VocabularyNodeType vocabularyType, NodeType nodeType, String fi, String en) {
        changeMetaLabel(findGraphsForVocabularyType(vocabularyType), nodeType, fi, en);
    }

    public void changeMetaLabel(List<Graph> graphs, NodeType nodeType, String fi, String en) {

        for (Graph graph : graphs) {
            MetaNode type = getType(new TypeId(nodeType, new GraphId(graph.getId())));
            type.updateProperties(prefLabel(fi, en));
            updateType(graph.getId(), type);
        }
    }

    public void changeMetaLabel(VocabularyNodeType vocabularyType, NodeType nodeType, String attributeName, String fi, String en) {
        changeMetaLabel(findGraphsForVocabularyType(vocabularyType), nodeType, attributeName, fi, en);
    }

    public void changeMetaLabel(List<Graph> graphs, NodeType nodeType, String attributeName, String fi, String en) {

        for (Graph graph : graphs) {

            MetaNode type = getType(new TypeId(nodeType, new GraphId(graph.getId())));

            Optional<AttributeMeta> textAttribute = type.getTextAttributes().stream().filter(a -> a.getId().equals(attributeName)).findAny();

            if (textAttribute.isPresent()) {
                textAttribute.get().updateProperties(prefLabel(fi, en));
            } else {
                Optional<ReferenceMeta> referenceAttribute = type.getReferenceAttributes().stream().filter(a -> a.getId().equals(attributeName)).findAny();

                if (!referenceAttribute.isPresent()) {
                    throw new RuntimeException("Attribute not found with name: " + attributeName);
                }

                referenceAttribute.get().updateProperties(prefLabel(fi, en));
            }

            updateType(graph.getId(), type);
        }
    }

    public void createGraph(Graph graph) {
        termedRequester.exchange("/graphs", POST, Parameters.empty(), String.class, graph);
    }

    public void updateAndDeleteInternalNodes(GenericDeleteAndSave deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "false");

        this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave);
    }

    public void updateTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", POST, params, String.class, metaNodes);
    }

    public void updateType(UUID graphId, MetaNode metaNode) {
        termedRequester.exchange("/graphs/" + graphId + "/types/" + metaNode.getId(), PUT, Parameters.empty(), String.class, metaNode);
    }

    public @Nullable MetaNode findType(TypeId typeId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = "/graphs/" + typeId.getGraphId() + "/types/" + typeId.getId().name();

        return termedRequester.exchange(path, GET, params, MetaNode.class);
    }

    public @NotNull MetaNode getType(TypeId typeId) {
        return requireNonNull(findType(typeId));
    }

    public List<Graph> findGraphsForVocabularyType(VocabularyNodeType vocabularyType) {
        return filterToList(getGraphs(), g -> findType(new TypeId(vocabularyType.asNodeType(), new GraphId(g.getId()))) != null);
    }

    public List<Graph> getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/graphs", GET, params, new ParameterizedTypeReference<List<Graph>>() {}));
    }

    public void updateNodesWithJson(Resource resource) {
        try {
            updateNodesWithJson(objectMapper.readTree(resource.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateNodesWithJson(JsonNode json) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        this.termedRequester.exchange("/nodes", POST, params, String.class, json);
    }

    public boolean isSchemaInitialized() {
        log.info("Checking if schema is initialized!");
        return termedRequester.exchange("/graphs/" + SCHEMA_GRAPH_ID, GET, Parameters.empty(), Graph.class) != null;
    }

    public List<GenericNode> getNodes(TypeId domain) {
        String url = "/graphs/" + domain.getGraphId() + "/types/" + domain.getId().name() + "/nodes/";
        List<GenericNode> result = termedRequester.exchange(url, GET, Parameters.empty(), new ParameterizedTypeReference<List<GenericNode>>() {});

        return result != null ? result : emptyList();
    }

    public GenericNode getNode(TypeId domain, UUID id) {
        String url = "/graphs/" + domain.getGraphId() + "/types/" + domain.getId().name() + "/nodes/" + id;
        return requireNonNull(termedRequester.exchange(url, GET, Parameters.empty(), GenericNode.class));
    }
}
