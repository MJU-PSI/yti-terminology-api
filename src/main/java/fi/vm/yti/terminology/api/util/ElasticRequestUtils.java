package fi.vm.yti.terminology.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.elasticsearch.client.Response;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ElasticRequestUtils {

    public static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("[a-z]+(?:-[a-zA-Z0-9-]+)?");
    public static final Pattern QUERY_SPLITTER_PATTERN = Pattern.compile("\\s+");

    private ElasticRequestUtils() {
        // prevent construction
    }

    public static @NotNull JsonNode responseContentAsJson(@NotNull ObjectMapper objectMapper,
                                                          @NotNull Response response) {
        try {
            return objectMapper.readTree(response.getEntity().getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull String responseContentAsString(@NotNull Response response) {
        try (InputStream is = response.getEntity().getContent()) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> labelFromKeyValueNode(JsonNode labelNode) {
        Map<String, String> ret = new HashMap<>();
        if (labelNode != null) {
            Iterator<Map.Entry<String, JsonNode>> labelIter = labelNode.fields();
            while (labelIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = labelIter.next();
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    ret.put(entry.getKey(), value.textValue());
                } else if (value.isArray() && value.has(0)) {
                    ret.put(entry.getKey(), value.get(0).textValue());
                }
            }
        }
        return !ret.isEmpty() ? ret : null;
    }

    public static Map<String, String> labelFromLangValueArray(JsonNode labelArray) {
        Map<String, String> ret = new HashMap<>();
        if (labelArray != null) {
            for (JsonNode label : labelArray) {
                ret.put(label.get("lang").textValue(), label.get("value").textValue());
            }
        }
        return !ret.isEmpty() ? ret : null;
    }

    public static String getTextValueOrNull(JsonNode node,
                                            String fieldName) {
        if (node != null) {
            JsonNode field = node.get(fieldName);
            if (field != null) {
                return field.textValue();
            }
        }
        return null;
    }

    public static void highlightLabel(Map<String, String> label, String query) {
        if (query != null && label != null && !query.isEmpty() && !label.isEmpty()) {
            String[] queryWords = QUERY_SPLITTER_PATTERN.split(query);
            for(String word : queryWords) {
                if (!word.isEmpty()) {
                    String quoted = Pattern.quote(word);
                    Pattern pattern = Pattern.compile(quoted + "\\b|\\b" + quoted, Pattern.CASE_INSENSITIVE);
                    for (Map.Entry<String, String> entry : label.entrySet()) {
                        entry.setValue(pattern.matcher(entry.getValue()).replaceAll("<b>$0</b>"));
                    }
                }
            }
        }
    }
}
