package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.frontend.FrontendElasticSearchService;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.index.Vocabulary;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.GenericNodeInlined;
import fi.vm.yti.terminology.api.model.termed.Graph;
import io.swagger.annotations.ApiParam;

import org.springframework.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/export")
public class ExportController {

    private final FrontendTermedService termedService;
    private final ExportService exportService;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final String namespaceRoot;
    private final String groupManagementUrl;
    private final boolean fakeLoginAllowed;

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    public ExportController(FrontendTermedService termedService, ExportService exportService,
            FrontendGroupManagementService groupManagementService, AuthenticatedUserProvider userProvider,
            @Value("${namespace.root}") String namespaceRoot,
            @Value("${groupmanagement.public.url}") String groupManagementUrl,
            @Value("${fake.login.allowed:false}") boolean fakeLoginAllowed) {
        this.termedService = termedService;
        this.exportService = exportService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.namespaceRoot = namespaceRoot;
        this.groupManagementUrl = groupManagementUrl;
        this.fakeLoginAllowed = fakeLoginAllowed;
    }

    /**
     * export/{vocabularyID}/ Toteutetaan proxy api termed node-trees api:lle, jolle
     * voi antaa parametrina ainakin seuraavat formaatit: application/json
     * text/turtle application/rdf+xml
     * 
     * API polku esim: /export/{uuid}?accept=... Mahdollista rakentaa tyyppi export
     * polkuun esim: /export/type/Concept/
     * 
     * @param id
     * @return
     */
    @RequestMapping(value = "/{vocabularyID}", method = GET, produces = { APPLICATION_JSON_VALUE, "application/rdf+xml",
            "text/turtle" })
    ResponseEntity export(
            @ApiParam(value = "Vocabulary identifier (UUID/URI)") @PathVariable("vocabularyID") Object vocId,
            @ApiParam(value = "Export format JSON, RDF, TURTLE.") @RequestParam(value = "format", required = true) String format) {
        logger.debug("ExportController uuid:" + vocId + " format:" + format);

        ResponseEntity re = null;
        UUID id = null;
        // Try to cast incoming as UUID and if fails, assume it is Code ie. name of the
        // vocabulary
        try {
            id = UUID.fromString((String) vocId);
        } catch (IllegalArgumentException ex) {
            id = this.resolveCode((String) vocId);
        } catch (Exception e) {
            logger.error("Error fetching vocabulary id", e.getMessage());
            e.printStackTrace();
        }
        // Id resolved, go fetch data
        if (id != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("ExportController uuid:" + id.toString());
            }
            if (format.equalsIgnoreCase("JSON")) {
                re = exportService.getJSON(id);
            } else if (format.equalsIgnoreCase("rdf")) {
                re = exportService.getRDF(id);
            } else {
                re = exportService.getTXT(id);
            }
        } else {
            re = new ResponseEntity<>("{}", HttpStatus.NOT_FOUND);
        }
        return re;
    }

    @RequestMapping(value = "/{vocabularyID}/type/{nodeType}", produces = { APPLICATION_JSON_VALUE,
            "application/rdf+xml", "text/turtle" }, method = GET)
    ResponseEntity export(
            @ApiParam(value = "Vocabulary identifier (UUID/URI)") @PathVariable("vocabularyID") Object vocId,
            @ApiParam(value = "Type of requested nodes. (Concept, Collection, Term)") @PathVariable("nodeType") String nodeType,
            @ApiParam(value = "Export format JSON, RDF, TURTLE.") @RequestParam(value = "format", required = true) String format) {
        if (logger.isDebugEnabled()) {
            logger.debug("ID:" + vocId + " node=" + nodeType + " format:" + format);
        }
        UUID id = null;
        // Try to cast incoming as UUID and if fails, assume it is Code ie. name of the
        // vocabulary
        try {
            id = UUID.fromString((String) vocId);
        } catch (IllegalArgumentException ex) {

            id = this.resolveCode((String) vocId);
        } catch (Exception e) {
            logger.error("Error fetching vocabulary id", e.getMessage());
            id = null;
        }

        ResponseEntity re = null;
        // Id resolved, go fetch data
        if (id != null) {
            if(logger.isDebugEnabled()){
                logger.error("ExportController uuid");
            }
            if (format.equalsIgnoreCase("JSON")) {
                re = exportService.getJSON(id, nodeType);
            } else if (format.equalsIgnoreCase("rdf")) {
                re = exportService.getRDF(id, nodeType);
            } else {
                re = exportService.getTXT(id, nodeType);
            }
        } else {
            re = new ResponseEntity<>("{}", HttpStatus.NOT_FOUND);
        }
        return re;
    }

    private UUID resolveCode(String code) {
        UUID rv = null;
        List<Graph> graphs = termedService.getGraphs();
        List<Graph> matchingGraphs = graphs.stream().filter(o -> o.getCode().equalsIgnoreCase((String) code))
                .collect(Collectors.toList());

        List<UUID> idlist = new ArrayList<>();
        matchingGraphs.forEach(g -> {
            idlist.add(g.getId());
        });
        if (!idlist.isEmpty() && idlist.size() == 1) {
            rv = idlist.get(0);
        } else {
            logger.error("Several matches for " + code);
        }
        return rv;
    }
}
