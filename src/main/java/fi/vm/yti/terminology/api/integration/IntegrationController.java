package fi.vm.yti.terminology.api.integration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.frontend.FrontendElasticSearchService;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.model.integration.ConceptSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/integration")
public class IntegrationController {

    private final FrontendTermedService termedService;
    private final IntegrationService integrationService;
    private final FrontendElasticSearchService elasticSearchService;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final String namespaceRoot;
    private final String groupManagementUrl;
    private final boolean fakeLoginAllowed;

    private static final Logger logger = LoggerFactory.getLogger(IntegrationController.class);

    public IntegrationController(FrontendTermedService termedService, IntegrationService integrationService,
            FrontendElasticSearchService elasticSearchService, FrontendGroupManagementService groupManagementService,
            AuthenticatedUserProvider userProvider, @Value("${namespace.root}") String namespaceRoot,
            @Value("${groupmanagement.public.url}") String groupManagementUrl,
            @Value("${fake.login.allowed:false}") boolean fakeLoginAllowed) {
        this.termedService = termedService;
        this.integrationService = integrationService;
        this.elasticSearchService = elasticSearchService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.namespaceRoot = namespaceRoot;
        this.groupManagementUrl = groupManagementUrl;
        this.fakeLoginAllowed = fakeLoginAllowed;
    }

    @ApiResponse(code = 200, message = "Returns JSON with Vocabulary-list, pref-labels, descriptions, status and modified date")
    @RequestMapping(value = "/vocabulary/{vocabularyId}/conceptSuggestion", method = POST, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    ResponseEntity conceptSuggestion(
            @ApiParam(value = "Vocabulary where new concept is suggested.") @PathVariable("vocabularyId") String vocabularyId,
            @RequestBody ConceptSuggestion incomingConcept) {
        return integrationService.handleConceptSuggestion(vocabularyId, incomingConcept);
    }

    @ApiResponse(code = 200, message = "Returns JSON with Vocabulary-list.")
     @RequestMapping(value = "/containers", method = GET, produces = APPLICATION_JSON_VALUE)
    ResponseEntity  containers(
                               @ApiParam(value = "Language code for sorting results.") @RequestParam(value="language", required = false, defaultValue = "fi") String language,
                               @ApiParam(value = "Pagination parameter for page size.") @RequestParam(value="pageSize", required = true, defaultValue= "0") int pageSize,
                               @ApiParam(value = "Pagination parameter for start index.") @RequestParam(value="from", required = false, defaultValue= "0")  int from,
                               @ApiParam(value = "Status enumerations in CSL format.") @RequestParam(value="status", required = false) String statusEnum,                               
                               @ApiParam(value = "After date filtering parameter, results will be codes with modified date after this ISO 8601 formatted date string.") @RequestParam(value="after", required = false) String after,
                               @ApiParam(value = "Include pagination related meta element and wrap response items in bulk array.") @RequestParam(value="includeMeta", required = false) boolean includeMeta
    ) {
        System.out.println("status="+statusEnum);
        return integrationService.handleContainers(language, pageSize, from, statusEnum, after, includeMeta);
    }

    @ApiResponse(code = 200, message = "Returns JSON with Concept-list.")
    @RequestMapping(value = "/resources", method = GET, produces = APPLICATION_JSON_VALUE)
    ResponseEntity  resources(@ApiParam(value = "Container URL.") @RequestParam(value="container", required = false) String container){
        System.out.println("Container URL="+container);
        return integrationService.handleResources(container);
    }
}
