package fi.csc.termed.search.service;

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
import java.util.Collections;

@Service
public class ElasticSearchService {

    @Value("${search.host.scheme}")
    private String SEARCH_HOST_HTTP_SCHEME;

    @Value("${search.host.url}")
    private String SEARCH_HOST_URL;

    @Value("${search.host.port}")
    private int SEARCH_HOST_PORT;

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
                new HttpHost(SEARCH_HOST_URL, SEARCH_HOST_PORT, SEARCH_HOST_HTTP_SCHEME)).build();

        if(DELETE_INDEX_ON_APP_RESTART) {
            deleteIndex();
        }

        if(!indexExists()) {
            if(createIndex()) {
                if(createMapping()) {
                    this.termedApiService.fetchAllNodes().forEach((id, doc) -> {
                        if(!addOrUpdateDocumentToIndex(id, doc)) {
                            log.error("Failed to index document: " + doc);
                            log.info("Exiting");
                            application.context.close();
                            System.exit(1);
                        }
                    });
                }
            }
        }
    }

    public void updateIndex(Notification notification) {
        String nodeId = notification.getBody().getNode().getId();
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

                String nodeData = termedApiService.fetchNode(graphId, nodeId);
                if(nodeData != null) {
                    log.debug(nodeData);
                    log.debug(nodeId);
                    addOrUpdateDocumentToIndex(nodeId, nodeData);
                }
                break;
            case NodeDeletedEvent:
                if(!deleteDocumentFromIndex(nodeId)) {
                    log.error("Unable to delete document from index or nodeId is not supplied: " + nodeId);
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
        }
    }
}
