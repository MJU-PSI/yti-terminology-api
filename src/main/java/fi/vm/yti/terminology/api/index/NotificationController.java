package fi.vm.yti.terminology.api.index;

import fi.vm.yti.terminology.api.model.termed.TermedIdentifier;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@RestController
public class NotificationController {

    private final IndexElasticSearchService elasticSearchService;

    private final Object lock = new Object();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final List<String> conceptTypes = singletonList("Concept");
    private static final List<String> vocabularyTypes = asList("TerminologicalVocabulary", "Vocabulary");

    @Autowired
    public NotificationController(IndexElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @RequestMapping("/notify")
    public void notify(@RequestBody TermedNotification notification) throws IOException, InterruptedException {
        log.debug("Notification received");

        synchronized(this.lock) {

            Map<UUID, List<TermedIdentifier>> nodesByGraphId =
                    notification.body.nodes.stream().collect(Collectors.groupingBy(node -> node.getType().getGraph().getId()));

            for (Map.Entry<UUID, List<TermedIdentifier>> entries : nodesByGraphId.entrySet()) {

                UUID graphId = entries.getKey();
                List<TermedIdentifier> nodes = entries.getValue();

                List<UUID> vocabularies = extractIdsOfType(nodes, vocabularyTypes);
                List<UUID> concepts = extractIdsOfType(nodes, conceptTypes);

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

    private static @NotNull List<UUID> extractIdsOfType(@NotNull List<TermedIdentifier> nodes, @NotNull List<String> types) {
        return nodes.stream()
                .filter(node -> types.contains(node.getType().getId()))
                .map(TermedIdentifier::getId)
                .collect(toList());
    }
}
