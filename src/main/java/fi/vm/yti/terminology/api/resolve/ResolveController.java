package fi.vm.yti.terminology.api.resolve;

import fi.vm.yti.terminology.api.TermedContentType;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;

@Controller
@RequestMapping("/api")
public class ResolveController {

    private final UrlResolverService urlResolverService;
    private final TermedRequester termedRequester;

    @Autowired
    ResolveController(UrlResolverService urlResolverService,
                      TermedRequester termedRequester) {
        this.urlResolverService = urlResolverService;
        this.termedRequester = termedRequester;
    }

    @GetMapping("/resolve")
    public String resolve(@RequestParam String uri, @RequestHeader("Accept") String acceptHeader) {
        return "redirect:" + urlResolverService.resolveResourceUrl(uri, ResolvableContentType.fromString(acceptHeader));
    }

    @GetMapping(value = "/vocabulary")
    @ResponseBody
    public String getVocabulary(@RequestParam UUID graphId, @RequestHeader("Accept") String acceptHeader) {
        return getResourceFromTermed(graphId, asList(NodeType.Vocabulary, NodeType.TerminologicalVocabulary), TermedContentType.fromString(acceptHeader), null);
    }

    @GetMapping("/concept")
    @ResponseBody
    public String getConcept(@RequestParam UUID graphId, @RequestParam UUID id, @RequestHeader("Accept") String acceptHeader) {
        return getResourceFromTermed(graphId, singletonList(NodeType.Concept), TermedContentType.fromString(acceptHeader), id);
    }

    @GetMapping("/collection")
    @ResponseBody
    public String getCollection(@RequestParam UUID graphId, @RequestParam UUID id, @RequestHeader("Accept") String acceptHeader) {
        return getResourceFromTermed(graphId, singletonList(NodeType.Collection), TermedContentType.fromString(acceptHeader), id);
    }

    private String getResourceFromTermed(@NotNull UUID graphId, @NotNull List<NodeType> types, TermedContentType contentType, @Nullable UUID resourceId) {

        Parameters params = new Parameters();
        params.add("select", "*");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("where", formatWhereClause(graphId, types, resourceId));

        return requireNonNull(termedRequester.exchange("/node-trees", GET, params, String.class, contentType));
    }

    private static @NotNull String formatWhereClause(@NotNull UUID graphId, @NotNull List<NodeType> types, @Nullable UUID resourceId) {

        if (types.size() == 0) {
            throw new IllegalArgumentException("Must include at least one type");
        }

        String typeClause = types.stream().map(t -> "type.id:" + t.name()).collect(Collectors.joining(" OR "));

        return "graph.id:" + graphId +
                " AND (" + typeClause + ")" + (resourceId != null ? " AND id:" + resourceId : "");
    }
}
