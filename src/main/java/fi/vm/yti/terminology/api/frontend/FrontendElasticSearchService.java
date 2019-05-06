package fi.vm.yti.terminology.api.frontend;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.frontend.elasticqueries.DeepConceptQueryFactory;
import fi.vm.yti.terminology.api.frontend.elasticqueries.TerminologyQueryFactory;
import fi.vm.yti.terminology.api.frontend.searchdto.DeepSearchHitListDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchResponse;
import fi.vm.yti.terminology.api.util.Parameters;
import static fi.vm.yti.terminology.api.util.ElasticRequestUtils.responseContentAsString;

@Service
public class FrontendElasticSearchService {

    private static final Logger logger = LoggerFactory.getLogger(FrontendElasticSearchService.class);

    private final RestClient esRestClient;

    private final String indexName;
    private final String indexMappingType;

    private final ObjectMapper objectMapper;
    private final TerminologyQueryFactory terminologyQueryFactory;
    private final DeepConceptQueryFactory deepConceptQueryFactory;

    @Autowired
    public FrontendElasticSearchService(@Value("${search.host.url}") String searchHostUrl,
                                        @Value("${search.host.port}") int searchHostPort,
                                        @Value("${search.host.scheme}") String searchHostScheme,
                                        @Value("${search.index.name}") String indexName,
                                        @Value("${search.index.mapping.type}") String indexMappingType,
                                        ObjectMapper objectMapper) {
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.esRestClient = RestClient.builder(new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();

        this.objectMapper = objectMapper;
        this.terminologyQueryFactory = new TerminologyQueryFactory(objectMapper);
        this.deepConceptQueryFactory = new DeepConceptQueryFactory(objectMapper);
    }

    @SuppressWarnings("Duplicates")
    String searchConcept(JsonNode query) {
        return searchFromIndex(query, "concepts");
    }

    @SuppressWarnings("Duplicates")
    String searchVocabulary(JsonNode query) {
        return searchFromIndex(query, "vocabularies");
    }

    String searchFromIndex(JsonNode query,
                           String index) {
        Parameters params = new Parameters();
        params.add("source", query.toString());
        params.add("source_content_type", "application/json");
        String endpoint = "/" + index + "/" + indexMappingType + "/_search";
        NStringEntity body = new NStringEntity(query.toString(), ContentType.APPLICATION_JSON);
        try {
            Response response = esRestClient.performRequest("GET", endpoint, Collections.emptyMap(), body);
            return responseContentAsString(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    TerminologySearchResponse searchTerminology(TerminologySearchRequest request) {
        request.setQuery(request.getQuery() != null ? request.getQuery().trim() : "");

        Map<String, List<DeepSearchHitListDTO<?>>> deepSearchHits = null;
        if (request.isSearchConcepts() && !request.getQuery().isEmpty()) {
            try {
                Request conceptRequest = new Request("POST", "/concepts/_search");
                String deepQuery = deepConceptQueryFactory.createQuery(request.getQuery(), request.getPrefLang());
                //logger.debug("Deep concept query:\n" + deepQuery);
                conceptRequest.setJsonEntity(deepQuery);
                Response response = esRestClient.performRequest(conceptRequest);
                deepSearchHits = deepConceptQueryFactory.parseResponse(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Request terminologyRequest = new Request("POST", "/vocabularies/_search");
            if (deepSearchHits != null && !deepSearchHits.isEmpty()) {
                Set<String> additionalTerminilogyIds = deepSearchHits.keySet();
                logger.debug("Deep concept search resulted in " + additionalTerminilogyIds.size() + " terminology matches");
                String finalQuery = terminologyQueryFactory.createQuery(request, additionalTerminilogyIds);
                //logger.debug("Final terminology query:\n" + finalQuery);
                terminologyRequest.setJsonEntity(finalQuery);
            } else {
                String finalQuery = terminologyQueryFactory.createQuery(request);
                //logger.debug("Final terminology query:\n" + finalQuery);
                terminologyRequest.setJsonEntity(finalQuery);
            }
            Response response = esRestClient.performRequest(terminologyRequest);
            return terminologyQueryFactory.parseResponse(response, request, deepSearchHits);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
