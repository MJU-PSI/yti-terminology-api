package fi.csc.termed.search;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;

@Service
public class ElasticSearchService {

    private RestClient esRestClient;
    private RestClient apiRestClient;
    private JSONParser jsonParser;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${search.host.scheme}")
    private String searchHostScheme = "http";

    @Value("${search.host.url}")
    private String searchHostUrl = "localhost";

    @Value("${search.host.port}")
    private int searchHostPort = 9200;

    @Value("${api.host.url}")
    private String apiHostUrl = "termed.csc.fi";

    @Value("${search.index.file}")
    private String createIndexFilename = "create_index_default.json";

    @Value("${search.index.mapping.file}")
    private String createMappingsFilename = "create_mappings.json";

    @Value("${search.index.name}")
    private String indexName = "concepts";

    @Value("${search.index.mapping.type}")
    private String indexMappingType = "concept";

    @Value("${search.index.deleteOnAppRestart}")
    private boolean deleteOnAppRestart = false;

    @Value("${search.index.document.importUrl}")
    private String importUrl = "http://termed.csc.fi/api/ext.json?max=-1&typeId=Concept&select.properties=prefLabel&select.referrers=&select.references=prefLabelXl&select.audit=true";

    @Value("${api.user}")
    private String apiUser = "admin";

    @Value("${api.pw}")
    private String apiPw = "admin";

    public ElasticSearchService() {

        this.jsonParser = new JSONParser();
        this.apiRestClient = RestClient.builder(
                new HttpHost(apiHostUrl)).build();
        this.esRestClient = RestClient.builder(
                new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();

        if(deleteOnAppRestart) {
            deleteIndex();
        }

        if(!indexExists()) {
            if(createIndex()) {
                if(createMapping()) {
                    doBatchDocumentImport();
                }
            }
        }
    }

    public void updateIndex(Notification notification) {
        // TODO: Extract info about operation and id, fetch data from termed api and then call addOrUpdateDocumentToIndex or deleteDocumentFromIndex
        log.info("Notification received:\n");
        log.info(notification.toString());
    }

    private void doBatchDocumentImport() {

        try {
            String encAuth = Base64.getEncoder().encodeToString((apiUser + ":" + apiPw).getBytes());
            BasicHeader auth = new BasicHeader("Authorization", "Basic " + encAuth);
            Response resp = apiRestClient.performRequest("GET", importUrl, auth);
            if (resp.getStatusLine().getStatusCode() == 200) {
                String respStr = EntityUtils.toString(resp.getEntity());
                JSONArray docs = (JSONArray) jsonParser.parse(respStr);

                Iterator it = docs.iterator();
                int added = 0;
                while (it.hasNext()) {
                    JSONObject doc = (JSONObject) it.next();
                    if(doc != null && doc.get("id") != null) {
                        addOrUpdateDocumentToIndex(String.valueOf(doc.get("id")), doc.toJSONString());
                        added++;
                    }
                }
                log.info("Added " + added + " document to elasticsearch index: " + indexName);
            } else {
                log.warn("Fetching documents for index failed with code: " + resp.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            log.error("Batch document import failed");
            e.printStackTrace();
        } catch (ParseException e) {
            log.error("Batch document import failed");
            e.printStackTrace();
        }
    }

    private void deleteIndex() {

        log.info("Deleting elasticsearch index: " + indexName);
        try {
            Response resp = esRestClient.performRequest("DELETE", "/" + indexName);
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 300) {
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

    private String getJsonFileAsString(String filename) {

        Object obj = null;
        try {
            return jsonParser.parse(new FileReader(getClass().getClassLoader().getResource(filename).getFile())).toString();
        } catch (IOException e) {
            log.error("Unable to read file: " + filename);
            return null;
        } catch (ParseException e) {
            log.error("Unable to parse file as JSON: " + filename);
            return null;
        }
    }

    private boolean createIndex() {

        HttpEntity entity = new NStringEntity(getJsonFileAsString(createIndexFilename), ContentType.APPLICATION_JSON);
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

        HttpEntity entity = new NStringEntity(getJsonFileAsString(createMappingsFilename), ContentType.APPLICATION_JSON);
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
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 300) {
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
            if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 300) {
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
