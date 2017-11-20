package fi.vm.yti.terminology.api.security;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.model.termed.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthorizationManager {

    private final AuthenticatedUserProvider userProvider;

    @Autowired
    AuthorizationManager(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    public boolean canModifyNodes(List<? extends Node> nodes) {
        return isLoggedIn(); // TODO
    }

    public boolean canRemoveNodes(List<Identifier> identifiers) {
        return isLoggedIn(); // TODO
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

    private boolean isLoggedIn() {
        return !userProvider.getUser().isAnonymous();
    }
}
