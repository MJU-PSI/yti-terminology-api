package fi.vm.yti.terminology.api;

import fi.vm.yti.terminology.api.synchronization.SynchronizationService;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SynchronizationTask {

    private final SynchronizationService synchronizationService;
    private final boolean synchronizationEnabled;

    SynchronizationTask(SynchronizationService synchronizationService,
                        Environment environment) {
        this.synchronizationService = synchronizationService;
        this.synchronizationEnabled = isSynchronizationEnabled(environment);
    }

    private boolean isSynchronizationEnabled(Environment environment) {

        for (String profile : environment.getActiveProfiles()) {
            if ("local".equals(profile)) {
                return false;
            }
        }

        return true;
    }

    @Scheduled(cron = "0 */5 * * * *")
    void synchronize() {
        if (synchronizationEnabled) {
            this.synchronizationService.synchronize();
        }
    }
}
