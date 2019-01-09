package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.exception.ElasticEndpointException;
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

import fi.vm.yti.terminology.api.util.Parameters;
import static fi.vm.yti.terminology.api.util.ElasticRequestUtils.responseContentAsJson;
import static fi.vm.yti.terminology.api.util.ElasticRequestUtils.responseContentAsString;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

@Service
public class IndexElasticSearchService {

    private static final Logger log = LoggerFactory.getLogger(IndexElasticSearchService.class);

    private final RestClient esRestClient;
    private final String createIndexFilename;
    private final String createMappingsFilename;
    private final String indexName;
    private final String indexMappingType;
    private final boolean deleteIndexOnAppRestart;

    private final IndexTermedService termedApiService;
    private final ObjectMapper objectMapper;

    @Autowired
    public IndexElasticSearchService(@Value("${search.host.url}") String searchHostUrl,
            @Value("${search.host.port}") int searchHostPort, @Value("${search.host.scheme}") String searchHostScheme,
            @Value("${search.index.file}") String createIndexFilename,
            @Value("${search.index.mapping.file}") String createMappingsFilename,
            @Value("${search.index.name}") String indexName,
            @Value("${search.index.mapping.type}") String indexMappingType,
            @Value("${search.index.deleteIndexOnAppRestart}") boolean deleteIndexOnAppRestart,
            IndexTermedService termedApiService, ObjectMapper objectMapper) {
        this.createIndexFilename = createIndexFilename;
        this.createMappingsFilename = createMappingsFilename;
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.deleteIndexOnAppRestart = deleteIndexOnAppRestart;
        this.termedApiService = termedApiService;
        this.objectMapper = objectMapper;
        this.esRestClient = RestClient.builder(new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();
    }

    public void initIndex() {

        if (deleteIndexOnAppRestart) {
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
            List<Concept> possiblyUpdatedConcepts = termedApiService.getConcepts(nodes.getGraphId(),
                    broaderAndNarrowerIds(asList(updatedConcepts, conceptsBeforeUpdate)));
            List<Concept> updateToIndex = Stream.concat(updatedConcepts.stream(), possiblyUpdatedConcepts.stream())
                    .collect(toList());

            bulkUpdateAndDeleteDocumentsToIndex(nodes.getGraphId(), updateToIndex, emptyList(), true);
        }
    }

    void updateIndexAfterDelete(@NotNull AffectedNodes nodes) {

        if (nodes.hasVocabulary()) {
            deleteDocumentsFromIndexByGraphId(nodes.getGraphId());
        } else {

            List<Concept> conceptsBeforeDelete = getConceptsFromIndex(nodes.getGraphId(), nodes.getConceptsIds());
            List<Concept> possiblyUpdatedConcepts = termedApiService.getConcepts(nodes.getGraphId(),
                    broaderAndNarrowerIds(singletonList(conceptsBeforeDelete)));

            bulkUpdateAndDeleteDocumentsToIndex(nodes.getGraphId(), possiblyUpdatedConcepts, nodes.getConceptsIds(),
                    true);
        }
    }

    private static @NotNull Set<UUID> broaderAndNarrowerIds(@NotNull List<List<Concept>> concepts) {

        return concepts.stream().flatMap(Collection::stream)
                .flatMap(concept -> Stream.concat(concept.getBroaderIds().stream(), concept.getNarrowerIds().stream()))
                .collect(Collectors.toSet());
    }

    private void reindexGraph(@NotNull UUID graphId, boolean waitForRefresh) {
        log.info("Trying to index concepts of graph " + graphId);

        List<Concept> concepts = termedApiService.getAllConceptsForGraph(graphId);
        bulkUpdateAndDeleteDocumentsToIndex(graphId, concepts, emptyList(), waitForRefresh);

        log.info("Indexed " + concepts.size() + " concepts");
    }

    private void deleteIndex() {
        log.info("Deleting elasticsearch index: " + indexName);

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("DELETE", "/" + indexName));

        if (isSuccess(response)) {
            log.info("Elasticsearch index deleted: " + indexName);
        } else {
            log.info("Elasticsearch index not deleted. Maybe because it did not exist?");
        }
    }

    private boolean indexExists() {
        log.info("Checking if elasticsearch index exists: " + indexName);

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("HEAD", "/" + indexName));

        if (response.getStatusLine().getStatusCode() == 404) {
            log.info("Elasticsearch index does not exist: " + indexName);
            return false;
        } else {
            return true;
        }
    }

    private boolean createIndex() {

        HttpEntity entity = createHttpEntity(createIndexFilename);
        log.info("Trying to create elasticsearch index: " + indexName);
        Response response = alsoUnsuccessful(
                () -> esRestClient.performRequest("PUT", "/" + indexName, singletonMap("pretty", "true"), entity));

        if (isSuccess(response)) {
            log.info("elasticsearch index successfully created: " + indexName);
            return true;
        } else {
            log.warn("Unable to create elasticsearch index: " + indexName);
            return false;
        }
    }

    private boolean createMapping() {

        HttpEntity entity = createHttpEntity(createMappingsFilename);
        log.info("Trying to create elasticsearch index mapping type: " + indexMappingType);

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("PUT",
                "/" + indexName + "/_mapping/" + indexMappingType, singletonMap("pretty", "true"), entity));

        if (isSuccess(response)) {
            log.info("elasticsearch index mapping type successfully created: " + indexMappingType);
            return true;
        } else {
            log.warn("Unable to create elasticsearch index mapping type: " + indexMappingType);
            return false;
        }
    }

    private @NotNull String createBulkIndexMetaAndSource(@NotNull Concept concept) {
        return "{\"index\":{\"_index\": \"" + indexName + "\", \"_type\": \"" + indexMappingType + "\", \"_id\":\""
                + concept.getDocumentId() + "\"}}\n" + concept.toElasticSearchDocument(objectMapper) + "\n";
    }

    private @NotNull String createBulkDeleteMeta(@NotNull UUID graphId, @NotNull UUID conceptId) {
        return "{\"delete\":{\"_index\": \"" + indexName + "\", \"_type\": \"" + indexMappingType + "\", \"_id\":\""
                + Concept.formDocumentId(graphId, conceptId) + "\"}}\n";
    }

    private void bulkUpdateAndDeleteDocumentsToIndex(@NotNull UUID graphId, @NotNull List<Concept> updateConcepts,
            @NotNull List<UUID> deleteConceptsIds, boolean waitForRefresh) {

        if (updateConcepts.size() == 0 && deleteConceptsIds.size() == 0) {
            return; // nothing to do
        }

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html

        String index = updateConcepts.stream().map(this::createBulkIndexMetaAndSource)
                .collect(Collectors.joining("\n"));
        String delete = deleteConceptsIds.stream().map(id -> createBulkDeleteMeta(graphId, id))
                .collect(Collectors.joining("\n"));
        HttpEntity entity = new NStringEntity(index + delete,
                ContentType.create("application/x-ndjson", StandardCharsets.UTF_8));
        Map<String, String> params = new HashMap<>();

        params.put("pretty", "true");

        if (waitForRefresh) {
            params.put("refresh", "wait_for");
        }

        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("POST", "/_bulk", params, entity));

        if (isSuccess(response)) {
            log.info("Successfully added/updated documents to elasticsearch index: " + updateConcepts.size());
            log.info("Successfully deleted documents from elasticsearch index: " + deleteConceptsIds.size());
        } else {
            log.warn("Unable to add or update document to elasticsearch index: " + updateConcepts.size());
            log.warn("Unable to delete document from elasticsearch index: " + deleteConceptsIds.size());
        }
    }

    private void deleteDocumentsFromIndexByGraphId(@NotNull UUID graphId) {

        HttpEntity body = new NStringEntity("{\"query\": { \"match\": {\"vocabulary.id\": \"" + graphId + "\"}}}",
                ContentType.APPLICATION_JSON);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("POST",
                "/" + indexName + "/" + indexMappingType + "/_delete_by_query", emptyMap(), body));

        if (isSuccess(response)) {
            log.info(responseContentAsString(response));
            log.info("Successfully deleted documents from elasticsearch index from graph: " + graphId);
        } else {
            log.warn("Unable to delete documents from elasticsearch index");
        }
    }

    private void deleteAllDocumentsFromIndex() {

        HttpEntity body = new NStringEntity("{\"query\": { \"match_all\": {}}}", ContentType.APPLICATION_JSON);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("POST",
                "/" + indexName + "/" + indexMappingType + "/_delete_by_query", emptyMap(), body));

        if (isSuccess(response)) {
            log.info(responseContentAsString(response));
            log.info("Successfully deleted all documents from elasticsearch index");
        } else {
            log.warn("Unable to delete documents from elasticsearch index");
        }
    }

    public @Nullable JsonNode freeSearchFromIndex(String query) {
        Parameters params = new Parameters();
        params.add("source", query.toString());
        params.add("source_content_type", "application/json");
        String endpoint = "/" + indexName + "/" + indexMappingType + "/_search";

        HttpEntity body = new NStringEntity(query, ContentType.APPLICATION_JSON);
        try {
            Response response = esRestClient.performRequest("GET", endpoint, Collections.emptyMap(), body);
            String resp = responseContentAsString(response);
            if (isSuccess(response)) {
                if (log.isDebugEnabled()) {
                    log.debug(resp);
                    log.debug("Index query successfull");
                }
            } else {
                log.warn("Unable to query documents from elasticsearch index");
            }
            // String -> JSON
            JsonNode obj = objectMapper.readTree(resp);
            return obj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @Nullable Concept getConceptFromIndex(@NotNull UUID graphId, @NotNull UUID conceptId) {

        String documentId = Concept.formDocumentId(graphId, conceptId);
        Response response = alsoUnsuccessful(() -> esRestClient.performRequest("GET",
                "/" + indexName + "/" + indexMappingType + "/" + urlEncode(documentId) + "/_source"));

        if (isSuccess(response)) {
            return Concept.createFromIndex(objectMapper, responseContentAsJson(objectMapper, response));
        } else {
            return null;
        }
    }

    private @NotNull List<Concept> getConceptsFromIndex(@NotNull UUID graphId, @NotNull Collection<UUID> conceptIds) {

        // TODO inefficient implementation
        return conceptIds.stream().map(conceptId -> this.getConceptFromIndex(graphId, conceptId))
                .filter(Objects::nonNull).collect(toList());
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
        @NotNull
        Response get() throws IOException;
    }

    private boolean isSuccess(@NotNull Response response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 400;
    }

    private @NotNull HttpEntity createHttpEntity(@NotNull String classPathResourceJsonFile) {

        ClassPathResource resource = new ClassPathResource(classPathResourceJsonFile);

        try (InputStream is = resource.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(reader);
            return new NStringEntity(jsonNode.toString(), ContentType.APPLICATION_JSON);
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
