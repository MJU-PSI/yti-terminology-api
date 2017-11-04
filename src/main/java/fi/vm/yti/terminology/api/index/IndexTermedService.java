package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.vm.yti.terminology.api.util.JsonUtils.*;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpMethod.GET;

@Service
public class IndexTermedService {

    private static final Logger log = LoggerFactory.getLogger(IndexTermedService.class);
    private final TermedRequester termedRequester;

    @Autowired
    public IndexTermedService(TermedRequester termedRequester) {

        this.termedRequester = termedRequester;
    }

	public boolean deleteChangeListener(@NotNull String hookId) {
        return termedRequester.exchange("/hooks/" + hookId, HttpMethod.DELETE, Parameters.empty(), String.class).getBody() != null;
	}

	public @Nullable String registerChangeListener(@NotNull String url) {

        ResponseEntity<String> response = termedRequester.exchange("/hooks", HttpMethod.POST, Parameters.single("url", url), String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody()
                    .replace("<string>", "")
                    .replace("</string>", "");
        } else {
            return null;
        }
    }

    @NotNull List<String> fetchAllAvailableGraphIds() {

        log.info("Fetching all graph IDs..");

        return asStream(termedRequester.exchange("/graphs", GET, Parameters.empty(), JsonNode.class).getBody())
                .map(x -> x.get("id").textValue())
                .collect(toList());
    }

	@NotNull List<Concept> getAllConceptsForGraph(@NotNull String graphId) {

	    AllNodesResult allNodesResult = this.fetchAllNodesInGraph(graphId);

        Optional<String> vocabularyNodeId = allNodesResult.getVocabularyNodeId();

        if (vocabularyNodeId.isPresent()) {
            return allNodesResult.getConceptNodeIds().stream()
                    .map(conceptId -> Concept.createFromAllNodeResult(conceptId, vocabularyNodeId.get(), allNodesResult))
                    .collect(toList());
        } else {
            log.warn("Vocabulary not found for graph: " + graphId);
            return emptyList();
        }
	}

    @NotNull List<Concept> getConcepts(@NotNull String graphId, @NotNull Collection<String> ids) {

	    if (ids.isEmpty()) {
	        return emptyList();
        }

        Vocabulary vocabulary = getVocabulary(graphId);

        if (vocabulary != null) {
            return getConcepts(vocabulary, ids);
        } else {
            return emptyList();
        }
    }

    private @NotNull List<Concept> getConcepts(@NotNull Vocabulary vocabulary, @NotNull Collection<String> conceptIds) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.prefLabel");
        params.add("select", "properties.definition");
        params.add("select", "properties.status");
        params.add("select", "references.prefLabelXl:2");
        params.add("select", "references.altLabelXl:2");
        params.add("select", "references.broader");
        params.add("select", "referrers.broader");
        params.add("where", "graph.id:" + vocabulary.getGraphId());
        params.add("where", idInCollectionWhereClause(conceptIds));
        params.add("max", "-1");

        return asStream(termedRequester.exchange("/node-trees", GET, params, JsonNode.class).getBody())
                .map(json -> Concept.createFromExtJson(json, vocabulary))
                .collect(toList());
    }

    private static @NotNull String idInCollectionWhereClause(@NotNull Collection<String> conceptIds) {

	    return conceptIds.stream()
                .map(conceptId -> "id:" + conceptId)
                .collect(Collectors.joining(" OR "));
    }

    private @Nullable Vocabulary getVocabulary(@NotNull String graphId) {

	    JsonNode vocabularyNode = getVocabularyNode(graphId);

        if (vocabularyNode != null) {
            return Vocabulary.createFromExtJson(vocabularyNode);
        } else {
            log.warn("Vocabulary not found for graph " + graphId);
            return null;
        }
    }

    private @Nullable JsonNode getVocabularyNode(@NotNull String graphId) {

        JsonNode json = getVocabularyNode(graphId, VocabularyType.TerminologicalVocabulary);

        if (json != null) {
            return json;
        } else {
            log.info("Vocabulary for graph " + graphId + " was not found as type " + VocabularyType.TerminologicalVocabulary.name() + ". Trying to find as type " + VocabularyType.Vocabulary.name());
            return getVocabularyNode(graphId, VocabularyType.Vocabulary);
        }
    }

    private @Nullable JsonNode getVocabularyNode(@NotNull String graphId, @NotNull VocabularyType vocabularyType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + vocabularyType.name());
        params.add("max", "-1");

        return findSingle(termedRequester.exchange("/node-trees", GET, params, JsonNode.class).getBody());
    }

    private @NotNull AllNodesResult fetchAllNodesInGraph(String graphId) {

        Parameters params = Parameters.single("max", "-1");
        ResponseEntity<JsonNode> response = termedRequester.exchange("/graphs/" + graphId + "/nodes", GET, params, JsonNode.class);
        return new AllNodesResult(response.getBody());
    }
}