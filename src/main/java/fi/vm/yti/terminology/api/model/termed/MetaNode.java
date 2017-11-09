package fi.vm.yti.terminology.api.model.termed;

import java.util.List;
import java.util.Map;

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

    public List<ReferenceMeta> getReferenceAttributes() {
        return referenceAttributes;
    }
}
