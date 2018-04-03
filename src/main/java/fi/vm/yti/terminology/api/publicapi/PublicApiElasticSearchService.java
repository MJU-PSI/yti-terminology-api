package fi.vm.yti.terminology.api.publicapi;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.terminology.api.util.Parameters;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.elasticsearch.client.Response;

import java.io.IOException;
import java.util.*;

import static fi.vm.yti.terminology.api.util.ElasticRequestUtils.responseContentAsString;

@Service
public class PublicApiElasticSearchService {

    private final String indexName;
    private final String indexMappingType;

    private final RestClient esRestClient;

    @Autowired
    public PublicApiElasticSearchService(@Value("${search.host.url}") String searchHostUrl,
                                        @Value("${search.host.port}") int searchHostPort,
                                        @Value("${search.host.scheme}") String searchHostScheme,
                                        @Value("${search.index.name}") String indexName,
                                        @Value("${search.index.mapping.type}") String indexMappingType) {
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.esRestClient = RestClient.builder(new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();
    }

    @SuppressWarnings("Duplicates")
    String searchConcept(JsonNode query) {
        Parameters params = new Parameters();
        params.add("source", query.toString());
        params.add("source_content_type", "application/json");
        String endpoint = "/" + indexName + "/" + indexMappingType + "/_search";
        NStringEntity body = new NStringEntity(query.toString(), ContentType.APPLICATION_JSON);
        try {
            Response response = esRestClient.performRequest("GET", endpoint, Collections.emptyMap(), body);
            return responseContentAsString(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}