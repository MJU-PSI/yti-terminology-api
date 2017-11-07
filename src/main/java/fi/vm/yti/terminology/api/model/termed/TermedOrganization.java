package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(value = { "references", "referrers", "number", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate" })
public final class TermedOrganization {

    private final UUID id;
    private final String code;
    private final TermedTypeId type;
    private final Map<String, List<TermedLangValue>> properties;

    // Jackson constructor
    private TermedOrganization() {
        this(randomUUID(), "", new TermedTypeId("", new TermedGraphId(randomUUID())), emptyMap());
    }

    public TermedOrganization(UUID id, String code, TermedTypeId type, Map<String, List<TermedLangValue>> properties) {
        this.id = id;
        this.code = code;
        this.type = type;
        this.properties = properties;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public TermedTypeId getType() {
        return type;
    }

    public Map<String, List<TermedLangValue>> getProperties() {
        return properties;
    }
}
