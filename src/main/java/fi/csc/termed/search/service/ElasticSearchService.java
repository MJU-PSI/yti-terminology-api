package fi.csc.termed.search.service;

import fi.csc.termed.search.Application;
import fi.csc.termed.search.Notification;
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
    private String searchHostScheme;

    @Value("${search.host.url}")
    private String searchHostUrl;

    @Value("${search.host.port}")
    private int searchHostPort;

    @Value("${search.index.file}")
    private String createIndexFilename;

    @Value("${search.index.mapping.file}")
    private String createMappingsFilename;

    @Value("${search.index.name}")
    private String indexName;

    @Value("${search.index.mapping.type}")
    private String indexMappingType;

    @Value("${search.index.deleteIndexOnAppRestart}")
    private boolean deleteIndexOnAppRestart;

    private RestClient esRestClient;
    private TermedApiService termedApiService;
    private JsonParserService jsonParserService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ElasticSearchService(TermedApiService termedApiService, JsonParserService jsonParserService) {
        this.termedApiService = termedApiService;
        this.jsonParserService = jsonParserService;
    }

    public void initIndex() {
        this.esRestClient = RestClient.builder(
                new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();

        if(deleteIndexOnAppRestart) {
            deleteIndex();
        }

        if(!indexExists()) {
            if(createIndex()) {
                if(createMapping()) {
                    this.termedApiService.fetchConceptDocuments().forEach((id, doc) -> {
                        if(!addOrUpdateDocumentToIndex(id, doc)) {
                            log.error("Failed to index document: " + doc);
                            log.info("Exiting");
                            Application.context.close();
                            System.exit(1);
                        }
                    });
                }
            }
        }
    }

    public void updateIndex(Notification notification) {
        // TODO: Extract info about operation and id, fetch data from termed api and then call addOrUpdateDocumentToIndex or deleteDocumentFromIndex
        log.info("Notification received:\n");
        log.info(notification.toString());
    }


    private void deleteIndex() {
        log.info("Deleting elasticsearch index: " + indexName);
        try {
            Response resp = esRestClient.performRequest("DELETE", "/" + indexName);
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                log.info("Elasticsearch index deleted: " + indexName);
            } else {
                log.info("Elasticsearch index not deleted. Maybe because it did not exist?");
            }
        } catch (IOException e) {
            log.info("Error deleting elasticsearch index: " + indexName);
        }
    }

    private boolean indexExists() {
        log.info("Checking if elasticsearch index exists: " + indexName);
        try {
            Response resp = esRestClient.performRequest("HEAD", "/" + indexName);
            if(resp.getStatusLine().getStatusCode() == 404) {
                log.info("Elasticsearch index does not exist: " + indexName);
                return false;
            }
        } catch (IOException e) {
            log.info("Error checking if elasticsearch index exists: " + indexName);
        }
        log.info("Elasticsearch index exists: " + indexName);
        return true;
    }

    private boolean createIndex() {
        HttpEntity entity = new NStringEntity(jsonParserService.getJsonFileAsString(createIndexFilename), ContentType.APPLICATION_JSON);
        try {
            log.info("Trying to create elasticsearch index: " + indexName);
            esRestClient.performRequest("PUT", "/" + indexName, Collections.singletonMap("pretty", "true"), entity);
            log.info("elasticsearch index successfully created: " + indexName);
            return true;
        } catch (IOException e) {
            log.error("Unable to create elasticsearch index: " + indexName);
        }
        return false;
    }

    private boolean createMapping() {
        HttpEntity entity = new NStringEntity(jsonParserService.getJsonFileAsString(createMappingsFilename), ContentType.APPLICATION_JSON);
        try {
            log.info("Trying to create elasticsearch index mapping type: " + indexMappingType);
            esRestClient.performRequest("PUT", "/" + indexName + "/_mapping/" + indexMappingType, Collections.singletonMap("pretty", "true"), entity);
            log.info("elasticsearch index mapping type successfully created: " + indexMappingType);
            return true;
        } catch (IOException e) {
            log.error("Unable to create elasticsearch index mapping type: " + indexMappingType);
        }
        return false;
    }

    private boolean addOrUpdateDocumentToIndex(String documentId, String document) {
        HttpEntity entity = new NStringEntity(document, ContentType.APPLICATION_JSON);
        try {
            Response resp = esRestClient.performRequest("PUT", "/" + indexName + "/" + indexMappingType + "/" + documentId, Collections.singletonMap("pretty", "true"), entity);
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
        try {
            Response resp = esRestClient.performRequest("DELETE", "/" + indexName + "/" + indexMappingType + "/" + documentId);
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
                log.info("Successfully deleted document from elasticsearch index: " + documentId);
                return true;
            }
        } catch (IOException e) {
            log.error("Unable to delete document from elasticsearch index: " + documentId);
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
