package fi.csc.termed.api.index;

import fi.csc.termed.api.common.NodeIdentifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@RestController
public class NotificationController {

    private final ElasticSearchService elasticSearchService;

    private final Object lock = new Object();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final List<String> conceptTypes = singletonList("Concept");
    private static final List<String> vocabularyTypes = asList("TerminologicalVocabulary", "Vocabulary");

    @Autowired
    public NotificationController(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping("/notify")
    public void notify(@RequestBody TermedNotification notification) throws IOException, InterruptedException {
        log.debug("Notification received");

        synchronized(this.lock) {

            Map<String, List<NodeIdentifier>> nodesByGraphId =
                    notification.body.nodes.stream().collect(Collectors.groupingBy(node -> node.type.graph.id));

            for (Map.Entry<String, List<NodeIdentifier>> entries : nodesByGraphId.entrySet()) {

                String graphId = entries.getKey();
                List<NodeIdentifier> nodes = entries.getValue();

                List<String> vocabularies = extractIdsOfType(nodes, vocabularyTypes);
                List<String> concepts = extractIdsOfType(nodes, conceptTypes);

                switch (notification.type) {
                    case NodeSavedEvent:
                        this.elasticSearchService.updateIndexAfterUpdate(new AffectedNodes(graphId, vocabularies, concepts));
                        break;
                    case NodeDeletedEvent:
                        this.elasticSearchService.updateIndexAfterDelete(new AffectedNodes(graphId, vocabularies, concepts));
                        break;
                }
            }
        }
    }

    private static @NotNull List<String> extractIdsOfType(@NotNull List<NodeIdentifier> nodes, @NotNull List<String> types) {
        return nodes.stream()
                .filter(node -> types.contains(node.type.id))
                .map(node -> node.id)
                .collect(toList());
    }
}
