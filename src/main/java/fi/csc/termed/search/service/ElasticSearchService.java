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
import java.io.IOException;
import java.util.*;

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

    private static Map<String, List<String>> vocabularyConceptCache = new HashMap<>();

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
                    termedApiService.fetchAllConcepts().forEach(conceptJsonObj -> {
                        if(conceptJsonObj.get("id") != null) {
                            String conceptId = conceptJsonObj.get("id").getAsString();
                            String vocabularyId = jsonParserService.getVocabularyIdForConcept(conceptJsonObj);
                            JsonElement vocabularyObj = termedApiService.fetchVocabularyForConcept(vocabularyId);
                            JsonObject indexConcept = jsonParserService.transformApiConceptToIndexConcept(conceptJsonObj, vocabularyObj);

                            if(indexConcept != null) {
                                if (addOrUpdateDocumentToIndex(conceptId, indexConcept.toString())) {
                                    if(vocabularyConceptCache.get(vocabularyId) == null) {
                                        vocabularyConceptCache.put(vocabularyId, new ArrayList<>());
                                    }
                                    vocabularyConceptCache.get(vocabularyId).add(conceptId);
                                } else {
                                    log.error("Failed to index document: " + indexConcept.toString());
                                    log.info("Exiting");
                                    application.context.close();
                                    System.exit(1);
                                }
                            }
                        }
                    });
                    // After batch import do not anymore use cached vocabulary data
                    termedApiService.invalidateVocabularyCache();
                }
            }
        }
    }

    public void updateIndex(Notification notification) {
        String conceptId = notification.getBody().getNode().getId();
        String graphId = notification.getBody().getNode().getType().getGraph().getId();

        switch (notification.getType()) {
            case NodeSavedEvent:

                // TODO: REMOVE THIS AFTER TERMED-API UPDATES API INDEX SYNCHRONOUSLY
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // TODO: END

                JsonObject conceptJsonObj = termedApiService.fetchConcept(graphId, conceptId);
                String vocabularyId = jsonParserService.getVocabularyIdForConcept(conceptJsonObj);
                JsonElement vocabularyObj = termedApiService.fetchVocabularyForConcept(vocabularyId);
                if(conceptJsonObj != null) {
                    JsonObject indexConcept = jsonParserService.transformApiConceptToIndexConcept(conceptJsonObj, vocabularyObj);
                    if(indexConcept != null) {
                        if (!addOrUpdateDocumentToIndex(conceptId, indexConcept.toString())) {
                            log.error("Failed to (re)index document: " + indexConcept.toString());
                        }
                    }
                }
                break;
            case NodeDeletedEvent:
                if(!deleteDocumentFromIndex(conceptId)) {
                    log.error("Unable to delete document from index or conceptId is not supplied: " + conceptId);
                }
                break;
        }
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
