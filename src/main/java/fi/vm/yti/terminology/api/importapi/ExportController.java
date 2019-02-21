package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.frontend.FrontendElasticSearchService;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.index.Vocabulary;
import io.swagger.annotations.ApiParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/export")
public class ExportController {

    private final FrontendTermedService termedService;
    private final ExportService exportService;
    private final FrontendElasticSearchService elasticSearchService;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final String namespaceRoot;
    private final String groupManagementUrl;
    private final boolean fakeLoginAllowed;

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    public ExportController(FrontendTermedService termedService, ExportService exportService,
            FrontendElasticSearchService elasticSearchService, FrontendGroupManagementService groupManagementService,
            AuthenticatedUserProvider userProvider, @Value("${namespace.root}") String namespaceRoot,
            @Value("${groupmanagement.public.url}") String groupManagementUrl,
            @Value("${fake.login.allowed:false}") boolean fakeLoginAllowed) {
        this.termedService = termedService;
        this.exportService = exportService;
        this.elasticSearchService = elasticSearchService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.namespaceRoot = namespaceRoot;
        this.groupManagementUrl = groupManagementUrl;
        this.fakeLoginAllowed = fakeLoginAllowed;
    }

    /**
     * export/{vocabularyID}/
     * Toteutetaan proxy api termed node-trees api:lle, jolle voi antaa parametrina ainakin seuraavat formaatit:
     * application/json
     * text/turtle
     * application/rdf+xml
     * 
     * API polku esim:
     * /export/{uuid}?accept=...
     * Mahdollista rakentaa tyyppi export polkuun esim: /export/type/Concept/
     * @param id
     * @return
     */
    @RequestMapping(value = "/{vocabularyID}", method = GET, produces = APPLICATION_JSON_VALUE)
    ResponseEntity export( @ApiParam(value = "Vocabulary UUID") @PathVariable("vocabularyID") UUID id,
    @ApiParam(value = "Export format JSON, RDF, TURTLE.") @RequestParam(value="format", required = true) String format) {
        System.out.println("uuid:"+id+" format:"+format);
        if(format.equalsIgnoreCase("JSON")){
            return exportService.getJSON(id);
        }
        return exportService.getRDF(id);
    }

    @RequestMapping(value = "/{vocabularyID}/type/{nodeType}", method = GET, produces = APPLICATION_JSON_VALUE)
    ResponseEntity export(@ApiParam(value = "Vocabulary UUID") @PathVariable("vocabularyID") UUID id,
            @ApiParam(value = "Type of requested nodes. (Concept, Collection)") @PathVariable("nodeType") String nodeType,
            @ApiParam(value = "Export format JSON, RDF, TURTLE.") @RequestParam(value = "format", required = true) String format) {
                System.out.println("uuid:"+id+" node="+nodeType+" format:"+format);
                if(format.equalsIgnoreCase("JSON")){
                    return exportService.getJSON(id);
                }
                return exportService.getRDF(id);
    }
}
