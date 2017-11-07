package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.vm.yti.terminology.api.util.JsonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

final class Vocabulary {

    private final String graphId;
    private final Map<String, List<String>> label;

    private Vocabulary(@NotNull String graphId, @NotNull Map<String, List<String>> label) {
        this.graphId = graphId;
        this.label = label;
    }

    @NotNull String getGraphId() {
        return graphId;
    }

    static @NotNull Vocabulary createFromExtJson(@NotNull JsonNode json) {

        JsonNode typeObj = json.get("type");
        JsonNode properties = json.get("properties");

        String graphId = typeObj.get("graph").get("id").textValue();
        Map<String, List<String>> label = JsonUtils.localizableFromTermedProperties(properties, "prefLabel");

        return new Vocabulary(graphId, label);
    }

    static @NotNull Vocabulary createFromIndex(ObjectMapper mapper, @NotNull JsonNode json) {

        String graphId = json.get("id").textValue();
        Map<String, List<String>> label = JsonUtils.jsonToLocalizable(mapper, json.get("label"));

        return new Vocabulary(graphId, label);
    }

    @NotNull ObjectNode toElasticSearchObject(ObjectMapper objectMapper) {

        ObjectNode output = objectMapper.createObjectNode();

        output.set("label", JsonUtils.localizableToJson(objectMapper, label));
        output.put("id", graphId);

        return output;
    }
}