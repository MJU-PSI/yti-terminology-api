package fi.vm.yti.terminology.api.system;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final SystemService systemService;

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    /**
     * 
     * @return
     */
    @ApiResponse(code = 200, message = "Returns JSON with item counts. Terminologies, concepts")
    @RequestMapping(value = "/system/count", method = GET, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<String> countStatistics() {
        return systemService.countStatistics();
    }
}
