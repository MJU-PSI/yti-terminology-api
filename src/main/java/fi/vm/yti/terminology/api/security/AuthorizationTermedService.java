package fi.vm.yti.terminology.api.security;

import java.util.Set;
import java.util.UUID;

public interface AuthorizationTermedService {
    Set<UUID> getOrganizationIds(UUID graphId);
}
