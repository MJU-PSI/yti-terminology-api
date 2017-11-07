package fi.vm.yti.terminology.api.synchronization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SynchronizationController {

    private final SynchronizationService synchronizationService;

    @Autowired
    public SynchronizationController(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @RequestMapping("/synchronize")
    public String synchronize() {
        synchronizationService.synchronize();
        return "OK!";
    }

}
