package fi.csc.termed.search.controller;

import fi.csc.termed.search.dto.TermedNotification;
import fi.csc.termed.search.service.ElasticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class NotificationController {

    private final ElasticSearchService elasticSearchService;

    private final Object lock = new Object();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public NotificationController(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping("/notify")
    public void notify(@RequestBody TermedNotification notification) throws IOException, InterruptedException {
        log.debug("Notification received");

        synchronized(this.lock) {
            for (TermedNotification.Node node : notification.getBody().getNodes()) {
                switch(node.getType().getId()) {
                    case Term:
                        break;
                    case Concept:
                        this.elasticSearchService.updateIndexAfterConceptEvent(notification.getType(), node);
                        break;
                    case TerminologicalVocabulary:
                    case Vocabulary:
                        this.elasticSearchService.updateIndexAfterVocabularyEvent(notification.getType(), node);
                        break;
                }
            }
        }
    }
}
