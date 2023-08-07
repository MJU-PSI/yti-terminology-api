package fi.vm.yti.terminology.api.resolve;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.terminology.api.TermedContentType;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@RequestMapping("/api/v1")
@Tag(name = "Resolve")
public class ResolveController {

    private static final Logger logger = LoggerFactory.getLogger(ResolveController.class);

    private final ResolveService urlResolverService;
    private final String applicationUrl;
    private final String betaUrl;

    @Autowired
    ResolveController(ResolveService urlResolverService,
                      @Value("${application.public.url}") String applicationUrl,
                      @Value("${application.public.beta.url:}") String betaUrl) {
        this.urlResolverService = urlResolverService;
        this.applicationUrl = applicationUrl;
        this.betaUrl = betaUrl;
    }

    @Operation(summary = "Resolve a resource URI", description = "Resolve the given terminology, concept or collection URI and forward to appropriate address to either view (in UI) or fetch (JSON or other format) the resource")
    @ApiResponse(responseCode = "303", description = "Response to forward requester to either UI, or download address for the requested format")
    @ApiResponse(responseCode = "400", description = "The given URI is not syntactically valid")
    @ApiResponse(responseCode = "404", description = "No resource found with the given URI")
    @GetMapping(path = "/resolve")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> resolve(
        @Parameter(description = "The resource URI to resolve") @RequestParam String uri,
        @Parameter(description = "Environment to redirect, e.g. awstest") @RequestParam(required = false) String env,
        @Parameter(
            description = "Requested format. This parameter has priority over the Accept header.",
            schema = @Schema(allowableValues = { "application/json", "application/ld+json", "application/rdf+xml", "text/turtle", "text/html" })
        )
        @RequestParam(required = false) String format,
        @Parameter(description = "Requested format. Depending on format the request is forwarded either to the UI or to download address.")
        @RequestHeader("Accept") String acceptHeader) {

        logger.info("Resolving URI: " + StringUtils.normalizeSpace(uri) + " [format=\"" + StringUtils.normalizeSpace(format) + "\", accept=\"" + StringUtils.normalizeSpace(acceptHeader) + "\"]");

        // Check whether uri is syntactically valid.
        try {
            URI u = new URI(uri);
            URL ur = u.toURL();
        } catch (Exception e) {
            logger.warn("Invalid URI " + StringUtils.normalizeSpace(uri), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // ok, continue into the resolver
        try {
            ResolvedResource resource = urlResolverService.resolveResource(uri);
            ResolvableContentType contentType = ResolvableContentType.fromString(format, acceptHeader);

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder
                    .append(!"".equals(betaUrl) && env != null && env.endsWith("_v2") ? betaUrl : applicationUrl)
                    .append(formatPath(resource, contentType))
                    .append(!contentType.isHandledByFrontend() && !StringUtils.isEmpty(format) && contentType.getMediaType().equals(format)
                        ? "&format=" + format.replaceAll("\\+", "%2b")
                        : "");

            String responseValue = urlBuilder.toString();

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setLocation(new URI(responseValue));
            return new ResponseEntity<>(httpHeaders, HttpStatus.SEE_OTHER);
        } catch (Exception ex) {
            logger.error("Error on resolve", ex);
            return new ResponseEntity<>("{}", HttpStatus.NOT_FOUND);
        }
    }

    private static String formatPath(ResolvedResource resource,
                                     ResolvableContentType contentType) {

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
                    return "/terminology-api/api/v1/vocabulary?graphId=" + resource.getGraphId();
                case CONCEPT:
                    return "/terminology-api/api/v1/concept?graphId=" + resource.getGraphId() + "&id=" + resource.getId();
                case COLLECTION:
                    return "/terminology-api/api/v1/collection?graphId=" + resource.getGraphId() + "&id=" + resource.getId();
                default:
                    throw new RuntimeException("Unsupported type: " + resource.getType());
            }
        }
    }

    @Operation(summary = "Get a terminology", description = "Fetch a terminology identified by the UUID in requested format")
    @ApiResponse(responseCode = "200", description = "If the terminology was found then it is returned in requested format. If the given ID did not match a terminology then behaviour is undefined.")
    @GetMapping(path = "/vocabulary", produces = { APPLICATION_JSON_VALUE, "application/ld+json", "application/rdf+xml", "text/turtle" })
    public ResponseEntity<String> getVocabulary(@Parameter(description = "The ID of the requested terminology") @RequestParam UUID graphId,
                                                @Parameter(
                                                    description = "Requested format. This parameter has priority over the Accept header. If neither format parameter nor the accept header is valid then JSON is returned.",
                                                    schema = @Schema(allowableValues = { "application/json", "application/ld+json", "application/rdf+xml", "text/turtle" })
                                                )
                                                @RequestParam(required = false) String format,
                                                @Parameter(description = "Requested format. The request parameter \"format\" has priority over the Accept header.")
                                                @RequestHeader("Accept") String acceptHeader) {

        logger.info("Fetching terminology [id=\"" + StringUtils.normalizeSpace(graphId.toString()) + "\", format=\"" + StringUtils.normalizeSpace(format) + "\", accept=\"" + StringUtils.normalizeSpace(acceptHeader) + "\"]");
        TermedContentType tct = TermedContentType.fromString(format, acceptHeader);
        return buildResponse(urlResolverService.getTerminology(graphId, tct), tct);
    }

    @Operation(summary = "Get a concept", description = "Fetch a concept identified by terminology and concept IDs in requested format")
    @ApiResponse(responseCode = "200", description = "If the concept was found then it is returned in requested format. If the given IDs did not match a concept then behaviour is undefined.")
    @GetMapping(path = "/concept", produces = { APPLICATION_JSON_VALUE, "application/ld+json", "application/rdf+xml", "text/turtle" })
    public ResponseEntity<String> getConcept(@Parameter(description = "The ID of the terminology containing the concept") @RequestParam UUID graphId,
                                             @Parameter(description = "The ID of the requested concept") @RequestParam UUID id,
                                             @Parameter(
                                                 description = "Requested format. This parameter has priority over the Accept header. If neither format parameter nor the accept header is valid then JSON is returned.",
                                                 schema = @Schema(allowableValues = { "application/json", "application/ld+json", "application/rdf+xml", "text/turtle" })
                                             )
                                             @RequestParam(required = false) String format,
                                             @Parameter(description = "Requested format. The request parameter \"format\" has priority over the Accept header.")
                                             @RequestHeader("Accept") String acceptHeader) {
        logger.info("Fetching concept [termonology=\"" + StringUtils.normalizeSpace(graphId.toString()) + "\", id=\"" + StringUtils.normalizeSpace(id.toString()) + "\", format=\"" + StringUtils.normalizeSpace(format) + "\", accept=\"" + StringUtils.normalizeSpace(acceptHeader) + "\"]");
        TermedContentType tct = TermedContentType.fromString(format, acceptHeader);
        return buildResponse(urlResolverService.getResource(graphId, singletonList(NodeType.Concept), tct, id), tct);
    }

    @Operation(summary = "Get a concept collection", description = "Fetch a concept collection identified by terminology and collection IDs in requested format")
    @ApiResponse(responseCode = "200", description = "If the collection was found then it is returned in requested format. If the given IDs did not match a collection then behaviour is undefined.")
    @GetMapping(path = "/collection", produces = { APPLICATION_JSON_VALUE, "application/ld+json", "application/rdf+xml", "text/turtle" })
    public ResponseEntity<String> getCollection(@Parameter(description = "The ID of the terminology containing the concept") @RequestParam UUID graphId,
                                                @Parameter(description = "The ID of the requested collection") @RequestParam UUID id,
                                                @Parameter(
                                                    description = "Requested format. This parameter has priority over the Accept header. If neither format parameter nor the accept header is valid then JSON is returned.",
                                                    schema = @Schema(allowableValues = { "application/json", "application/ld+json", "application/rdf+xml", "text/turtle" })
                                                )
                                                @RequestParam(required = false) String format,
                                                @Parameter(description = "Requested format. The request parameter \"format\" has priority over the Accept header.")
                                                @RequestHeader("Accept") String acceptHeader) {
        logger.info("Fetching collection [termonology=\"" + StringUtils.normalizeSpace(graphId.toString()) + "\", id=\"" + StringUtils.normalizeSpace(id.toString()) + "\", format=\"" + StringUtils.normalizeSpace(format) + "\", accept=\"" + StringUtils.normalizeSpace(acceptHeader) + "\"]");
        TermedContentType tct = TermedContentType.fromString(format, acceptHeader);
        return buildResponse(urlResolverService.getResource(graphId, singletonList(NodeType.Collection), tct, id), tct);
    }

    private ResponseEntity<String> buildResponse(String body,
                                                 TermedContentType type) {
        return ResponseEntity
            .ok()
            .contentType(MediaType.valueOf(type.getContentType()))
            .body(body);
    }
}
