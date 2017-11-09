package fi.vm.yti.terminology.api.model.termed;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;

public final class GenericTermedNode implements Node {

    private final UUID id;
    private final String code;
    private final String uri;
    private final Long number;

    private final String createdBy;
    private final Date createdDate;
    private final String lastModifiedBy;
    private final Date lastModifiedDate;

    private final TypeId type;

    private final Map<String, List<Attribute>> properties;
    private final Map<String, List<Identifier>> references;
    private final Map<String, List<Identifier>> referrers;

    // Jackson constructor
    private GenericTermedNode() {
        this(randomUUID(), "", "", 0L, "", new Date(), "", new Date(), TypeId.placeholder(), emptyMap(), emptyMap(), emptyMap());
    }

    public GenericTermedNode(UUID id,
                             String code,
                             String uri,
                             Long number,
                             String createdBy,
                             Date createdDate,
                             String lastModifiedBy,
                             Date lastModifiedDate,
                             TypeId type,
                             Map<String, List<Attribute>> properties,
                             Map<String, List<Identifier>> references,
                             Map<String, List<Identifier>> referrers) {
        this.id = id;
        this.code = code;
        this.uri = uri;
        this.number = number;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
        this.type = type;
        this.properties = properties;
        this.references = references;
        this.referrers = referrers;
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

    public Long getNumber() {
        return number;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public TypeId getType() {
        return type;
    }

    public Map<String, List<Attribute>> getProperties() {
        return properties;
    }

    public Map<String, List<Identifier>> getReferences() {
        return references;
    }

    public Map<String, List<Identifier>> getReferrers() {
        return referrers;
    }
}
