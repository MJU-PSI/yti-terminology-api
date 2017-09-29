package fi.csc.termed.api.index;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
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
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

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
    private final TermedService termedApiService;
    private final JsonParser jsonParser = new JsonParser();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ElasticSearchService(TermedService termedApiService,
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

    public void reindex() {
        log.info("Starting reindexing task..");
        this.deleteAllDocumentsFromIndex();
        this.doFullIndexing();
        log.info("Finished reindexing!");
    }

    private void doFullIndexing() {
        termedApiService.fetchAllAvailableGraphIds().forEach(graphId -> reindexGraph(graphId, false));
    }

    void updateIndexAfterUpdate(@NotNull AffectedNodes nodes) {

        int fullReindexNodeCountThreshold = 20;

        if (nodes.hasVocabulary() || nodes.getConceptsIds().size() > fullReindexNodeCountThreshold) {
            reindexGraph(nodes.getGraphId(), true);
        } else {

            List<Concept> updatedConcepts = termedApiService.getConcepts(nodes.getGraphId(), nodes.getConceptsIds());
            List<Concept> conceptsBeforeUpdate = getConceptsFromIndex(nodes.getGraphId(), nodes.getConceptsIds());
            List<Concept> possiblyUpdatedConcepts = termedApiService.getConcepts(nodes.getGraphId(), broaderAndNarrowerIds(asList(updatedConcepts, conceptsBeforeUpdate)));
            List<Concept> updateToIndex = Stream.concat(updatedConcepts.stream(), possiblyUpdatedConcepts.stream()).collect(toList());

            bulkUpdateAndDeleteDocumentsToIndex(nodes.getGraphId(), updateToIndex, emptyList(), true);
        }
    }

    void updateIndexAfterDelete(@NotNull AffectedNodes nodes) {

        if (nodes.hasVocabulary()) {
            deleteDocumentsFromIndexByGraphId(nodes.getGraphId());
        } else {

            List<Concept> conceptsBeforeDelete = getConceptsFromIndex(nodes.getGraphId(), nodes.getConceptsIds());
            List<Concept> possiblyUpdatedConcepts = termedApiService.getConcepts(nodes.getGraphId(), broaderAndNarrowerIds(singletonList(conceptsBeforeDelete)));

            bulkUpdateAndDeleteDocumentsToIndex(nodes.getGraphId(), possiblyUpdatedConcepts, nodes.getConceptsIds(), true);
        }
    }

    private static @NotNull Set<String> broaderAndNarrowerIds(@NotNull List<List<Concept>> concepts) {

        return concepts.stream()
                .flatMap(Collection::stream)
                .flatMap(concept -> Stream.concat(concept.getBroaderIds().stream(), concept.getNarrowerIds().stream()))
                .collect(Collectors.toSet());
    }

    private void reindexGraph(@NotNull String graphId, boolean waitForRefresh) {
        log.info("Trying to index concepts of graph " + graphId);

        List<Concept> concepts = termedApiService.getAllConceptsForGraph(graphId);
        bulkUpdateAndDeleteDocumentsToIndex(graphId, concepts, emptyList(), waitForRefresh);

        log.info("Indexed " + concepts.size() + " concepts");
    }

    private void deleteIndex() {
        log.info("Deleting elasticsearch index: " + INDEX_NAME);

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("DELETE", "/" + INDEX_NAME));

        if (isSuccess(response)) {
            log.info("Elasticsearch index deleted: " + INDEX_NAME);
        } else {
            log.info("Elasticsearch index not deleted. Maybe because it did not exist?");
        }
    }

    private boolean indexExists() {
        log.info("Checking if elasticsearch index exists: " + INDEX_NAME);

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("HEAD", "/" + INDEX_NAME));

        if (response.getStatusLine().getStatusCode() == 404) {
            log.info("Elasticsearch index does not exist: " + INDEX_NAME);
            return false;
        } else {
            return true;
        }
    }

    private boolean createIndex() {

        HttpEntity entity = createHttpEntity(CREATE_INDEX_FILENAME);
        log.info("Trying to create elasticsearch index: " + INDEX_NAME);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("PUT", "/" + INDEX_NAME, singletonMap("pretty", "true"), entity));

        if (isSuccess(response)) {
            log.info("elasticsearch index successfully created: " + INDEX_NAME);
            return true;
        } else {
            log.warn("Unable to create elasticsearch index: " + INDEX_NAME);
            return false;
        }
    }

    private boolean createMapping() {

        HttpEntity entity = createHttpEntity(CREATE_MAPPINGS_FILENAME);
        log.info("Trying to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("PUT", "/" + INDEX_NAME + "/_mapping/" + INDEX_MAPPING_TYPE, singletonMap("pretty", "true"), entity));

        if (isSuccess(response)) {
            log.info("elasticsearch index mapping type successfully created: " + INDEX_MAPPING_TYPE);
            return true;
        } else {
            log.warn("Unable to create elasticsearch index mapping type: " + INDEX_MAPPING_TYPE);
            return false;
        }
    }

    private @NotNull String createBulkIndexMetaAndSource(@NotNull Concept concept) {
        return "{\"index\":{\"_index\": \"" + INDEX_NAME + "\", \"_type\": \"" + INDEX_MAPPING_TYPE + "\", \"_id\":\"" + concept.getDocumentId() + "\"}}\n" + concept.toElasticSearchDocument() + "\n";
    }

    private @NotNull String createBulkDeleteMeta(@NotNull String graphId, @NotNull String conceptId) {
        return "{\"delete\":{\"_index\": \"" + INDEX_NAME + "\", \"_type\": \"" + INDEX_MAPPING_TYPE + "\", \"_id\":\"" + Concept.formDocumentId(graphId, conceptId) + "\"}}\n";
    }

    private void bulkUpdateAndDeleteDocumentsToIndex(@NotNull String graphId,
                                                     @NotNull List<Concept> updateConcepts,
                                                     @NotNull List<String> deleteConceptsIds,
                                                     boolean waitForRefresh) {

        if (updateConcepts.size() == 0 && deleteConceptsIds.size() == 0) {
            return; // nothing to do
        }

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html

        String index = updateConcepts.stream().map(this::createBulkIndexMetaAndSource).collect(Collectors.joining("\n"));
        String delete = deleteConceptsIds.stream().map(id -> createBulkDeleteMeta(graphId, id)).collect(Collectors.joining("\n"));
        HttpEntity entity = new NStringEntity(index + delete, ContentType.create("application/x-ndjson", StandardCharsets.UTF_8));
        Map<String, String> params = new HashMap<>();

        params.put("pretty", "true");

        if (waitForRefresh) {
            params.put("refresh", "wait_for");
        }

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("POST",  "/_bulk", params, entity));

        if (isSuccess(response)) {
            log.info("Successfully added/updated documents to elasticsearch index: " + updateConcepts.size());
            log.info("Successfully deleted documents from elasticsearch index: " + deleteConceptsIds.size());
        } else {
            log.warn("Unable to add or update document to elasticsearch index: " + updateConcepts.size());
            log.warn("Unable to delete document from elasticsearch index: " + deleteConceptsIds.size());
        }
    }

    private void deleteDocumentsFromIndexByGraphId(@NotNull String graphId) {

        HttpEntity body = new NStringEntity("{\"query\": { \"match\": {\"vocabulary.id\": \"" + graphId + "\"}}}", ContentType.APPLICATION_JSON);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("POST", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/_delete_by_query", emptyMap(), body));

        if (isSuccess(response)) {
            log.info(responseContentAsString(response));
            log.info("Successfully deleted documents from elasticsearch index from graph: " + graphId);
        } else {
            log.warn("Unable to delete documents from elasticsearch index");
        }
    }

    private void deleteAllDocumentsFromIndex() {

        HttpEntity body = new NStringEntity("{\"query\": { \"match_all\": {}}}", ContentType.APPLICATION_JSON);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("POST", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/_delete_by_query", emptyMap(), body));

        if (isSuccess(response)) {
            log.info(responseContentAsString(response));
            log.info("Successfully deleted all documents from elasticsearch index");
        } else {
            log.warn("Unable to delete documents from elasticsearch index");
        }
    }

    private @Nullable Concept getConceptFromIndex(@NotNull String graphId, @NotNull String conceptId) {

        String documentId = Concept.formDocumentId(graphId, conceptId);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("GET", "/" + INDEX_NAME + "/" + INDEX_MAPPING_TYPE + "/" + urlEncode(documentId) + "/_source"));

        if (isSuccess(response)) {
            return Concept.createFromIndex(responseContentAsJson(response).getAsJsonObject());
        } else {
            return null;
        }
    }

    private @NotNull List<Concept> getConceptsFromIndex(@NotNull String graphId, @NotNull Collection<String> conceptIds) {

        // TODO inefficient implementation
        return conceptIds.stream()
                .map(conceptId -> this.getConceptFromIndex(graphId, conceptId))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private @NotNull Response alsoUnsuccessful(@NotNull ResponseSupplier supplier) {
        try {
            return supplier.get();
        } catch (ResponseException e) {
            return e.getResponse();
        } catch (IOException e) {
            throw new ElasticEndpointException(e);
        }
    }

    private static @NotNull String urlEncode(@NotNull String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private interface ResponseSupplier {
        @NotNull Response get() throws IOException;
    }

    private boolean isSuccess(@NotNull Response response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 400;
    }

    private @NotNull JsonElement responseContentAsJson(@NotNull Response response) {
        return jsonParser.parse(responseContentAsString(response));
    }

    private static @NotNull String responseContentAsString(@NotNull Response response) {
        try (InputStream is = response.getEntity().getContent()) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull HttpEntity createHttpEntity(@NotNull String classPathResourceJsonFile) {

        ClassPathResource resource = new ClassPathResource(classPathResourceJsonFile);

        try (InputStream is = resource.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            String resourceJsonAsString = jsonParser.parse(reader).toString();
            return new NStringEntity(resourceJsonAsString, ContentType.APPLICATION_JSON);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void destroy() {
        try {
            log.info("Closing rest client");
            this.esRestClient.close();
        } catch (IOException e) {
            log.warn("Unable to close rest client");
            throw new RuntimeException(e);
        }
    }
}
