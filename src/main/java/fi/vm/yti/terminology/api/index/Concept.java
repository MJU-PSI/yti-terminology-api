package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fi.vm.yti.terminology.api.util.JsonUtils.*;
import static java.util.Objects.requireNonNull;
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

    private static @NotNull Concept createFromTermedNodes(@NotNull JsonNode conceptJson,
                                                          @NotNull List<JsonNode> prefLabelXlReferences,
                                                          @NotNull List<JsonNode> altLabelXlReferences,
                                                          @NotNull Vocabulary vocabulary) {

        String id = conceptJson.get("id").textValue();
        String lastModifiedDate = conceptJson.get("lastModifiedDate").textValue();

        JsonNode properties = conceptJson.get("properties");
        JsonNode references = conceptJson.get("references");
        JsonNode referrers = conceptJson.get("referrers");

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

    static @NotNull Concept createFromExtJson(@NotNull JsonNode json, @NotNull Vocabulary vocabulary) {

        JsonNode references = json.get("references");

        List<JsonNode> prefLabelXlReferences = references.has("prefLabelXl")
                ? asStream(references.get("prefLabelXl")).collect(toList())
                : Collections.emptyList();

        List<JsonNode> altLabelXlReferences = references.has("prefLabelXl")
                ? asStream(references.get("prefLabelXl")).collect(toList())
                : Collections.emptyList();

        return createFromTermedNodes(json, prefLabelXlReferences, altLabelXlReferences, vocabulary);
    }

    static @NotNull Concept createFromAllNodeResult(@NotNull String conceptId, @NotNull String vocabularyId, @NotNull AllNodesResult allNodesResult) {

        JsonNode conceptJson = requireNonNull(allNodesResult.getNode(conceptId, "Concept"));
        JsonNode vocabularyJson = requireNonNull(allNodesResult.getNode(vocabularyId));
        Vocabulary vocabulary = Vocabulary.createFromExtJson(vocabularyJson);

        JsonNode references = conceptJson.get("references");

        List<JsonNode> prefLabelXLReferences =
                getReferenceIdsFromTermedReferences(references, "prefLabelXl").stream()
                        .map(refId -> {
                            JsonNode term = allNodesResult.getNode(refId, "Term");

                            if (term == null)
                                throw new BrokenTermedDataLinkException(vocabulary, refId);

                            return term;
                        })
                        .collect(toList());

        List<JsonNode> altLabelXLReferences =
                getReferenceIdsFromTermedReferences(references, "altLabelXl").stream()
                        .map(refId -> {
                            JsonNode term = allNodesResult.getNode(refId, "Term");

                            if (term == null)
                                throw new BrokenTermedDataLinkException(vocabulary, refId);

                            return term;
                        })
                        .collect(toList());

        return createFromTermedNodes(conceptJson, prefLabelXLReferences, altLabelXLReferences, vocabulary);
    }

    static @NotNull Concept createFromIndex(ObjectMapper mapper, @NotNull JsonNode json) {

        String id = json.get("id").textValue();
        List<String> broader = jsonToList(json.get("broader"));
        List<String> narrower = jsonToList(json.get("narrower"));
        Map<String, List<String>> definition = jsonToLocalizable(mapper, json.get("definition"));
        Map<String, List<String>> label = jsonToLocalizable(mapper, json.get("label"));
        Map<String, List<String>> altLabel = jsonToLocalizable(mapper, json.get("altLabel"));
        String lastModifiedDate = json.has("modified")  ? json.get("modified").textValue() : null;
        String status = json.has("status") ? json.get("status").textValue() : null;
        Vocabulary vocabulary = Vocabulary.createFromIndex(mapper, json.get("vocabulary"));

        return new Concept(id, vocabulary, label, altLabel, definition, status, broader, narrower, lastModifiedDate);
    }

    private static @NotNull List<String> getReferenceIdsFromTermedReferences(@NotNull JsonNode references, @NotNull String referenceName) {
        if (references.has(referenceName)) {
            return asStream(references.get(referenceName))
                    .map(node -> node.get("id").textValue())
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

    @NotNull JsonNode toElasticSearchDocument(ObjectMapper mapper) {

        ObjectNode output = mapper.createObjectNode();

        output.put("id", id);
        output.set("broader", listToJson(mapper, broaderIds));
        output.set("narrower", listToJson(mapper, narrowerIds));
        output.set("definition", localizableToJson(mapper, definition));
        output.set("label", localizableToJson(mapper, label));
        output.set("altLabel", localizableToJson(mapper, altLabel));
        output.set("sortByLabel", localizableToJson(mapper, getSingleLabelAsLower()));

        if (lastModifiedDate != null) {
            output.put("modified", lastModifiedDate);
        }

        output.put("hasNarrower",narrowerIds.size() > 0);

        if (status != null) {
            output.put("status", status);
        }

        output.set("vocabulary", vocabulary.toElasticSearchObject(mapper));

        return output;
    }
}