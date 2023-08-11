package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.model.termed.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fi.vm.yti.terminology.api.util.CollectionUtils.filterToList;
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

    public static Map<String, List<Property>> localizable(String propertyName, Property property) {
        return singletonMap(propertyName, localizations(property));
    }

    public static Map<String, List<Property>> localizable(String propertyName, List<Property> properties) {
        return singletonMap(propertyName, properties);
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

    public static List<Property> localizations(Property property) {
        return singletonList(property);
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

    public static Map<String, List<Property>> prefLabel(Property property) {
        return localizable("prefLabel", property);
    }

    public static Map<String, List<Property>> prefLabel(List<Property> properties) {
        return localizable("prefLabel", properties);
    }

    public static Map<String, List<Property>> description(String fi) {
        return localizable("description", fi);
    }

    public static Map<String, List<Property>> description(String fi, String en) {
        return localizable("description", fi, en);
    }

    public static Map<String, List<Property>> description(String fi, String en, String sv) {
        return localizable("description", fi, en, sv);
    }

    public static Map<String, List<Property>> description(Property property) {
        return localizable("description", property);
    }

    public static Map<String, List<Property>> description(List<Property> properties) {
        return localizable("description", properties);
    }

    public static Map<String, List<Property>> type(String value) {
        return literal("type", value);
    }

    @SafeVarargs
    public static Map<String, List<Property>> merge(Map<String, List<Property>>... properties) {

        Map<String, List<Property>> result = new HashMap<>();

        for (Map<String, List<Property>> property : properties) {
            result = merge(result, property);
        }

        return result;
    }

    public static Map<String, List<Property>> merge(Map<String, List<Property>> properties, Map<String, List<Property>> updatedProperties) {

        HashMap<String, List<Property>> result = new HashMap<>(properties);

        for (Map.Entry<String, List<Property>> updatedProperty : updatedProperties.entrySet()) {

            String name = updatedProperty.getKey();
            List<Property> value = updatedProperty.getValue();
            result.merge(name, value, PropertyUtil::merge);
        }

        return result;
    }

    public static List<Property> merge(List<Property> properties, List<Property> updatedProperties) {

        List<Property> unchangedProperties = filterToList(properties, property ->
                updatedProperties.stream().noneMatch(updatedProperty -> property.getLang().equals(updatedProperty.getLang())));

        List<Property> result = new ArrayList<>(properties.size() + updatedProperties.size());
        result.addAll(unchangedProperties);
        result.addAll(updatedProperties);

        return result;
    }

    // prevent construction
    private PropertyUtil() {
    }
}
