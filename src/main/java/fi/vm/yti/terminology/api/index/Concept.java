package fi.vm.yti.terminology.api.index;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.vm.yti.terminology.api.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fi.vm.yti.terminology.api.util.JsonUtils.*;
import static java.util.stream.Collectors.toList;

final class Concept {

    private final String id;
    private final Vocabulary vocabulary;
    private final Map<String, List<String>> label;
    private final Map<String, List<String>> altLabel;
    private final Map<String, List<String>> definition;
    @Nullable
    private final String status;
    private final List<String> broaderIds;
    private final List<String> narrowerIds;
    @Nullable
    private final String lastModifiedDate;

    private Concept(String id,
                    Vocabulary vocabulary,
                    Map<String, List<String>> label,
                    Map<String, List<String>> altLabel,
                    Map<String, List<String>> definition,
                    @Nullable String status,
                    List<String> broaderIds,
                    List<String> narrowerIds,
                    @Nullable String lastModifiedDate) {

        this.id = id;
        this.vocabulary = vocabulary;
        this.label = label;
        this.altLabel = altLabel;
        this.definition = definition;
        this.status = status;
        this.broaderIds = broaderIds;
        this.narrowerIds = narrowerIds;
        this.lastModifiedDate = lastModifiedDate;
    }

    private static @NotNull Concept createFromTermedNodes(@NotNull JsonObject conceptJson,
                                                          @NotNull List<JsonObject> prefLabelXlReferences,
                                                          @NotNull List<JsonObject> altLabelXlReferences,
                                                          @NotNull Vocabulary vocabulary) {

        String id = conceptJson.get("id").getAsString();
        String lastModifiedDate = conceptJson.get("lastModifiedDate").getAsString();

        JsonObject properties = conceptJson.getAsJsonObject("properties");
        JsonObject references = conceptJson.getAsJsonObject("references");
        JsonObject referrers = conceptJson.getAsJsonObject("referrers");

        Map<String, List<String>> label =
                properties.has("prefLabel")
                        ? localizableFromTermedProperties(properties, "prefLabel")
                        : prefLabelXlReferences.size() > 0
                        ? localizableFromTermReferences(prefLabelXlReferences, "prefLabel")
                        : Collections.emptyMap();

        Map<String, List<String>> altLabel =
                properties.has("altLabel")
                        ? localizableFromTermedProperties(properties, "altLabel")
                        : altLabelXlReferences.size() > 0
                        ? localizableFromTermReferences(altLabelXlReferences, "altLabel")
                        : Collections.emptyMap();

        Map<String, List<String>> definition = localizableFromTermedProperties(properties, "definition");

        String status = getSinglePropertyValue(properties, "status");

        List<String> broaderIds = getReferenceIdsFromTermedReferences(references, "broader");
        List<String> narrowerIds = getReferenceIdsFromTermedReferences(referrers, "broader");

        return new Concept(id, vocabulary, label, altLabel, definition, status, broaderIds, narrowerIds, lastModifiedDate);
    }

    static @NotNull Concept createFromExtJson(@NotNull JsonObject json, @NotNull Vocabulary vocabulary) {

        JsonObject references = json.get("references").getAsJsonObject();

        List<JsonObject> prefLabelXlReferences = references.has("prefLabelXl")
                ? asStream(references.getAsJsonArray("prefLabelXl")).map(JsonElement::getAsJsonObject).collect(toList())
                : Collections.emptyList();

        List<JsonObject> altLabelXlReferences = references.has("prefLabelXl")
                ? asStream(references.getAsJsonArray("prefLabelXl")).map(JsonElement::getAsJsonObject).collect(toList())
                : Collections.emptyList();

        return createFromTermedNodes(json, prefLabelXlReferences, altLabelXlReferences, vocabulary);
    }

    static @NotNull Concept createFromAllNodeResult(@NotNull String conceptId, @NotNull String vocabularyId, @NotNull AllNodesResult allNodesResult) {

        JsonObject conceptJson = ObjectUtils.requireNonNull(allNodesResult.getNode(conceptId, "Concept"));
        JsonObject vocabularyJson = ObjectUtils.requireNonNull(allNodesResult.getNode(vocabularyId));

        JsonObject references = conceptJson.getAsJsonObject("references");

        List<JsonObject> prefLabelXLReferences =
                getReferenceIdsFromTermedReferences(references, "prefLabelXl").stream()
                        .map(refId -> allNodesResult.getNode(refId, "Term"))
                        .collect(toList());

        List<JsonObject> altLabelXLReferences =
                getReferenceIdsFromTermedReferences(references, "altLabelXl").stream()
                        .map(refId -> allNodesResult.getNode(refId, "Term"))
                        .collect(toList());

        return createFromTermedNodes(conceptJson, prefLabelXLReferences, altLabelXLReferences, Vocabulary.createFromExtJson(vocabularyJson));
    }

    static @NotNull Concept createFromIndex(@NotNull JsonObject json) {

        String id = json.get("id").getAsString();
        List<String> broader = jsonToList(json.getAsJsonArray("broader"));
        List<String> narrower = jsonToList(json.getAsJsonArray("narrower"));
        Map<String, List<String>> definition = jsonToLocalizable(json.getAsJsonObject("definition"));
        Map<String, List<String>> label = jsonToLocalizable(json.getAsJsonObject("label"));
        Map<String, List<String>> altLabel = jsonToLocalizable(json.getAsJsonObject("altLabel"));
        String lastModifiedDate = json.has("modified")  ? json.get("modified").getAsString() : null;
        String status = json.has("status") ? json.get("status").getAsString() : null;
        Vocabulary vocabulary = Vocabulary.createFromIndex(json.getAsJsonObject("vocabulary"));

        return new Concept(id, vocabulary, label, altLabel, definition, status, broader, narrower, lastModifiedDate);
    }

    private static @NotNull List<String> getReferenceIdsFromTermedReferences(@NotNull JsonObject references, @NotNull String referenceName) {
        if (references.has(referenceName)) {
            return asStream(references.getAsJsonArray(referenceName))
                    .map(JsonElement::getAsJsonObject)
                    .map(node -> node.get("id").getAsString())
                    .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull String getDocumentId() {
        return formDocumentId(vocabulary.getGraphId(), id);
    }

    static @NotNull String formDocumentId(@NotNull String graphId, @NotNull String conceptId) {
        return graphId + "/" + conceptId;
    }

    @NotNull List<String> getBroaderIds() {
        return broaderIds;
    }

    @NotNull List<String> getNarrowerIds() {
        return narrowerIds;
    }

    private @NotNull Map<String, List<String>> getSingleLabelAsLower() {

        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : label.entrySet()) {

            String language = entry.getKey();
            List<String> singleLocalizationAsLower = entry.getValue().stream().limit(1).map(String::toLowerCase).collect(toList());

            result.put(language, singleLocalizationAsLower);
        }

        return result;
    }

    @NotNull JsonObject toElasticSearchDocument() {

        JsonObject output = new JsonObject();

        output.addProperty("id", id);
        output.add("broader", listToJson(broaderIds));
        output.add("narrower", listToJson(narrowerIds));
        output.add("definition", localizableToJson(definition));
        output.add("label", localizableToJson(label));
        output.add("altLabel", localizableToJson(altLabel));
        output.add("sortByLabel", localizableToJson(getSingleLabelAsLower()));

        if (lastModifiedDate != null) {
            output.addProperty("modified", lastModifiedDate);
        }

        output.addProperty("hasNarrower",narrowerIds.size() > 0);

        if (status != null) {
            output.addProperty("status", status);
        }

        output.add("vocabulary", vocabulary.toElasticSearchObject());

        return output;
    }
}
