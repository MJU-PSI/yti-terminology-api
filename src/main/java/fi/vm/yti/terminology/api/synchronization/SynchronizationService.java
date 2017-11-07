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
import java.util.Map;
import java.util.UUID;

import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Service
public class SynchronizationService {

    private static final String DEFAULT_ATTRIBUTE_REGEX = "(?s)^.*$";

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

        List<TermedOrganization> termedOrganizations = mapToList(getGroupManagementOrganizations(), org -> {

            TermedTypeId type = new TermedTypeId("Organization", new TermedGraphId(UUID.fromString(organizationGraphId)));
            Map<String, List<TermedLangValue>> properties = singletonMap("prefLabel", localizableToLocalizations(org.getPrefLabel()));

            return new TermedOrganization(org.getUuid(), "", type, properties);
        });

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        termedRequester.exchange("/nodes", POST, params, String.class, new DeleteAndSave(emptyList(), termedOrganizations));
    }

    private static List<TermedLangValue> localizableToLocalizations(Map<String, String> localizable) {
        return mapToList(localizable.entrySet(), entry -> {
            String lang = entry.getKey();
            String value = entry.getValue();
            return new TermedLangValue(lang, value, DEFAULT_ATTRIBUTE_REGEX);
        });
    }

    private @NotNull List<GroupManagementOrganization> getGroupManagementOrganizations() {
        return restTemplate.exchange(groupManagementUrl + "/public-api/organizations",
                GET, null, new ParameterizedTypeReference<List<GroupManagementOrganization>>() {}).getBody();
    }
}
