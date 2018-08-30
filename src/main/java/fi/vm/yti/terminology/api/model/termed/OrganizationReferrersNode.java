package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(value = { "properties", "references", "number", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate" })
public final class OrganizationReferrersNode {

    private final UUID id;
    private final TypeId type;
    private final Map<String, List<GenericNode>> referrers;

    // Jackson constructor
    private OrganizationReferrersNode() {
        this(randomUUID(), TypeId.placeholder(), emptyMap());
    }

    public OrganizationReferrersNode(UUID id, TypeId type, Map<String, List<GenericNode>> referrers) {
        this.id = id;
        this.type = type;
        this.referrers = referrers;
    }

    public UUID getId() {
        return id;
    }

    public Identifier getIdentifier() {
        return new Identifier(this.id, this.type);
    }

    public boolean hasReferrers() {
        return !this.referrers.isEmpty();
    }

    public TypeId getType() {
        return type;
    }

    public Map<String, List<GenericNode>> getReferrers() {
        return referrers;
    }
}
