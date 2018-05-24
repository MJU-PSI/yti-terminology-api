package fi.vm.yti.terminology.api.model.termed;

import fi.vm.yti.terminology.api.migration.PropertyUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.prefLabel;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class MetaNode {

    private final String id;
    private final String uri;
    private final Long index;
    private final GraphId graph;
    private final Map<String, List<Permission>> permissions;
    private final Map<String, List<Property>> properties;
    private final List<AttributeMeta> textAttributes;
    private final List<ReferenceMeta> referenceAttributes;

    // Jackson constructor
    private MetaNode() {
        this("", "", 0L, GraphId.placeholder(), emptyMap(), emptyMap(), emptyList(), emptyList());
    }

    public MetaNode(String id,
                    String uri,
                    Long index,
                    GraphId graph,
                    Map<String, List<Permission>> permissions,
                    Map<String, List<Property>> properties,
                    List<AttributeMeta> textAttributes,
                    List<ReferenceMeta> referenceAttributes) {
        this.id = id;
        this.uri = uri;
        this.index = index;
        this.graph = graph;
        this.permissions = permissions;
        this.properties = properties;
        this.textAttributes = textAttributes;
        this.referenceAttributes = referenceAttributes;
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

    public GraphId getGraph() {
        return graph;
    }

    public Map<String, List<Permission>> getPermissions() {
        return permissions;
    }

    public Map<String, List<Property>> getProperties() {
        return properties;
    }

    public List<AttributeMeta> getTextAttributes() {
        return textAttributes;
    }

    public AttributeMeta getAttribute(String name) {
        return textAttributes.stream()
                .filter(x -> x.getId().equals(name))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Attribute not found with name: " + name));
    }

    public List<ReferenceMeta> getReferenceAttributes() {
        return referenceAttributes;
    }

    public ReferenceMeta getReference(String name) {
        return referenceAttributes.stream()
                .filter(x -> x.getId().equals(name))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Reference not found with name: " + name));
    }

    public void updateLabel(String fi, String en) {
        updateProperties(prefLabel(fi, en));
    }

    public void updateProperties(Map<String, List<Property>> updatedProperties) {

        Map<String, List<Property>> newProperties = PropertyUtil.merge(this.properties, updatedProperties);

        properties.clear();
        properties.putAll(newProperties);
    }

    public MetaNode copyToGraph(UUID graphId) {

        List<AttributeMeta> newAttributes = mapToList(textAttributes, textAttribute -> textAttribute.copyToGraph(graphId));
        List<ReferenceMeta> newReferences = mapToList(referenceAttributes, referenceAttribute -> referenceAttribute.copyToGraph(graphId));

        return new MetaNode(id, uri, index, new GraphId(graphId), permissions, properties, newAttributes, newReferences);
    }
}
