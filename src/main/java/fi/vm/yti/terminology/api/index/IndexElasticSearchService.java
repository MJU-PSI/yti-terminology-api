package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@Service
public class IndexElasticSearchService {

    private static final Logger log = LoggerFactory.getLogger(IndexElasticSearchService.class);

    private final RestTemplate restTemplate;
    private String baseUrl;
    private final String createIndexFilename;
    private final String createMappingsFilename;
    private final String indexName;
    private final String indexMappingType;
    private final boolean deleteIndexOnAppRestart;

    private final IndexTermedService termedApiService;
    private ObjectMapper objectMapper;

    @Autowired
    public IndexElasticSearchService(@Value("${search.host.url}") String searchHostUrl,
                                     @Value("${search.host.port}") int searchHostPort,
                                     @Value("${search.host.scheme}") String searchHostScheme,
                                     @Value("${search.index.file}") String createIndexFilename,
                                     @Value("${search.index.mapping.file}") String createMappingsFilename,
                                     @Value("${search.index.name}") String indexName,
                                     @Value("${search.index.mapping.type}") String indexMappingType,
                                     @Value("${search.index.deleteIndexOnAppRestart}") boolean deleteIndexOnAppRestart,
                                     IndexTermedService termedApiService,
                                     ObjectMapper objectMapper) {
        this.createIndexFilename = createIndexFilename;
        this.createMappingsFilename = createMappingsFilename;
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.deleteIndexOnAppRestart = deleteIndexOnAppRestart;
        this.termedApiService = termedApiService;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.baseUrl = searchHostScheme + "://" + searchHostUrl + ":" + searchHostPort;
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

    private static @NotNull Set<UUID> broaderAndNarrowerIds(@NotNull List<List<Concept>> concepts) {

        return concepts.stream()
                .flatMap(Collection::stream)
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
        boolean gotError = false;
        try {
            this.restTemplate.delete(baseUrl + "/" + indexName);
        } catch (HttpClientErrorException e) {
            gotError = true;
            if (e.getRawStatusCode() == 404) {
                log.info("Elasticsearch index not deleted because it did not exist, Http code 404 was returned.");
            } else {
                log.info("Elasticsearch index not deleted. Http code returned was " + e.getRawStatusCode());
                log.info("The exception was " + e);
            }
        }
        if (!gotError) {
            log.info("Elasticsearch index deleted: " + indexName);
        }
    }

    private boolean indexExists() {
        log.info("Checking if elasticsearch index exists: " + indexName);

        try {
            this.restTemplate.headForHeaders(baseUrl + "/" + indexName);
        } catch (HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404) {
                log.info("Elasticsearch index does not exist: " + indexName);
                return false;
            }
        }
        return true;
    }

    private boolean createIndex() {
        HttpEntity entity = createHttpEntity(createIndexFilename);
        log.info("Trying to create elasticsearch index: " + indexName);
        try {
            this.restTemplate.put(baseUrl + "/" + indexName, entity, singletonMap("pretty", "true"));
        } catch (HttpClientErrorException e) {
            log.warn("Unable to create elasticsearch index: " + indexName);
            return false;
        }
        log.info("elasticsearch index successfully created: " + indexName);
        return true;
    }

    private boolean createMapping() {
        HttpEntity entity = createHttpEntity(createMappingsFilename);
        log.info("Trying to create elasticsearch index mapping type: " + indexMappingType);
        try {
            this.restTemplate.put(baseUrl + "/" + indexName + "/_mapping/" + indexMappingType, entity, singletonMap("pretty", "true"));
        } catch (HttpClientErrorException e) {
            log.warn("Unable to create elasticsearch index mapping type: " + indexMappingType + ". Http code was " + e.getRawStatusCode());
            return false;
        }
        log.info("elasticsearch index mapping type successfully created: " + indexMappingType);
        return true;

    }

    private @NotNull String createBulkIndexMetaAndSource(@NotNull Concept concept) {
        return "{\"index\":{\"_index\": \"" + indexName + "\", \"_type\": \"" + indexMappingType + "\", \"_id\":\"" + concept.getDocumentId() + "\"}}\n" + concept.toElasticSearchDocument(objectMapper) + "\n";
    }

    private @NotNull String createBulkDeleteMeta(@NotNull UUID graphId, @NotNull UUID conceptId) {
        return "{\"delete\":{\"_index\": \"" + indexName + "\", \"_type\": \"" + indexMappingType + "\", \"_id\":\"" + Concept.formDocumentId(graphId, conceptId) + "\"}}\n";
    }

    private void bulkUpdateAndDeleteDocumentsToIndex(@NotNull UUID graphId,
                                                     @NotNull List<Concept> updateConcepts,
                                                     @NotNull List<UUID> deleteConceptsIds,
                                                     boolean waitForRefresh) {

        if (updateConcepts.size() == 0 && deleteConceptsIds.size() == 0) {
            return; // nothing to do
        }

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html

        String index = updateConcepts.stream().map(this::createBulkIndexMetaAndSource).collect(Collectors.joining("\n"));
        String delete = deleteConceptsIds.stream().map(id -> createBulkDeleteMeta(graphId, id)).collect(Collectors.joining("\n"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "x-ndjson", StandardCharsets.UTF_8));
        HttpEntity entity = new HttpEntity(index + delete, headers);
        Map<String, String> params = new HashMap<>();
        params.put("pretty", "true");

        if (waitForRefresh) {
            params.put("refresh", "wait_for");
        }

        boolean gotError = false;
        try {
            this.restTemplate.postForLocation(baseUrl + "/_bulk", entity, params);
        } catch (HttpClientErrorException e) {
            log.warn("Unable to add or update document to elasticsearch index: " + updateConcepts.size());
            log.warn("Unable to delete document from elasticsearch index: " + deleteConceptsIds.size());
            gotError = true;
        }
        if (!gotError) {
            log.info("Successfully added/updated documents to elasticsearch index: " + updateConcepts.size());
            log.info("Successfully deleted documents from elasticsearch index: " + deleteConceptsIds.size());
        }
    }

    private void deleteDocumentsFromIndexByGraphId(@NotNull UUID graphId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        HttpEntity body = new HttpEntity("{\"query\": { \"match\": {\"vocabulary.id\": \"" + graphId + "\"}}}", headers);
        boolean gotError = false;
        try {
            this.restTemplate.postForLocation(baseUrl + "/" + indexName + "/" + indexMappingType + "/_delete_by_query", body, emptyMap());
        } catch (HttpClientErrorException e) {
            log.warn("Unable to delete documents from elasticsearch index", e);
            gotError = true;
        }
        if (!gotError) {
            log.info("Successfully deleted documents from elasticsearch index from graph: " + graphId);
        }
    }

    private void deleteAllDocumentsFromIndex() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        HttpEntity body = new HttpEntity("{\"query\": { \"match_all\": {}}}", headers);

        boolean gotError = false;
        try {
            this.restTemplate.postForLocation(baseUrl + "/" + indexName + "/" + indexMappingType + "/_delete_by_query", body, emptyMap());
        } catch (HttpClientErrorException e) {
            log.warn("Unable to delete documents from elasticsearch index", e);
            gotError = true;
        }
        if (!gotError) {
            log.info("Successfully deleted all documents from elasticsearch index");
        }
    }

    private @Nullable Concept getConceptFromIndex(@NotNull UUID graphId, @NotNull UUID conceptId) {
        String documentId = Concept.formDocumentId(graphId, conceptId);
        URI uri = URI.create(baseUrl + "/" + indexName + "/" + indexMappingType + "/" + urlEncode(documentId) + "/_source");
        String resultAsJson = this.restTemplate.getForObject(uri, String.class);
        try {
            return Concept.createFromIndex(objectMapper, objectMapper.readTree(resultAsJson));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull List<Concept> getConceptsFromIndex(@NotNull UUID graphId, @NotNull Collection<UUID> conceptIds) {

        // TODO inefficient implementation
        return conceptIds.stream()
                .map(conceptId -> this.getConceptFromIndex(graphId, conceptId))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private static @NotNull String urlEncode(@NotNull String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull HttpEntity createHttpEntity(@NotNull String classPathResourceJsonFile) {

        ClassPathResource resource = new ClassPathResource(classPathResourceJsonFile);

        try (InputStream is = resource.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(reader);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity(jsonNode.toString(), headers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
