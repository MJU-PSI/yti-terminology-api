package fi.vm.yti.terminology.api.synchronization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(value = "/synchronize")
public class SynchronizationController {

    private final SynchronizationService synchronizationService;

    @Autowired
    public SynchronizationController(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @RequestMapping(method = GET, produces = TEXT_PLAIN_VALUE)
    public String synchronize() {
        synchronizationService.synchronize();
        return "OK!";
    }
}
