package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.model.termed.Property;

import java.util.List;
import java.util.Map;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.literal;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.localizable;

public class PropertyIndex {

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

    // prevent construction
    private PropertyIndex() {
    }
}
