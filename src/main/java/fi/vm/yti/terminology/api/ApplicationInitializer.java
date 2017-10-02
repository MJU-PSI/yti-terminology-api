package fi.vm.yti.terminology.api;

import fi.vm.yti.terminology.api.index.ElasticEndpointException;
import fi.vm.yti.terminology.api.index.ElasticSearchService;
import fi.vm.yti.terminology.api.index.TermedEndpointException;
import fi.vm.yti.terminology.api.index.TermedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class ApplicationInitializer {

    private final ElasticSearchService elasticSearchService;
    private final TermedService termedApiService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String hookId;

    @Value("${notify.hook.url}")
    private String NOTIFY_HOOK_URL;

    @Autowired
    public ApplicationInitializer(ElasticSearchService elasticSearchService, TermedService termedApiService) {
        this.elasticSearchService = elasticSearchService;
        this.termedApiService = termedApiService;
    }

    @PostConstruct
    public void onInit() throws InterruptedException {

        for (int retryCount = 0; retryCount < 10; retryCount++) {
            try {

                if (retryCount > 0) {
                    log.info("Retrying");
                }

                this.elasticSearchService.initIndex();

                if (!NOTIFY_HOOK_URL.isEmpty()) {
                    registerNotificationUrl(NOTIFY_HOOK_URL);
                }

                return;

            } catch (TermedEndpointException | ElasticEndpointException e) {
                log.warn("Initialization failed (" + retryCount + ")", e);
                Thread.sleep(20000);
            }
        }

        throw new RuntimeException("Cannot initialize");
    }

    @PreDestroy
    public void onDestroy() {
        if (!NOTIFY_HOOK_URL.isEmpty()) {
            this.unRegisterNotificationUrl();
        }
    }

    private void registerNotificationUrl(String url) {
        log.info("Registering change listener to termed API");

        hookId = termedApiService.registerChangeListener(url);

        if (hookId != null) {
            log.info("Registered change listener to termed API with id: " + hookId);
        } else {
            log.warn("Unable to register change listener to termed API");
        }
    }

    private void unRegisterNotificationUrl() {
        if (hookId != null) {
            log.info("Deleting change listener from termed API having id: " + hookId);
            if (termedApiService.deleteChangeListener(hookId)) {
                log.info("Deleted change listener successfully");
            } else {
                log.error("Unable to delete change listener from termed API using id: " + hookId);
            }
        }
    }
}
