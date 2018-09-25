package fi.vm.yti.terminology.api.security;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.*;

import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToSet;
import static fi.vm.yti.terminology.api.util.CollectionUtils.requireSingle;
import static java.util.Collections.emptyList;
import static org.springframework.context.annotation.ScopedProxyMode.INTERFACES;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.web.util.TagUtils.SCOPE_REQUEST;

@Service
@Scope(value = SCOPE_REQUEST, proxyMode = INTERFACES)
public class DefaultAuthorizationTermedService implements AuthorizationTermedService {

    private final Map<UUID, Set<UUID>> cache = new HashMap<>();
    private final TermedRequester termedRequester;

    @Autowired
    public DefaultAuthorizationTermedService(TermedRequester termedRequester) {
        this.termedRequester = termedRequester;
    }

    @NotNull public Set<UUID> getOrganizationIds(UUID graphId) {
        return cache.computeIfAbsent(graphId, this::fetchOrganizationIds);
    }

    private Set<UUID> fetchOrganizationIds(UUID graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "references.contributor");
        params.add("where",
                "graph.id:" + graphId +
                " AND (type.id:" + Vocabulary +
                " OR type.id:" + TerminologicalVocabulary + ")");
        params.add("max", "-1");

        List<GenericNode> result = termedRequester.exchange("/node-trees", GET, params, new ParameterizedTypeReference<List<GenericNode>>() {});

        GenericNode vocabularyNode = requireSingle(result);

        return mapToSet(vocabularyNode.getReferences().getOrDefault("contributor", emptyList()), Identifier::getId);
    }
}
