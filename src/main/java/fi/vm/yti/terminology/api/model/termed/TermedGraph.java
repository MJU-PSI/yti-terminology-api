package fi.vm.yti.terminology.api.model.termed;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.*;

public class TermedGraph {

    private final UUID id;
    private final String code;
    private final String uri;

    private final List<String> roles;
    private final Map<String, List<TermedPermission>> permissions;
    private final Map<String, List<TermedLangValue>> properties;

    // Jackson constructor
    private TermedGraph() {
        this(UUID.randomUUID(), "", "", emptyList(), emptyMap(), emptyMap());
    }

    public TermedGraph(UUID id, String code, String uri, List<String> roles, Map<String, List<TermedPermission>> permissions, Map<String, List<TermedLangValue>> properties) {
        this.id = id;
        this.code = code;
        this.uri = uri;
        this.roles = unmodifiableList(roles);
        this.permissions = unmodifiableMap(permissions);
        this.properties = unmodifiableMap(properties);
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getUri() {
        return uri;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Map<String, List<TermedPermission>> getPermissions() {
        return permissions;
    }

    public Map<String, List<TermedLangValue>> getProperties() {
        return properties;
    }
}
