package fi.csc.termed.api;

import fi.csc.termed.api.service.ElasticSearchService;
import fi.csc.termed.api.service.TermedApiService;
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
    private final TermedApiService termedApiService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String hookId;

    @Value("${notify.hook.url}")
    private String NOTIFY_HOOK_URL;

    @Autowired
    public ApplicationInitializer(ElasticSearchService elasticSearchService, TermedApiService termedApiService) {
        this.elasticSearchService = elasticSearchService;
        this.termedApiService = termedApiService;
    }

    @PostConstruct
    public void onInit() {
        this.elasticSearchService.initIndex();

        if (!NOTIFY_HOOK_URL.isEmpty()) {
            registerNotificationUrl(NOTIFY_HOOK_URL);
        }
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
