package fi.vm.yti.terminology.api.migration;

import static fi.vm.yti.terminology.api.migration.DomainIndex.SCHEMA_GRAPH_ID;
import static fi.vm.yti.terminology.api.util.CollectionUtils.filterToList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.GraphId;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import fi.vm.yti.terminology.api.util.Parameters;

@Service
/*
 * NOTE: usage of node-trees api is not probably a good idea here because termed
 * index might not have been initialized yet when running migrations
 */
public class MigrationService {

    private static Logger log = LoggerFactory.getLogger(MigrationService.class);
    private final TermedRequester termedRequester;
    private final ObjectMapper objectMapper;

    @Autowired
    MigrationService(TermedRequester termedRequester, ObjectMapper objectMapper) {
        this.termedRequester = termedRequester;
        this.objectMapper = objectMapper;
    }

    public void createGraph(Graph graph) {
        termedRequester.exchange("/graphs", POST, Parameters.empty(), String.class, graph);
    }

    public void deleteVocabularyGraph(UUID graphId) {
        log.info("deleteVocabularyGraph: graphId=" + graphId);
        if (findGraph(graphId) != null) {
            removeNodes(true, false, graphId, getAllNodeIdentifiers(graphId));
            log.info("deleteVocabularyGraph: after nodes removed getTypes=" + getTypes(graphId));
            removeTypes(graphId, getTypes(graphId));
            log.info("deleteVocabularyGraph: after types removed");
            deleteGraph(graphId);
        }
    }

    private void deleteGraph(UUID graphId) {
        termedRequester.exchange("/graphs/" + graphId, DELETE, Parameters.empty(), String.class);
    }

    private @NotNull List<Identifier> getAllNodeIdentifiers(UUID graphId) {
        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "graph.id:" + graphId);
        params.add("max", "-1");

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params,
                new ParameterizedTypeReference<List<Identifier>>() {
                }));
    }

    public void removeNodes(boolean sync, boolean disconnect, UUID graphId, List<Identifier> identifiers) {
        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("disconnect", Boolean.toString(disconnect));
        params.add("sync", Boolean.toString(sync));
        log.info("RemoveNodes identifiers:" + identifiers);
        identifiers.forEach(id -> {
            System.out.println("Graph:" + graphId + " Remove node:" + id.getId().toString());
        });
        termedRequester.exchange("/nodes", DELETE, params, String.class, identifiers, "admin", "user");
    }

    private void removeTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", DELETE, params, String.class, metaNodes);
    }

    public void updateAndDeleteInternalNodes(GenericDeleteAndSave deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "false");

        this.termedRequester.exchange("/nodes", POST, params, String.class, deleteAndSave);
    }

    public @Nullable MetaNode findType(TypeId typeId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = "/graphs/" + typeId.getGraphId() + "/types/" + typeId.getId().name();

        return termedRequester.exchange(path, GET, params, MetaNode.class);
    }

    public @Nullable MetaNode findGraph(UUID graphId) {
        Parameters params = new Parameters();
        params.add("max", "-1");
        String path = "/graphs/" + graphId;
        log.info("findGraph(" + graphId + ") called");
        return termedRequester.exchange(path, GET, params, MetaNode.class);
    }

    public void updateTypes(VocabularyNodeType vocabularyNodeType, Consumer<MetaNode> modifier) {
        findGraphIdsForVocabularyType(vocabularyNodeType).forEach(graphId -> updateTypes(graphId, modifier));
    }

    public void updateTypes(VocabularyNodeType vocabularyNodeType, NodeType nodeType, Consumer<MetaNode> modifier) {
        findGraphIdsForVocabularyType(vocabularyNodeType).forEach(graphId -> updateTypes(graphId, nodeType, modifier));
    }

    public void updateTypes(VocabularyNodeType vocabularyNodeType, Predicate<MetaNode> filter,
            Consumer<MetaNode> modifier) {
        findGraphIdsForVocabularyType(vocabularyNodeType).forEach(graphId -> updateTypes(graphId, filter, modifier));
    }

    public void updateTypes(UUID graphId, Consumer<MetaNode> modifier) {
        updateTypes(graphId, type -> true, modifier);
    }

    public void updateTypes(UUID graphId, NodeType nodeType, Consumer<MetaNode> modifier) {
        updateTypes(graphId, type -> type.isOfType(nodeType), modifier);
    }

    public void updateTypes(UUID graphId, Predicate<MetaNode> filter, Consumer<MetaNode> modifier) {

        List<MetaNode> types = filterToList(getTypes(graphId), filter);

        for (MetaNode metaNode : types) {
            modifier.accept(metaNode);
        }

        updateTypes(graphId, types);
    }

    public void updateTypes(UUID graphId, List<MetaNode> metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        termedRequester.exchange("/graphs/" + graphId + "/types", POST, params, String.class, metaNodes);
    }

    public @NotNull List<MetaNode> getTypes(UUID graphId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String path = graphId != null ? "/graphs/" + graphId + "/types" : "/types";

        return requireNonNull(
                termedRequester.exchange(path, GET, params, new ParameterizedTypeReference<List<MetaNode>>() {
                }));
    }

    public @NotNull MetaNode getType(TypeId typeId) {
        return requireNonNull(findType(typeId));
    }

    public List<UUID> findGraphIdsForVocabularyType(VocabularyNodeType vocabularyType) {
        return getGraphs().stream()
                .filter(g -> findType(new TypeId(vocabularyType.asNodeType(), new GraphId(g.getId()))) != null)
                .map(Graph::getId).collect(toList());
    }

    public List<Graph> getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        return requireNonNull(
                termedRequester.exchange("/graphs", GET, params, new ParameterizedTypeReference<List<Graph>>() {
                }));
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
        List<GenericNode> result = termedRequester.exchange(url, GET, Parameters.empty(),
                new ParameterizedTypeReference<List<GenericNode>>() {
                });

        return result != null ? result : emptyList();
    }

    public GenericNode getNode(TypeId domain, UUID id) {
        String url = "/graphs/" + domain.getGraphId() + "/types/" + domain.getId().name() + "/nodes/" + id;
        return requireNonNull(termedRequester.exchange(url, GET, Parameters.empty(), GenericNode.class));
    }

    public void deleteTypes(VocabularyNodeType vocabularyNodeType, String typeName) {
        log.info("DeleteTypes id:" + vocabularyNodeType.toString() + " type:" + typeName);
        try {
            UUID id = UUID.fromString(vocabularyNodeType.toString());
            if (id != null) {
                findGraphIdsForVocabularyType(vocabularyNodeType).forEach(graphId -> deleteType(graphId, typeName));
            }
        } catch (IllegalArgumentException iaex) {
            System.out.println("id not UUID " + vocabularyNodeType.toString());
        }
    }

    public void deleteType(UUID graphId, String typeName) {
        log.info("DeleteType id:" + graphId.toString() + " type:" + typeName);
        termedRequester.exchange("/graphs/" + graphId + "/types/" + typeName, DELETE, Parameters.empty(), String.class);
    }

    public List<GenericNode> getNodes(Predicate<GenericNode> filter) {
        Parameters parameters = new Parameters();
        parameters.add("max", "-1");

        List<GenericNode> nodes = termedRequester.exchange("/nodes/", GET, parameters,
                new ParameterizedTypeReference<>() {
        });
        return nodes.stream()
                .filter(filter)
                .collect(toList());
    }

}
