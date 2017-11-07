package fi.vm.yti.terminology.api;

import fi.vm.yti.terminology.api.synchronization.SynchronizationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SynchronizationTask {

    private final SynchronizationService synchronizationService;

    SynchronizationTask(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    void synchronize() {
        this.synchronizationService.synchronize();
    }
}
