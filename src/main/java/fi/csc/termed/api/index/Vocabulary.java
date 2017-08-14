package fi.csc.termed.api.index;

import com.google.gson.JsonObject;
import fi.csc.termed.api.util.JsonUtils;
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

    static @NotNull Vocabulary createFromExtJson(@NotNull JsonObject json) {

        JsonObject typeObj = json.get("type").getAsJsonObject();
        JsonObject properties = json.get("properties").getAsJsonObject();

        String graphId = typeObj.get("graph").getAsJsonObject().get("id").getAsString();
        Map<String, List<String>> label = JsonUtils.localizableFromTermedProperties(properties, "prefLabel");

        return new Vocabulary(graphId, label);
    }

    static @NotNull Vocabulary createFromIndex(@NotNull JsonObject json) {

        String graphId = json.get("id").getAsString();
        Map<String, List<String>> label = JsonUtils.jsonToLocalizable(json.get("label").getAsJsonObject());

        return new Vocabulary(graphId, label);
    }

    @NotNull JsonObject toElasticSearchObject() {
        JsonObject output = new JsonObject();

        output.add("label", JsonUtils.localizableToJson(label));
        output.addProperty("id", graphId);

        return output;
    }
}
