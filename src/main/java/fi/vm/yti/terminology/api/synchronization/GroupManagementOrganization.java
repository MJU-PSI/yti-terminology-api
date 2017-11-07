package fi.vm.yti.terminology.api.synchronization;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;

public final class GroupManagementOrganization {

    private final UUID uuid;
    private final Map<String, String> prefLabel;
    private final Map<String, String> description;
    private final String url;

    // Jackson constructor
    private GroupManagementOrganization() {
        this(UUID.randomUUID(), emptyMap(), emptyMap(), "");
    }

    GroupManagementOrganization(UUID uuid, Map<String, String> prefLabel, Map<String, String> description, String url) {
        this.uuid = uuid;
        this.prefLabel = prefLabel;
        this.description = description;
        this.url = url;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }
}
