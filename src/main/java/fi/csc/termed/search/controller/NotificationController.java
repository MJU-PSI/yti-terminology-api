package fi.csc.termed.search.controller;

import fi.csc.termed.search.dto.TermedNotification;
import fi.csc.termed.search.service.ElasticSearchService;
import fi.csc.termed.search.service.TermedApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import java.io.IOException;

@RestController
public class NotificationController {

    private final ElasticSearchService elasticSearchService;
    private final TermedApiService termedApiService;

    private static String hookId = null;

    private final Object lock = new Object();

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public NotificationController(ElasticSearchService elasticSearchService, TermedApiService termedApiService) {
        this.elasticSearchService = elasticSearchService;
        this.elasticSearchService.initIndex();
        this.termedApiService = termedApiService;
        registerNotificationUrl();
    }

    @RequestMapping("/notify")
    public void notify(@RequestBody TermedNotification notification) throws IOException, InterruptedException {
        log.debug("Notification received");

        synchronized(this.lock) {
            switch(notification.getBody().getNode().getType().getId()) {
                case Term:
                    break;
                case Concept:
                    this.elasticSearchService.updateIndexAfterConceptEvent(notification);
                    break;
                case TerminologicalVocabulary:
                case Vocabulary:
                    this.elasticSearchService.updateIndexAfterVocabularyEvent(notification);
                    break;
            }
        }
    }

    @PreDestroy
    private void unRegisterNotificationUrl() {
        if(hookId != null) {
            log.info("Deleting change listener from termed API having id: " + hookId);
            if(termedApiService.deleteChangeListener(hookId)) {
                log.info("Deleted change listener successfully");
            } else {
                log.error("Unable to delete change listener from termed API using id: " + hookId);
            }
        }
    }

    private void registerNotificationUrl() {
        log.info("Registering change listener to termed API");
        hookId = termedApiService.registerChangeListener();
        if(hookId != null) {
            log.info("Registered change listener to termed API with id: " + hookId);
        } else {
            log.error("Unable to register change listener to termed API");
        }
    }
}
