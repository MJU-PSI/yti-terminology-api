package fi.csc.termed.search.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.csc.termed.search.Application;
import fi.csc.termed.search.domain.Notification;
import fi.csc.termed.search.service.api.TermedApiService;
import fi.csc.termed.search.service.api.TermedExtApiService;
import fi.csc.termed.search.service.json.JsonTools;
import fi.csc.termed.search.service.json.TermedExtJsonService;
import fi.csc.termed.search.service.json.TermedJsonService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticSearchService {

    @Value("${search.host.url}")
    private String SEARCH_HOST_URL;

    @Value("${search.host.port}")
    private int SEARCH_HOST_PORT;

    @Value("${search.host.scheme}")
    private String SEARCH_HOST_SCHEME;

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

    private Application application;
    private RestClient esRestClient;
    private TermedExtApiService termedExtApiService;
    private TermedApiService termedApiService;
    private TermedExtJsonService termedExtJsonService;
    private TermedJsonService termedJsonService;
    private JsonParser gsonParser;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ElasticSearchService(Application application, TermedApiService termedApiService, TermedExtApiService termedExtApiService, TermedJsonService termedJsonService, TermedExtJsonService termedExtJsonService) {
        this.application = application;
        this.termedApiService = termedApiService;
        this.termedExtApiService = termedExtApiService;
        this.termedJsonService = termedJsonService;
        this.termedExtJsonService = termedExtJsonService;
        this.gsonParser = new JsonParser();
    }

    public void initIndex() {
        this.esRestClient = RestClient.builder(
                new HttpHost(SEARCH_HOST_URL, SEARCH_HOST_PORT, SEARCH_HOST_SCHEME)).build();

        if(DELETE_INDEX_ON_APP_RESTART) {
            deleteIndex();
        }
        if(!indexExists()) {
            if(createIndex()) {
                if(createMapping()) {
                   doFullIndexing();
                }
            }
        }
    }

    public void doFullIndexing() {
        List<String> vocabularyIds = termedApiService.fetchAllAvailableVocabularyIds();
        for(String vocId : vocabularyIds) {
            indexListOfConceptsInVocabulary(vocId);
        }
    }

    public JsonElement getDocumentFromIndex(String documentId) {
        if(documentId != null) {
            try {
                Response resp = esRestClient.performRequest("GET", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/" + documentId + "/_source");
                if (resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
                    return gsonParser.parse(reader);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private List<JsonObject> conceptIdsToRefreshedIndexDocuments(String vocabularyId, JsonElement vocabularyObj, Collection<String> ids) {
        return ids.stream()
                .map(id -> termedExtApiService.fetchConcept(vocabularyId, id))
                .filter(Objects::nonNull)
                .map(concept -> termedExtJsonService.transformApiConceptToIndexConcept(concept, vocabularyObj))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateIndexAfterConceptEvent(Notification notification) {
        String conceptId = notification.getBody().getNode().getId();
        String vocabularyId = notification.getBody().getNode().getType().getGraph().getId();
        JsonElement previousIndexedConcept = getDocumentFromIndex(conceptId);
        List<String> previousBroaderIds = getDocumentRefIds(previousIndexedConcept, "broader");
        List<String> previousNarrowerIds = getDocumentRefIds(previousIndexedConcept, "narrower");
        JsonElement vocabularyObj = termedExtApiService.getVocabularyForIndexing(vocabularyId);

        switch (notification.getType()) {
            case NodeSavedEvent:
                JsonObject conceptJsonObj = termedExtApiService.fetchConcept(vocabularyId, conceptId);

                if (conceptJsonObj != null) {

                    // First reindex the saved concept

                    // Next reindex broader concepts in case the saved concept's broader concept list changed
                    // Basically each broader concept's hasNarrower index value needs to be revised

                    // First compare API's broader concepts and compare against the corresponding documents in index
                    // Case: A concept becomes some concept's child (index doc does not contain the new broader concept)

                    // Then the other way around: Compare index documents against API's broader concepts
                    // Case: A concept is removed from being some concept's child

                    // Narrower and same logic as above

                    HashSet<String> tryUpdateConceptIds = new HashSet<>();
                    tryUpdateConceptIds.addAll(previousBroaderIds);
                    tryUpdateConceptIds.addAll(termedExtJsonService.getBroaderIdsFromConcept(conceptJsonObj));
                    tryUpdateConceptIds.addAll(previousNarrowerIds);
                    tryUpdateConceptIds.addAll(termedExtJsonService.getNarrowerIdsFromConcept(conceptJsonObj));

                    List<JsonObject> possiblyUpdatedConcepts = new ArrayList<>(tryUpdateConceptIds.size() + 1);
                    possiblyUpdatedConcepts.add(termedExtJsonService.transformApiConceptToIndexConcept(conceptJsonObj, vocabularyObj));
                    possiblyUpdatedConcepts.addAll(conceptIdsToRefreshedIndexDocuments(vocabularyId, vocabularyObj, tryUpdateConceptIds));

                    bulkUpdateAndDeleteDocumentsToIndex(possiblyUpdatedConcepts, Collections.emptyList(), true);
                }
                break;
            case NodeDeletedEvent:

                HashSet<String> tryUpdateConceptIds = new HashSet<>();
                tryUpdateConceptIds.addAll(previousBroaderIds);
                tryUpdateConceptIds.addAll(previousNarrowerIds);

                List<JsonObject> possiblyUpdatedConcepts = conceptIdsToRefreshedIndexDocuments(vocabularyId, vocabularyObj, tryUpdateConceptIds);
                bulkUpdateAndDeleteDocumentsToIndex(possiblyUpdatedConcepts, Collections.singletonList(conceptId), true);
                break;
        }
    }

    private List<String> getDocumentRefIds(JsonElement documentJsonElem, String type) {
        List<String> output = new ArrayList<>();
        if(documentJsonElem != null && documentJsonElem.isJsonObject() && documentJsonElem.getAsJsonObject().get(type).isJsonArray()) {
            JsonArray docBroaderArray = documentJsonElem.getAsJsonObject().getAsJsonArray(type);
            Iterator<JsonElement> it = docBroaderArray.iterator();
            if (it.hasNext()) {
                JsonElement el = it.next();
                if(el.isJsonPrimitive()) {
                    output.add(el.getAsString());
                }

            }
        }
        return output;
    }

    public void updateIndexAfterVocabularyEvent(Notification notification) {
        String vocabularyId = notification.getBody().getNode().getType().getGraph().getId();
        if(vocabularyId != null) {
            deleteDocumentsFromIndexByVocabularyId(vocabularyId);

            switch (notification.getType()) {
                case NodeSavedEvent:
                    indexListOfConceptsInVocabulary(vocabularyId);
                    break;
            }
        } else {
            log.error("Unable to update index after vocabulary event");
        }
    }

    private void indexListOfConceptsInVocabulary(String vocabularyId) {
        log.info("Trying to index concepts of vocabulary " + vocabularyId);
        List<JsonObject> allNodesInVocabulary = termedApiService.fetchAllNodesInVocabulary(vocabularyId);
        Optional<JsonObject> vocabularyObj = termedApiService.getOneVocabulary(vocabularyId, allNodesInVocabulary);
        if(vocabularyObj.isPresent()) {
            Map<String, JsonObject> vocabularyConcepts = termedApiService.getAllConceptsFromNodes(allNodesInVocabulary);
            Map<String, JsonObject> vocabularyTerms = termedApiService.getAllTermsFromNodes(allNodesInVocabulary);
            if (vocabularyConcepts != null && vocabularyConcepts.size() > 0) {
                List<JsonObject> indexConcepts = termedJsonService.transformApiConceptsToIndexConcepts(termedApiService.transformVocabularyForIndexing(vocabularyObj.get()), vocabularyConcepts, vocabularyTerms);
                bulkUpdateAndDeleteDocumentsToIndex(indexConcepts, Collections.emptyList(), false);
            } else {
                log.warn("Nothing to index in the vocabulary");
            }
        }
        log.info("Finished indexing documents");
    }

    private void deleteIndex() {
        log.info("Deleting elasticsearch index: " + INDEX_NAME);
        try {
            Response resp = esRestClient.performRequest("DELETE", "/" + INDEX_NAME);
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
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
            Response resp = esRestClient.performRequest("HEAD", "/" + INDEX_NAME);
            if(resp.getStatusLine().getStatusCode() == 404) {
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
        HttpEntity entity = new NStringEntity(JsonTools.getJsonFileAsString(CREATE_INDEX_FILENAME), ContentType.APPLICATION_JSON);
        try {
            log.info("Trying to create elasticsearch index: " + INDEX_NAME);
            esRestClient.performRequest("PUT", "/" + INDEX_NAME, Collections.singletonMap("pretty", "true"), entity);
            log.info("elasticsearch index successfully created: " + INDEX_NAME);
            return true;
        } catch (IOException e) {
            log.error("Unable to create elasticsearch index: " + INDEX_NAME);
            e.printStackTrace();
        }
        return false;
    }

    private boolean createMapping() {
        HttpEntity entity = new NStringEntity(JsonTools.getJsonFileAsString(CREATE_MAPPINGS_FILENAME), ContentType.APPLICATION_JSON);
        try {
            log.info("Trying to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);
            esRestClient.performRequest("PUT", "/" + INDEX_NAME + "/_mapping/" + INDEX_MAPPING_TYPE, Collections.singletonMap("pretty", "true"), entity);
            log.info("elasticsearch index mapping type successfully created: " + INDEX_MAPPING_TYPE);
            return true;
        } catch (IOException e) {
            log.error("Unable to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);
            e.printStackTrace();
        }
        return false;
    }

    private String createBulkIndexMetaAndSource(JsonObject document) {
        String documentId = document.get("id").getAsString();
        return "{\"index\":{\"_index\": \"" + INDEX_NAME + "\", \"_type\": \"" + INDEX_MAPPING_TYPE + "\", \"_id\":\"" + documentId + "\"}}\n" + document.toString() + "\n";
    }

    private String createBulkDeleteMeta(String documentId) {
        return "{\"delete\":{\"_index\": \"" + INDEX_NAME + "\", \"_type\": \"" + INDEX_MAPPING_TYPE + "\", \"_id\":\"" + documentId + "\"}}\n";
    }

    private void bulkUpdateAndDeleteDocumentsToIndex(List<JsonObject> indexDocuments, List<String> deleteDocumentIds, boolean waitForRefresh) {

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html

        String index = indexDocuments.stream().map(this::createBulkIndexMetaAndSource).collect(Collectors.joining("\n"));
        String delete = deleteDocumentIds.stream().map(this::createBulkDeleteMeta).collect(Collectors.joining("\n"));
        HttpEntity entity = new NStringEntity(index + delete, ContentType.create("application/x-ndjson", StandardCharsets.UTF_8));
        Map<String, String> params = new HashMap<>();

        params.put("pretty", "true");

        if (waitForRefresh) {
            params.put("refresh", "wait_for");
        }

        try {
            Response resp = esRestClient.performRequest("POST",  "/_bulk", params, entity);

            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                log.info("Successfully added/updated documents to elasticsearch index: " + indexDocuments.size());
                log.info("Successfully deleted documents from elasticsearch index: " + deleteDocumentIds.size());
            }
        } catch (IOException e) {
            log.error("Unable to add or update document to elasticsearch index: " + indexDocuments.size());
            log.error("Unable to delete document from elasticsearch index: " + deleteDocumentIds.size());
            e.printStackTrace();
        }
    }

    private boolean deleteDocumentsFromIndexByVocabularyId(String vocabularyId) {
        if(vocabularyId != null) {
            try {
                HttpEntity body = new NStringEntity("{\"query\": { \"match\": {\"vocabulary.id\": \"" + vocabularyId + "\"}}}", ContentType.APPLICATION_JSON);
                Response resp = esRestClient.performRequest("POST", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/_delete_by_query", Collections.emptyMap(), body);
                if (resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
                    log.info(reader.lines().collect(Collectors.joining("\n")));
                    log.info("Successfully deleted documents from elasticsearch index from vocabulary: " + vocabularyId);
                    return true;
                } else {
                    log.error("Unable to delete documents from elasticsearch index");
                }
            } catch (IOException e) {
                log.error("Unable to delete documents from elasticsearch index");
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean deleteAllDocumentsFromIndex() {
        try {
            HttpEntity body = new NStringEntity("{\"query\": { \"match_all\": {}}}", ContentType.APPLICATION_JSON);
            Response resp = esRestClient.performRequest("POST", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/_delete_by_query", Collections.emptyMap(), body);
            if (resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
                log.info(reader.lines().collect(Collectors.joining("\n")));
                log.info("Successfully deleted all documents from elasticsearch index");
                return true;
            } else {
                log.error("Unable to delete documents from elasticsearch index");
            }
        } catch (IOException e) {
            log.error("Unable to delete documents from elasticsearch index");
            e.printStackTrace();
        }
        return false;
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
