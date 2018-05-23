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
        return singletonMap(propertyName, localizations(fi));
    }

    public static Map<String, List<Property>> localizable(String propertyName, String fi, String en) {
        return singletonMap(propertyName, localizations(fi, en));
    }

    public static Map<String, List<Property>> localizable(String propertyName, String fi, String en, String sv) {
        return singletonMap(propertyName, localizations(fi, en, sv));
    }

    public static List<Property> localizations(String fi) {
        return singletonList(new Property("fi", fi));
    }

    public static List<Property> localizations(String fi, String en) {
        return asList(new Property("fi", fi), new Property("en", en));
    }

    public static List<Property> localizations(String fi, String en, String sv) {
        return asList(new Property("fi", fi), new Property("en", en), new Property("sv", sv));
    }

    public static Map<String, List<Property>> literal(String propertyName, String value) {
        return singletonMap(propertyName, literal(value));
    }

    public static List<Property> literal(String value) {
        return singletonList(new Property("", value));
    }

    public static Map<String, List<Property>> prefLabel(String fi) {
        return localizable("prefLabel", fi);
    }

    public static Map<String, List<Property>> prefLabel(String fi, String en) {
        return localizable("prefLabel", fi, en);
    }

    public static Map<String, List<Property>> prefLabel(String fi, String en, String sv) {
        return localizable("prefLabel", fi, en, sv);
    }

    public static Map<String, List<Property>> type(String value) {
        return literal("type", value);
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
