package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.frontend.FrontendElasticSearchService;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/importapi")
public class ImportController {

    private final FrontendTermedService termedService;
    private final ImportService importService;
    private final FrontendElasticSearchService elasticSearchService;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final String namespaceRoot;
    private final String groupManagementUrl;
    private final boolean fakeLoginAllowed;

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    public ImportController(FrontendTermedService termedService,
                            ImportService importService,
                            FrontendElasticSearchService elasticSearchService,
                            FrontendGroupManagementService groupManagementService,
                            AuthenticatedUserProvider userProvider,
                            @Value("${namespace.root}") String namespaceRoot,
                            @Value("${groupmanagement.public.url}") String groupManagementUrl,
                            @Value("${fake.login.allowed:false}") boolean fakeLoginAllowed) {
        this.termedService = termedService;
        this.importService = importService;
        this.elasticSearchService = elasticSearchService;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.namespaceRoot = namespaceRoot;
        this.groupManagementUrl = groupManagementUrl;
        this.fakeLoginAllowed = fakeLoginAllowed;
    }

    @RequestMapping(value = "/import/{format}", method = POST, produces = APPLICATION_JSON_VALUE)
    ResponseEntity importTerms(@PathVariable("format") String format,
                               @RequestParam UUID vocabularityId,
                               @RequestBody VOCABULARY ntrfDocument) {
        return importService.handleNtrfDocument(format,vocabularityId, ntrfDocument);
    }
}
