package fi.csc.termed.search.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.csc.termed.search.Application;
import fi.csc.termed.search.domain.Notification;
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
import java.util.Collections;
import java.util.List;
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
    private TermedApiService termedApiService;
    private JsonParserService jsonParserService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ElasticSearchService(Application application, TermedApiService termedApiService, JsonParserService jsonParserService) {
        this.application = application;
        this.termedApiService = termedApiService;
        this.jsonParserService = jsonParserService;
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
                    indexListOfConcepts(termedApiService.fetchAllConcepts());
                }
            }
        }
    }

    public void updateIndexAfterConceptEvent(Notification notification) {
        String conceptId = notification.getBody().getNode().getId();

        switch (notification.getType()) {
            case NodeSavedEvent:
                String vocabularyId = notification.getBody().getNode().getType().getGraph().getId();
                JsonObject conceptJsonObj = termedApiService.fetchConcept(vocabularyId, conceptId);
                JsonElement vocabularyObj = termedApiService.fetchVocabularyForConcept(vocabularyId, false);
                if(!indexOneConcept(conceptJsonObj, vocabularyObj)) {
                    log.error("Failed to (re)index document: " + conceptId);
                }
                break;
            case NodeDeletedEvent:
                if(!deleteDocumentFromIndex(conceptId)) {
                    log.error("Unable to delete document from index or conceptId is not supplied: " + conceptId);
                }
                break;
        }
    }

    public void updateIndexAfterVocabularyEvent(Notification notification) {
        String vocabularyId = notification.getBody().getNode().getType().getGraph().getId();
        if(vocabularyId != null) {
            deleteDocumentsFromIndexByVocabularyId(vocabularyId);

            switch (notification.getType()) {
                case NodeSavedEvent:
                    indexListOfConcepts(termedApiService.fetchAllConceptsInVocabulary(vocabularyId));
                    break;
            }
        } else {
            log.error("Unable to update index after vocabulary event");
        }
    }

    private boolean indexOneConcept(JsonObject conceptJsonObj, JsonElement vocabularyJsonObj) {
        if (conceptJsonObj.get("id") != null) {
            String conceptId = conceptJsonObj.get("id").getAsString();
            JsonObject indexConcept = jsonParserService.transformApiConceptToIndexConcept(conceptJsonObj, vocabularyJsonObj);

            if (indexConcept != null) {
                if (!addOrUpdateDocumentToIndex(conceptId, indexConcept.toString())) {
                    log.error("Failed to index document: " + indexConcept.toString());
                }
                return true;
            } else {
                log.error("Failed to index document: " + indexConcept.toString());
            }
        } else {
            log.error("Failed to index document");
        }
        return false;
    }

    private void indexListOfConcepts(List<JsonObject> conceptJsonObjects) {
        if(conceptJsonObjects != null) {
            conceptJsonObjects.forEach(conceptJsonObj -> {
                JsonElement vocabularyObj = termedApiService.fetchVocabularyForConcept(jsonParserService.getVocabularyIdForConcept(conceptJsonObj), true);
                indexOneConcept(conceptJsonObj, vocabularyObj);
            });
            termedApiService.invalidateVocabularyCache();
            log.info("Finished indexing documents");
        } else {
            log.warn("Nothing to index");
        }
    }

    private void batchDeleteFromIndex(List<String> conceptIds) {
        conceptIds.forEach(conceptId -> {
            if(!deleteDocumentFromIndex(conceptId)) {
                log.error("Failed to delete concept from index. " + conceptId);
            }
        });
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
        HttpEntity entity = new NStringEntity(jsonParserService.getJsonFileAsString(CREATE_INDEX_FILENAME), ContentType.APPLICATION_JSON);
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
        HttpEntity entity = new NStringEntity(jsonParserService.getJsonFileAsString(CREATE_MAPPINGS_FILENAME), ContentType.APPLICATION_JSON);
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

    private boolean addOrUpdateDocumentToIndex(String documentId, String document) {
        HttpEntity entity = new NStringEntity(document, ContentType.APPLICATION_JSON);
        try {
            Response resp = esRestClient.performRequest("PUT", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/" + documentId, Collections.singletonMap("pretty", "true"), entity);
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                log.info("Successfully added/updated document to elasticsearch index: " + documentId);
                return true;
            }
        } catch (IOException e) {
            log.error("Unable to add or update document to elasticsearch index: " + documentId);
            e.printStackTrace();
        }
        return false;
    }

    private boolean deleteDocumentFromIndex(String documentId) {
        if(documentId != null) {
            try {
                Response resp = esRestClient.performRequest("DELETE", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/" + documentId);
                if (resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                    log.info("Successfully deleted document from elasticsearch index: " + documentId);
                    return true;
                } else {
                    log.error("Unable to delete document from elasticsearch index: " + documentId);
                }
            } catch (IOException e) {
                log.error("Unable to delete document from elasticsearch index: " + documentId);
                e.printStackTrace();
            }
        }
        return false;
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
