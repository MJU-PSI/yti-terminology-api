package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.vm.yti.terminology.api.synchronization.GroupManagementOrganization;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fi.vm.yti.terminology.api.model.termed.NodeType.Organization;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(value = { "references", "referrers", "number", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate" })
public final class OrganizationNode implements Node {

    private static final String DEFAULT_ATTRIBUTE_REGEX = "(?s)^.*$";

    private final UUID id;
    private final TypeId type;
    private final Map<String, List<Attribute>> properties;

    // Jackson constructor
    private OrganizationNode() {
        this(randomUUID(), TypeId.placeholder(), emptyMap());
    }

    public OrganizationNode(UUID id, TypeId type, Map<String, String> prefLabel) {
        this.id = id;
        this.type = type;
        this.properties = singletonMap("prefLabel", localizableToLocalizations(prefLabel));
    }

    public static OrganizationNode fromGroupManagement(GroupManagementOrganization org, String organizationGraphId) {
        TypeId type = new TypeId(Organization, new GraphId(UUID.fromString(organizationGraphId)));
        return new OrganizationNode(org.getUuid(), type, org.getPrefLabel());
    }

    private static List<Attribute> localizableToLocalizations(Map<String, String> localizable) {
        return mapToList(localizable.entrySet(), entry -> {
            String lang = entry.getKey();
            String value = entry.getValue();
            return new Attribute(lang, value, DEFAULT_ATTRIBUTE_REGEX);
        });
    }

    public UUID getId() {
        return id;
    }

    @Override
    public TypeId getType() {
        return type;
    }

    @Override
    public Map<String, List<Attribute>> getProperties() {
        return properties;
    }

    @Override
    public Map<String, List<Identifier>> getReferences() {
        return emptyMap();
    }
}
