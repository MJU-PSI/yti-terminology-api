package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FrontendElasticSearchService {

    private final RestTemplate restTemplate;
    private String theUrl;

    @Autowired
    public FrontendElasticSearchService(@Value("${search.host.url}") String searchHostUrl,
                                        @Value("${search.host.port}") int searchHostPort,
                                        @Value("${search.host.scheme}") String searchHostScheme,
                                        @Value("${search.index.name}") String indexName,
                                        @Value("${search.index.mapping.type}") String indexMappingType) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.theUrl = searchHostScheme + "://" + searchHostUrl + ":" + searchHostPort + "/" + indexName + "/" + indexMappingType + "/_search";
    }

    String searchConcept(JsonNode query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(query.toString(), headers);
        try {
            return this.restTemplate.exchange(this.theUrl, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
