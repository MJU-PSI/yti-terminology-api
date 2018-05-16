package fi.vm.yti.terminology.api.resolve;

import fi.vm.yti.terminology.api.TermedContentType;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Controller
@RequestMapping("/api")
public class ResolveController {

    private final ResolveService urlResolverService;
    private final String applicationUrl;

    @Autowired
    ResolveController(ResolveService urlResolverService,
                      @Value("${application.public.url}") String applicationUrl) {
        this.urlResolverService = urlResolverService;
        this.applicationUrl = applicationUrl;
    }

    @GetMapping("/resolve")
    public String resolve(@RequestParam String uri, @RequestHeader("Accept") String acceptHeader) {

        ResolvedResource resource = urlResolverService.resolveResource(uri);
        ResolvableContentType contentType = ResolvableContentType.fromString(acceptHeader);

        return applicationUrl + formatPath(resource, contentType);
    }

    private static String formatPath(ResolvedResource resource, ResolvableContentType contentType) {

        if (contentType.isHandledByFrontend()) {
            switch (resource.getType()) {
                case VOCABULARY:
                    return "/concepts/" + resource.getGraphId();
                case CONCEPT:
                    return "/concepts/" + resource.getGraphId() + "/concept/" + resource.getId();
                case COLLECTION:
                    return "/concepts/" + resource.getGraphId() + "/collection/" + resource.getId();
                default:
                    throw new RuntimeException("Unsupported type: " + resource.getType());
            }
        } else {
            switch (resource.getType()) {
                case VOCABULARY:
                    return "/api/vocabulary?graphId=" + resource.getGraphId();
                case CONCEPT:
                    return "/api/concept?graphId=" + resource.getGraphId() + "&id=" + resource.getId();
                case COLLECTION:
                    return "/api/collection?graphId=" + resource.getGraphId() + "&id=" + resource.getId();
                default:
                    throw new RuntimeException("Unsupported type: " + resource.getType());
            }
        }
    }

    @GetMapping("/vocabulary")
    @ResponseBody
    public String getVocabulary(@RequestParam UUID graphId, @RequestHeader("Accept") String acceptHeader) {
        return urlResolverService.getResource(graphId, asList(NodeType.Vocabulary, NodeType.TerminologicalVocabulary), TermedContentType.fromString(acceptHeader), null);
    }

    @GetMapping("/concept")
    @ResponseBody
    public String getConcept(@RequestParam UUID graphId, @RequestParam UUID id, @RequestHeader("Accept") String acceptHeader) {
        return urlResolverService.getResource(graphId, singletonList(NodeType.Concept), TermedContentType.fromString(acceptHeader), id);
    }

    @GetMapping("/collection")
    @ResponseBody
    public String getCollection(@RequestParam UUID graphId, @RequestParam UUID id, @RequestHeader("Accept") String acceptHeader) {
        return urlResolverService.getResource(graphId, singletonList(NodeType.Collection), TermedContentType.fromString(acceptHeader), id);
    }
}
