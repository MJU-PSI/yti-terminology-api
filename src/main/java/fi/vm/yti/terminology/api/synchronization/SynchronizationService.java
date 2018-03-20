package fi.vm.yti.terminology.api.synchronization;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static fi.vm.yti.terminology.api.util.CollectionUtils.filterToList;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Service
public class SynchronizationService {

    private final String groupManagementUrl;
    private final String organizationGraphId;
    private final RestTemplate restTemplate;
    private final TermedRequester termedRequester;

    @Autowired
    SynchronizationService(@Value("${groupmanagement.url}") String groupManagementUrl,
                           @Value("${organization.graph}") String organizationGraphId,
                           RestTemplate restTemplate,
                           TermedRequester termedRequester) {
        this.groupManagementUrl = groupManagementUrl;
        this.organizationGraphId = organizationGraphId;
        this.restTemplate = restTemplate;
        this.termedRequester = termedRequester;
    }

    public void synchronize() {

        List<GroupManagementOrganization> organizations = getGroupManagementOrganizations();

        Set<UUID> removedOrganizations = organizations.stream()
                .filter(GroupManagementOrganization::isRemoved)
                .map(GroupManagementOrganization::getUuid)
                .collect(Collectors.toSet());

        List<Identifier> delete =
                filterToList(getUnusedOrganizationIds(), o -> removedOrganizations.contains(o.getId()));

        List<OrganizationNode> save = organizations.stream()
                .filter(o -> !o.isRemoved())
                .map(o -> OrganizationNode.fromGroupManagement(o, organizationGraphId))
                .collect(Collectors.toList());

        Parameters params = new Parameters();
        params.add("changeset", "true");

        termedRequester.exchange("/nodes", POST, params, String.class, new DeleteAndSaveOrganizations(delete, save));
    }

    private @NotNull List<Identifier> getUnusedOrganizationIds() {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "type.id:" + NodeType.Organization);
        params.add("max", "-1");
        params.add("select", "referrers.*");

        List<OrganizationReferrersNode> organizations =
                termedRequester.exchange("/node-trees", GET, params, new ParameterizedTypeReference<List<OrganizationReferrersNode>>() {});

        return requireNonNull(organizations).stream()
                .filter(o -> !o.hasReferrers())
                .map(OrganizationReferrersNode::getIdentifier)
                .collect(Collectors.toList());
    }

    private @NotNull List<GroupManagementOrganization> getGroupManagementOrganizations() {
        return restTemplate.exchange(groupManagementUrl + "/public-api/organizations",
                GET, null, new ParameterizedTypeReference<List<GroupManagementOrganization>>() {}).getBody();
    }
}
