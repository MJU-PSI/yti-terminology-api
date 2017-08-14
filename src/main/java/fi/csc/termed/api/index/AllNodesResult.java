package fi.csc.termed.api.index;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

final class AllNodesResult {

    private final Map<String, JsonObject> nodes;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    AllNodesResult(@NotNull List<JsonObject> nodes) {
        this.nodes = nodes.stream().collect(Collectors.toMap(node -> node.get("id").getAsString(), identity()));
    }

    @Nullable JsonObject getNode(@NotNull String id) {
        return getNode(id, null);
    }

    @Nullable JsonObject getNode(@NotNull String id, @Nullable String expectedType) {

        JsonObject node = this.nodes.get(id);

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
                .map(node -> node.get("id").getAsString())
                .collect(Collectors.toList());
    }

    @NotNull Optional<String> getVocabularyNodeId() {
        return this.nodes.values().stream()
                .filter(AllNodesResult::isVocabularyNode)
                .map(node -> node.get("id").getAsString())
                .findFirst();
    }

    private static boolean isConceptNode(@NotNull JsonObject jsonObj) {
        return typeIs(jsonObj, "Concept");
    }

    private static boolean isVocabularyNode(@NotNull JsonObject jsonObj) {
        return typeIs(jsonObj, VocabularyType.TerminologicalVocabulary.name(), VocabularyType.Vocabulary.name());
    }

    private @Nullable static String type(@NotNull JsonObject jsonObj) {

        JsonElement type = jsonObj.get("type");

        if (type != null) {
            JsonElement id = type.getAsJsonObject().get("id");

            if (id != null) {
                return id.getAsString();
            }
        }

        return null;
    }

    private static boolean typeIs(@NotNull JsonObject jsonObj, @NotNull String... types) {
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
