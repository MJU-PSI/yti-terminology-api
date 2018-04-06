package fi.vm.yti.terminology.api.synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(value = "/synchronize")
public class SynchronizationController {

    private final SynchronizationService synchronizationService;

    private static final Logger logger = LoggerFactory.getLogger(SynchronizationController.class);

    @Autowired
    public SynchronizationController(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @RequestMapping(method = GET, produces = TEXT_PLAIN_VALUE)
    public String synchronize() {
        logger.info("GET /synchronize requested");
        synchronizationService.synchronize();
        return "OK!";
    }
}
