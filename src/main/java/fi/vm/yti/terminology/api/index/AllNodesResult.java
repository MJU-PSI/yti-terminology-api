package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fi.vm.yti.terminology.api.util.JsonUtils.asStream;
import static java.util.function.Function.identity;

final class AllNodesResult {

    private final Map<String, JsonNode> nodes;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    AllNodesResult(@NotNull JsonNode node) {
        this.nodes = asStream(node).collect(Collectors.toMap(n -> n.get("id").textValue(), identity()));
    }

    @Nullable JsonNode getNode(@NotNull String id) {
        return getNode(id, null);
    }

    @Nullable JsonNode getNode(@NotNull String id, @Nullable String expectedType) {

        JsonNode node = this.nodes.get(id);

        if (node == null) {
            log.warn("No node found for id: " + id);
            return null;
        }

        String type = type(node);

        if (type == null) {
            log.warn("Cannot resolve type for node: " + id);
            return null;
        }

        if (expectedType != null && !type.equals(expectedType)) {
            log.warn("Expected type " + expectedType + " for node " + id + " but was " + type);
            return null;
        }

        return node;
    }

    @NotNull List<String> getConceptNodeIds() {
        return this.nodes.values().stream()
                .filter(AllNodesResult::isConceptNode)
                .map(node -> node.get("id").textValue())
                .collect(Collectors.toList());
    }

    @NotNull Optional<String> getVocabularyNodeId() {
        return this.nodes.values().stream()
                .filter(AllNodesResult::isVocabularyNode)
                .map(node -> node.get("id").textValue())
                .findFirst();
    }

    private static boolean isConceptNode(@NotNull JsonNode jsonObj) {
        return typeIs(jsonObj, "Concept");
    }

    private static boolean isVocabularyNode(@NotNull JsonNode jsonObj) {
        return typeIs(jsonObj, VocabularyType.TerminologicalVocabulary.name(), VocabularyType.Vocabulary.name());
    }

    private @Nullable static String type(@NotNull JsonNode jsonObj) {

        JsonNode type = jsonObj.get("type");

        if (type != null) {
            JsonNode id = type.get("id");

            if (id != null) {
                return id.textValue();
            }
        }

        return null;
    }

    private static boolean typeIs(@NotNull JsonNode jsonObj, @NotNull String... types) {
        String jsonObjType = type(jsonObj);

        if (jsonObjType == null) {
            return false;
        }

        for (String type : types) {
            if (type.equals(jsonObjType)) {
                return true;
            }
        }

        return false;
    }
}
