package fi.vm.yti.terminology.api.security;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static fi.vm.yti.security.Role.ADMIN;
import static fi.vm.yti.security.Role.TERMINOLOGY_EDITOR;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;

@Service
public class AuthorizationManager {

    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationTermedService termedService;

    @Autowired
    AuthorizationManager(AuthenticatedUserProvider userProvider,
                         AuthorizationTermedService termedService) {
        this.userProvider = userProvider;
        this.termedService = termedService;
    }

    public boolean canModifyNodes(List<? extends Node> nodes) {
        return canModifyAllGraphs(mapToList(nodes, node -> node.getType().getGraphId()));
    }

    public boolean canRemoveNodes(List<Identifier> identifiers) {
        return canModifyAllGraphs(mapToList(identifiers, id -> id.getType().getGraphId()));
    }

    public boolean canModifyMetaNodes(List<MetaNode> metaNodes) {
        return isLoggedIn(); // TODO
    }

    public boolean canRemoveMetaNodes(List<MetaNode> metaNodes) {
        return isLoggedIn(); // TODO
    }

    public boolean canCreateGraph(Graph graph) {
        return isLoggedIn(); // TODO
    }

    public boolean canDeleteGraph(UUID graphId) {
        return isLoggedIn(); // TODO
    }

    private boolean canModifyAllGraphs(Collection<UUID> graphIds) {

        YtiUser user = userProvider.getUser();

        if (user.isSuperuser()) {
            return true;
        }

        for (UUID graphId : graphIds) {

            Set<UUID> organizationIds = termedService.getOrganizationIds(graphId);

            if (!user.isInAnyRole(EnumSet.of(ADMIN, TERMINOLOGY_EDITOR), organizationIds)) {
                return false;
            }
        }

        return true;
    }

    private boolean isLoggedIn() {
        return !userProvider.getUser().isAnonymous();
    }
}
