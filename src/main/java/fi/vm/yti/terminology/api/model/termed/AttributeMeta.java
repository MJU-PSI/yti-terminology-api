package fi.vm.yti.terminology.api.model.termed;

import fi.vm.yti.terminology.api.migration.PropertyUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.prefLabel;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.type;
import static java.util.Collections.emptyMap;

public final class AttributeMeta {

    private final String regex;
    private final String id;
    private final String uri;
    private Long index;
    private final TypeId domain;
    private final Map<String, List<Permission>> permissions;
    private final Map<String, List<Property>> properties;

    private static final String DEFAULT_ATTRIBUTE_REGEX = "(?s)^.*$";

    // Jackson constructor
    private AttributeMeta() {
        this("", "", "", 0L, TypeId.placeholder(), emptyMap(), emptyMap());
    }

    public AttributeMeta(String id,
                         String uri,
                         Long index,
                         TypeId domain,
                         Map<String, List<Permission>> permissions,
                         Map<String, List<Property>> properties) {
        this(DEFAULT_ATTRIBUTE_REGEX, id, uri, index, domain, permissions, properties);
    }

    public AttributeMeta(String regex,
                         String id,
                         String uri,
                         Long index,
                         TypeId domain,
                         Map<String, List<Permission>> permissions,
                         Map<String, List<Property>> properties) {
        this.regex = regex;
        this.id = id;
        this.uri = uri;
        this.index = index;
        this.domain = domain;
        this.permissions = permissions;
        this.properties = properties;
    }


    public String getRegex() {
        return regex;
    }

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public Long getIndex() {
        return index;
    }

    void setIndex(Long newIndex) {
        index = newIndex;
    }

    void incrementIndex() {
        index++;
    }

    void decrementIndex() {
        index--;
    }

    public TypeId getDomain() {
        return domain;
    }

    public Map<String, List<Permission>> getPermissions() {
        return permissions;
    }

    public Map<String, List<Property>> getProperties() {
        return properties;
    }

    public void updateLabel(String fi, String en) {
        updateProperties(prefLabel(fi, en));
    }

    public void updateType(String type) {
        updateProperties(type(type));
    }

    public void updateProperties(Map<String, List<Property>> updatedProperties) {

        Map<String, List<Property>> newProperties = PropertyUtil.merge(this.properties, updatedProperties);

        properties.clear();
        properties.putAll(newProperties);
    }

    public AttributeMeta copyToGraph(UUID graphId) {

        TypeId newDomain = domain.copyToGraph(graphId);

        return new AttributeMeta(regex, id, uri, index, newDomain, permissions, properties);
    }
}
