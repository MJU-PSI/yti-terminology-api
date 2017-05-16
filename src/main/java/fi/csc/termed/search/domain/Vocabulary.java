package fi.csc.termed.search.domain;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static fi.csc.termed.search.util.JsonUtils.jsonToLocalizable;
import static fi.csc.termed.search.util.JsonUtils.localizableToJson;
import static fi.csc.termed.search.util.JsonUtils.localizableFromTermedProperties;

public final class Vocabulary {

    private final String graphId;
    private final Map<String, List<String>> label;

    private Vocabulary(@NotNull String graphId, @NotNull Map<String, List<String>> label) {
        this.graphId = graphId;
        this.label = label;
    }

    public @NotNull String getGraphId() {
        return graphId;
    }

    public static @NotNull Vocabulary createFromExtJson(@NotNull JsonObject json) {

        JsonObject typeObj = json.get("type").getAsJsonObject();
        JsonObject properties = json.get("properties").getAsJsonObject();

        String graphId = typeObj.get("graph").getAsJsonObject().get("id").getAsString();
        Map<String, List<String>> label = localizableFromTermedProperties(properties, "prefLabel");

        return new Vocabulary(graphId, label);
    }

    static @NotNull Vocabulary createFromIndex(@NotNull JsonObject json) {

        String graphId = json.get("id").getAsString();
        Map<String, List<String>> label = jsonToLocalizable(json.get("label").getAsJsonObject());

        return new Vocabulary(graphId, label);
    }

    @NotNull JsonObject toElasticSearchObject() {
        JsonObject output = new JsonObject();

        output.add("label", localizableToJson(label));
        output.addProperty("id", graphId);

        return output;
    }
}
