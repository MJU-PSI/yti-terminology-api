package fi.vm.yti.terminology.api.synchronization;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.OrganizationNode;
import fi.vm.yti.terminology.api.model.termed.UpdateOrganizations;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
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

        List<OrganizationNode> organizations =
                mapToList(getGroupManagementOrganizations(), o -> OrganizationNode.fromGroupManagement(o, organizationGraphId));

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        termedRequester.exchange("/nodes", POST, params, String.class, new UpdateOrganizations(organizations));
    }

    private @NotNull List<GroupManagementOrganization> getGroupManagementOrganizations() {
        return restTemplate.exchange(groupManagementUrl + "/public-api/organizations",
                GET, null, new ParameterizedTypeReference<List<GroupManagementOrganization>>() {}).getBody();
    }
}
