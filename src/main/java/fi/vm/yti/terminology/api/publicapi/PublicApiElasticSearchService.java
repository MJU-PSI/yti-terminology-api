package fi.vm.yti.terminology.api.publicapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.index.IndexTermedService;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.elasticsearch.client.Response;

import java.io.IOException;
import java.util.*;

import static fi.vm.yti.terminology.api.util.ElasticRequestUtils.responseContentAsJson;

@Service
public class PublicApiElasticSearchService {

    private final String indexName;
    private final String indexMappingType;

    private final RestClient esRestClient;

    private final IndexTermedService indexTermedService;

    @Autowired
    public PublicApiElasticSearchService(@Value("${search.host.url}") String searchHostUrl,
                                        @Value("${search.host.port}") int searchHostPort,
                                        @Value("${search.host.scheme}") String searchHostScheme,
                                        @Value("${search.index.name}") String indexName,
                                        @Value("${search.index.mapping.type}") String indexMappingType,
                                        IndexTermedService indexTermedService) {
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.esRestClient = RestClient.builder(new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();
        this.indexTermedService = indexTermedService;
    }


    List<PublicApiConcept> searchConcept(String searchTerm, String vocabularyId) {

        String endpoint = "/" + indexName + "/" + indexMappingType + "/_search";
        NStringEntity body = null;

        if (vocabularyId == null || vocabularyId.isEmpty() || vocabularyId.equals("0")) {
            String queryWithAllVocabulariesIncluded = "{\"query\":{\"bool\":{\"must\":[{\"multi_match\":{\"query\":\"" + searchTerm + "\",\"fields\":[\"label.fi^10\",\"label.*\"],\"type\":\"best_fields\",\"minimum_should_match\":\"90%\"}}],\"must_not\":[]}},\"highlight\":{\"pre_tags\":[\"<b>\"],\"post_tags\":[\"</b>\"],\"fields\":{\"label.*\":{}}},\"from\":0,\"size\":100,\"sort\":[\"_score\"]}";
            body = new NStringEntity(queryWithAllVocabulariesIncluded, ContentType.APPLICATION_JSON);
        } else {
            String queryWithVocabularySpecified = "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"vocabulary.id\":\"" + vocabularyId + "\"}},{\"multi_match\":{\"query\":\"" + searchTerm + "\",\"fields\":[\"label.fi^10\",\"label.*\"],\"type\":\"best_fields\",\"minimum_should_match\":\"90%\"}}],\"must_not\":[]}},\"highlight\":{\"pre_tags\":[\"<b>\"],\"post_tags\":[\"</b>\"],\"fields\":{\"label.*\":{}}},\"from\":0,\"size\":100,\"sort\":[\"_score\"]}";
            body = new NStringEntity(queryWithVocabularySpecified, ContentType.APPLICATION_JSON);
        }

        try {
            Response response = esRestClient.performRequest("GET", endpoint, Collections.emptyMap(), body);
            ObjectMapper om = new ObjectMapper();
            return getAsPublicApiConcepts(responseContentAsJson(om, response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PublicApiConcept> getAsPublicApiConcepts(JsonNode concepts) {
        List<PublicApiConcept> result = new ArrayList<>();
        JsonNode hitsNode = concepts.path("hits").path("hits");
        for(JsonNode jsonNode : hitsNode) {
            PublicApiConcept concept = new PublicApiConcept();
            concept.setId(UUID.fromString(jsonNode.path("_source").path("id").asText()));
            concept.setVocabularyId(UUID.fromString(jsonNode.path("_source").path("vocabulary").path("id").asText()));
            concept.setPrefLabel(extractLocalizableFromGivenField(jsonNode.path("_source"), "label"));
            concept.setDefinition(extractLocalizableFromGivenField(jsonNode.path("_source"), "definition"));
            concept.setVocabularyPrefLabel(extractLocalizableFromGivenField(jsonNode.path("_source").path("vocabulary"),"label"));
            concept.setUri(jsonNode.path("_source").path("uri").asText());
            result.add(concept);
        }
        return result;
    }

    public  HashMap<String, String> extractLocalizableFromGivenField(JsonNode node, String fieldName) {
        HashMap<String, String> result = new HashMap<>();
        result.put("fi", Jsoup.clean(node.get(fieldName).get("fi") == null ? "" : node.get(fieldName).get("fi").get(0).textValue(), Whitelist.none()));
        result.put("sv", Jsoup.clean(node.get(fieldName).get("sv") == null ? "" : node.get(fieldName).get("sv").get(0).textValue(), Whitelist.none()));
        result.put("en", Jsoup.clean(node.get(fieldName).get("en") == null ? "" : node.get(fieldName).get("en").get(0).textValue(), Whitelist.none()));
        return result;
    }
}