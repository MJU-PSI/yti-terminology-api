package fi.csc.termed.search.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.csc.termed.search.domain.Concept;
import fi.csc.termed.search.dto.TermedNotification;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;

@Service
public class ElasticSearchService {

    @Value("${search.index.file}")
    private String CREATE_INDEX_FILENAME;

    @Value("${search.index.mapping.file}")
    private String CREATE_MAPPINGS_FILENAME;

    @Value("${search.index.name}")
    private String INDEX_NAME;

    @Value("${search.index.mapping.type}")
    private String INDEX_MAPPING_TYPE;

    @Value("${search.index.deleteIndexOnAppRestart}")
    private boolean DELETE_INDEX_ON_APP_RESTART;

    private final RestClient esRestClient;
    private final TermedApiService termedApiService;
    private final JsonParser jsonParser = new JsonParser();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ElasticSearchService(TermedApiService termedApiService,
                                @Value("${search.host.url}") String searchHostUrl,
                                @Value("${search.host.port}") int searchHostPort,
                                @Value("${search.host.scheme}") String searchHostScheme) {
        this.termedApiService = termedApiService;
        this.esRestClient = RestClient.builder(new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();
    }

    public void initIndex() {

        if (DELETE_INDEX_ON_APP_RESTART) {
            deleteIndex();
        }

        if (!indexExists() && createIndex() && createMapping()) {
           doFullIndexing();
        }
    }

    public void doFullIndexing() {
        termedApiService.fetchAllAvailableGraphIds().forEach(this::indexListOfConceptsInGraph);
    }

    public void updateIndexAfterConceptEvent(@NotNull TermedNotification notification) {

        String conceptId = notification.getBody().getNode().getId();
        String graphId = notification.getBody().getNode().getType().getGraph().getId();
        String documentId = Concept.formDocumentId(graphId, conceptId);
        Concept previousIndexedConcept = getConceptFromIndex(documentId);
        List<String> previousBroader = previousIndexedConcept != null ? previousIndexedConcept.getBroaderIds() : emptyList();
        List<String> previousNarrower = previousIndexedConcept != null ? previousIndexedConcept.getNarrowerIds() : emptyList();

        switch (notification.getType()) {
            case NodeSavedEvent:
                Concept concept = termedApiService.getConcept(graphId, conceptId);

                if (concept != null) {

                    // First reindex the saved concept

                    // Next reindex broader concepts in case the saved concept's broader concept list changed
                    // Basically each broader concept's hasNarrower index value needs to be revised

                    // First compare API's broader concepts and compare against the corresponding documents in index
                    // Case: A concept becomes some concept's child (index doc does not contain the new broader concept)

                    // Then the other way around: Compare index documents against API's broader concepts
                    // Case: A concept is removed from being some concept's child

                    // Narrower and same logic as above

                    HashSet<String> tryUpdateConceptIds = new HashSet<>();
                    tryUpdateConceptIds.addAll(previousBroader);
                    tryUpdateConceptIds.addAll(concept.getBroaderIds());
                    tryUpdateConceptIds.addAll(previousNarrower);
                    tryUpdateConceptIds.addAll(concept.getNarrowerIds());

                    List<Concept> possiblyUpdatedConcepts = new ArrayList<>(tryUpdateConceptIds.size() + 1);
                    possiblyUpdatedConcepts.add(concept);
                    possiblyUpdatedConcepts.addAll(termedApiService.getConcepts(graphId, tryUpdateConceptIds));

                    bulkUpdateAndDeleteDocumentsToIndex(possiblyUpdatedConcepts, emptyList(), true);
                }
                break;
            case NodeDeletedEvent:

                HashSet<String> tryUpdateConceptIds = new HashSet<>();
                tryUpdateConceptIds.addAll(previousBroader);
                tryUpdateConceptIds.addAll(previousNarrower);

                List<Concept> possiblyUpdatedConcepts = termedApiService.getConcepts(graphId, tryUpdateConceptIds);
                bulkUpdateAndDeleteDocumentsToIndex(possiblyUpdatedConcepts, singletonList(documentId), true);
                break;
        }
    }

    public void updateIndexAfterVocabularyEvent(@NotNull TermedNotification notification) {

        String graphId = notification.getBody().getNode().getType().getGraph().getId();

        deleteDocumentsFromIndexByGraphId(graphId);

        switch (notification.getType()) {
            case NodeSavedEvent:
                indexListOfConceptsInGraph(graphId);
                break;
        }
    }

    private void indexListOfConceptsInGraph(@NotNull String graphId) {
        log.info("Trying to index concepts of graph " + graphId);

        List<Concept> concepts = termedApiService.getAllConceptsForGraph(graphId);
        bulkUpdateAndDeleteDocumentsToIndex(concepts, emptyList(), false);

        log.info("Indexed " + concepts.size() + " concepts");
    }

    private void deleteIndex() {
        log.info("Deleting elasticsearch index: " + INDEX_NAME);
        try {
            Response response = esRestClient.performRequest("DELETE", "/" + INDEX_NAME);

            if(isSuccess(response)) {
                log.info("Elasticsearch index deleted: " + INDEX_NAME);
            } else {
                log.info("Elasticsearch index not deleted. Maybe because it did not exist?");
            }
        } catch (IOException e) {
            log.info("Error deleting elasticsearch index: " + INDEX_NAME);
            e.printStackTrace();
        }
    }

    private boolean indexExists() {
        log.info("Checking if elasticsearch index exists: " + INDEX_NAME);
        try {
            Response response = esRestClient.performRequest("HEAD", "/" + INDEX_NAME);
            if(response.getStatusLine().getStatusCode() == 404) {
                log.info("Elasticsearch index does not exist: " + INDEX_NAME);
                return false;
            }
        } catch (IOException e) {
            log.info("Error checking if elasticsearch index exists: " + INDEX_NAME);
            return true;
        }
        log.info("Elasticsearch index exists: " + INDEX_NAME);
        return true;
    }

    private boolean createIndex() {
        try {
            HttpEntity entity = createHttpEntity(CREATE_INDEX_FILENAME);
            log.info("Trying to create elasticsearch index: " + INDEX_NAME);
            Response response = esRestClient.performRequest("PUT", "/" + INDEX_NAME, singletonMap("pretty", "true"), entity);

            if (isSuccess(response)) {
                log.info("elasticsearch index successfully created: " + INDEX_NAME);
                return true;
            } else {
                log.error("Unable to create elasticsearch index: " + INDEX_NAME);
            }
        } catch (IOException e) {
            log.error("Unable to create elasticsearch index: " + INDEX_NAME);
            e.printStackTrace();
        }
        return false;
    }

    private boolean createMapping() {
        try {
            HttpEntity entity = createHttpEntity(CREATE_MAPPINGS_FILENAME);
            log.info("Trying to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);
            Response response = esRestClient.performRequest("PUT", "/" + INDEX_NAME + "/_mapping/" + INDEX_MAPPING_TYPE, singletonMap("pretty", "true"), entity);
            if (isSuccess(response)) {
                log.info("elasticsearch index mapping type successfully created: " + INDEX_MAPPING_TYPE);
                return true;
            } else {
                log.error("Unable to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);
                return false;
            }
        } catch (IOException e) {
            log.error("Unable to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);
            e.printStackTrace();
        }
        return false;
    }

    private @NotNull String createBulkIndexMetaAndSource(@NotNull Concept concept) {
        return "{\"index\":{\"_index\": \"" + INDEX_NAME + "\", \"_type\": \"" + INDEX_MAPPING_TYPE + "\", \"_id\":\"" + concept.getDocumentId() + "\"}}\n" + concept.toElasticSearchDocument() + "\n";
    }

    private @NotNull String createBulkDeleteMeta(@NotNull String documentId) {
        return "{\"delete\":{\"_index\": \"" + INDEX_NAME + "\", \"_type\": \"" + INDEX_MAPPING_TYPE + "\", \"_id\":\"" + documentId + "\"}}\n";
    }

    private void bulkUpdateAndDeleteDocumentsToIndex(@NotNull List<Concept> updateConcepts,
                                                     @NotNull List<String> deleteDocumentIds,
                                                     boolean waitForRefresh) {

        if (updateConcepts.size() == 0 && deleteDocumentIds.size() == 0) {
            return; // nothing to do
        }

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html

        String index = updateConcepts.stream().map(this::createBulkIndexMetaAndSource).collect(Collectors.joining("\n"));
        String delete = deleteDocumentIds.stream().map(this::createBulkDeleteMeta).collect(Collectors.joining("\n"));
        HttpEntity entity = new NStringEntity(index + delete, ContentType.create("application/x-ndjson", StandardCharsets.UTF_8));
        Map<String, String> params = new HashMap<>();

        params.put("pretty", "true");

        if (waitForRefresh) {
            params.put("refresh", "wait_for");
        }

        try {
            Response response = esRestClient.performRequest("POST",  "/_bulk", params, entity);

            if (isSuccess(response)) {
                log.info("Successfully added/updated documents to elasticsearch index: " + updateConcepts.size());
                log.info("Successfully deleted documents from elasticsearch index: " + deleteDocumentIds.size());
            }
        } catch (IOException e) {
            log.error("Unable to add or update document to elasticsearch index: " + updateConcepts.size());
            log.error("Unable to delete document from elasticsearch index: " + deleteDocumentIds.size());
            e.printStackTrace();
        }
    }

    private void deleteDocumentsFromIndexByGraphId(@NotNull String graphId) {
        try {
            HttpEntity body = new NStringEntity("{\"query\": { \"match\": {\"vocabulary.id\": \"" + graphId + "\"}}}", ContentType.APPLICATION_JSON);
            Response response = esRestClient.performRequest("POST", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/_delete_by_query", emptyMap(), body);

            if (isSuccess(response)) {
                log.info(responseContentAsString(response));
                log.info("Successfully deleted documents from elasticsearch index from graph: " + graphId);
            } else {
                log.error("Unable to delete documents from elasticsearch index");
            }
        } catch (IOException e) {
            log.error("Unable to delete documents from elasticsearch index");
            e.printStackTrace();
        }
    }

    public void deleteAllDocumentsFromIndex() {
        try {
            HttpEntity body = new NStringEntity("{\"query\": { \"match_all\": {}}}", ContentType.APPLICATION_JSON);
            Response response = esRestClient.performRequest("POST", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/_delete_by_query", emptyMap(), body);

            if (isSuccess(response)) {
                log.info(responseContentAsString(response));
                log.info("Successfully deleted all documents from elasticsearch index");
            } else {
                log.error("Unable to delete documents from elasticsearch index");
            }
        } catch (IOException e) {
            log.error("Unable to delete documents from elasticsearch index");
            e.printStackTrace();
        }
    }

    private @Nullable Concept getConceptFromIndex(@NotNull String documentId) {
        try {
            Response response = esRestClient.performRequest("GET", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/" + documentId + "/_source");

            if (isSuccess(response)) {
                return Concept.createFromIndex(responseContentAsJson(response).getAsJsonObject());
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isSuccess(Response response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 400;
    }

    private @NotNull JsonElement responseContentAsJson(@NotNull Response response) {
        return jsonParser.parse(responseContentAsString(response));
    }

    private static @NotNull String responseContentAsString(@NotNull Response response) {
        try (InputStream is = response.getEntity().getContent()) {
            return new BufferedReader(new InputStreamReader(is)).lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull HttpEntity createHttpEntity(@NotNull String classPathResourceJsonFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(classPathResourceJsonFile);
        InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String resourceJsonAsString = jsonParser.parse(reader).toString();
        return new NStringEntity(resourceJsonAsString, ContentType.APPLICATION_JSON);
    }

    @PreDestroy
    private void destroy() {
        try {
            log.info("Closing rest client");
            this.esRestClient.close();
        } catch(IOException e) {
            log.error("Unable to close rest client");
            e.printStackTrace();
        }
    }
}
