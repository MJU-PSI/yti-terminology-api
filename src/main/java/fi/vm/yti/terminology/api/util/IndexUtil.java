package fi.vm.yti.terminology.api.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class IndexUtil {

    static final List<String> SORT_LABEL_LANGUAGES = List.of("fi", "sv", "en");

    public static Map<String, List<String>> createSortLabels(Map<String, List<String>> label) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : label.entrySet()) {

            String language = entry.getKey();
            List<String> singleLocalizationAsLower = entry.getValue().stream().limit(1).map(String::toLowerCase).collect(toList());

            result.put(language, singleLocalizationAsLower);
        }

        if (result.isEmpty()) {
            return result;
        }

        // Add default sort label for missing languages
        String defaultKey = result.keySet().iterator().next();
        List<String> defaultValue = result
                .getOrDefault("fi", result.get(defaultKey))
                .stream()
                .map(String::toLowerCase)
                .collect(toList());

        SORT_LABEL_LANGUAGES.forEach(lang -> {
            if (!result.containsKey(lang)) {
                result.put(lang, defaultValue);
            }
        });

        return result;
    }
}
