package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.model.termed.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public final class PropertyUtil {

    public static Map<String, List<Property>> localizable(String propertyName, String fi) {
        return singletonMap(propertyName, singletonList(
                new Property("fi", fi)
        ));
    }

    public static Map<String, List<Property>> localizable(String propertyName, String fi, String en) {
        return singletonMap(propertyName, asList(
                new Property("fi", fi),
                new Property("en", en)
        ));
    }

    public static Map<String, List<Property>> localizable(String propertyName, String fi, String en, String sv) {
        return singletonMap(propertyName, asList(
                new Property("fi", fi),
                new Property("en", en),
                new Property("sv", sv)
        ));
    }

    public static Map<String, List<Property>> literal(String propertyName, String value) {
        return singletonMap(propertyName, singletonList(
                new Property("", value)
        ));
    }

    @SafeVarargs
    public static Map<String, List<Property>> merge(Map<String, List<Property>>... properties) {

        Map<String, List<Property>> result = new HashMap<>();

        for (Map<String, List<Property>> property : properties) {
            for (String propertyName : property.keySet()) {
                result.put(propertyName, property.get(propertyName));
            }
        }

        return result;
    }

    // prevent construction
    private PropertyUtil() {
    }
}
