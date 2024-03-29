package fi.vm.yti.terminology.api.system;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.vm.yti.terminology.api.config.UriProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System")
public class SystemController {

    private final SystemService systemService;
    private final ServiceUrls serviceUrls;
    private final boolean restrictFilterOptions;
    private final UriProperties uriProperties;

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    public SystemController(SystemService systemService,
                            ServiceUrls serviceUrls,
                            UriProperties uriProperties,
                            @Value("${front.restrictFilterOptions}") boolean restrictFilterOptions) {
        this.systemService = systemService;
        this.serviceUrls = serviceUrls;
        this.uriProperties = uriProperties;
        this.restrictFilterOptions = restrictFilterOptions;
    }

    @Operation(summary = "Get entity counts", description = "Get counts of main entity types, i.e., terminologies and concepts.")
    @ApiResponse(responseCode = "200", description = "Returns terminology and concept counts as JSON")
    @GetMapping(path = "/counts", produces = APPLICATION_JSON_VALUE)
    ResponseEntity<String> countStatistics (
        @Parameter(description = "Get also concept counts per terminology")
        @RequestParam(required = false, defaultValue = "false") boolean full) throws IOException {

        logger.info("GET /api/v1/system/counts requested (full: " + full + ")");
        return systemService.countStatistics(full);
    }

    @Operation(summary = "Get cluster configuration", description = "Get configuration options mainly relevant to terminology UI")
    @ApiResponse(responseCode = "200", description = "Returns some configuration options as JSON")
    @RequestMapping(path = "/config", method = GET, produces = APPLICATION_JSON_VALUE)
    public Configuration getConfiguration() {
        logger.info("GET /api/v1/system/config requested");

        Configuration conf = new Configuration();
        conf.setCodeListUrl(this.serviceUrls.getCodeListUrl());
        conf.setDataModelUrl(this.serviceUrls.getDataModelUrl());
        conf.setCommentsUrl(this.serviceUrls.getCommentsUrl());
        conf.setGroupmanagementUrl(this.serviceUrls.getGroupManagementUrl());
        conf.setMessagingEnabled(this.serviceUrls.getMessagingEnabled());
        conf.setEnv(this.serviceUrls.getEnv());
        conf.setNamespaceRoot(this.uriProperties.getUriHostPathAddress());
        conf.setRestrictFilterOptions(this.restrictFilterOptions);

        return conf;
    }
}
