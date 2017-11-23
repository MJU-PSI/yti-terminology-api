package fi.vm.yti.terminology.api.frontend;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Service
public class FrontendGroupManagementService {

    private final String groupManagementUrl;
    private final RestTemplate restTemplate;
    private final AuthenticatedUserProvider userProvider;

    public FrontendGroupManagementService(@Value("${groupmanagement.url}") String groupManagementUrl,
                                          RestTemplate restTemplate,
                                          AuthenticatedUserProvider userProvider) {
        this.groupManagementUrl = groupManagementUrl;
        this.restTemplate = restTemplate;
        this.userProvider = userProvider;
    }

    @NotNull List<GroupManagementUserRequest> getUserRequests() {

        YtiUser user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        String url = groupManagementUrl + "/public-api/requests" + Parameters.single("email", user.getEmail());
        return restTemplate.exchange(url, GET, null, new ParameterizedTypeReference<List<GroupManagementUserRequest>>() {}).getBody();
    }

    void sendRequest(UUID organizationId) {

        YtiUser user = userProvider.getUser();

        if (user.isAnonymous()) {
            throw new RuntimeException("User not authenticated");
        }

        Parameters parameters = new Parameters();
        parameters.add("email", user.getEmail());
        parameters.add("role", Role.TERMINOLOGY_EDITOR.toString());
        parameters.add("organizationId", organizationId.toString());

        String url = groupManagementUrl + "/public-api/request" + parameters;
        restTemplate.exchange(url, POST, null, String.class);
    }
}
