package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.exception.NotFoundException;
import fi.vm.yti.terminology.api.util.Parameters;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/frontend")
public class FrontendController {

    private final String termedUsername;
    private final String termedPassword;
    private final String termedUrl;
    private final String indexName;
    private final String indexMappingType;
    private final RestTemplate restTemplate;
    private final RestClient esRestClient;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public FrontendController(@Value("${api.user}") String termedUser,
                              @Value("${api.pw}") String termedPassword,
                              @Value("${api.url}") String termedUrl,
                              @Value("${search.host.url}") String searchHostUrl,
                              @Value("${search.host.port}") int searchHostPort,
                              @Value("${search.host.scheme}") String searchHostScheme,
                              @Value("${search.index.name}") String indexName,
                              @Value("${search.index.mapping.type}") String indexMappingType,
                              RestTemplate restTemplate,
                              AuthenticatedUserProvider userProvider) {
        this.termedUsername = termedUser;
        this.termedPassword = termedPassword;
        this.termedUrl = termedUrl;
        this.indexName = indexName;
        this.indexMappingType = indexMappingType;
        this.restTemplate = restTemplate;
        this.esRestClient = RestClient.builder(new HttpHost(searchHostUrl, searchHostPort, searchHostScheme)).build();
        this.userProvider = userProvider;
    }

    @RequestMapping(value = "/authenticated-user", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    YtiUser getUser() {
        return this.userProvider.getUser();
    }
    
    @RequestMapping("/vocabulary")
    JsonNode getVocabulary(@RequestParam String graphId,
                           @RequestParam String vocabularyType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + vocabularyType);
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return requireSingle(response.getBody());
    }

    @RequestMapping("/vocabularies")
    ArrayNode getVocabularyList(@RequestParam String vocabularyType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("select", "references.publisher");
        params.add("select", "references.inGroup");
        params.add("where", "type.id:" + vocabularyType);
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return response.getBody();
    }

    @RequestMapping("/concept")
    @Nullable JsonNode getConcept(@RequestParam String graphId,
                                  @RequestParam String conceptId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("select", "referrers.*");
        params.add("select", "referrers.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "id:" + conceptId);
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        JsonNode concept = findSingle(response.getBody());

        if (concept == null) {
            throw new NotFoundException(graphId, conceptId);
        }

        return concept;
    }

    @RequestMapping("/collection")
    JsonNode getCollection(@RequestParam String graphId,
                           @RequestParam String collectionId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.*");
        params.add("select", "references.*");
        params.add("select", "references.prefLabelXl:2");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "id:" + collectionId);
        params.add("max", "-1");
        
        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return requireSingle(response.getBody());
    }

    @RequestMapping("/collections")
    ArrayNode getCollectionList(@RequestParam String graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.prefLabel");
        params.add("select", "properties.status");
        params.add("select", "lastModifiedDate");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + "Collection");
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return response.getBody();
    }

    @RequestMapping("/organizations")
    ArrayNode getOrganizationList() {
        return getNodeListWithoutReferencesOrReferrers("Organization");
    }

    @RequestMapping("/groups")
    ArrayNode getGroupList() {
        return getNodeListWithoutReferencesOrReferrers("Group");
    }

    private ArrayNode getNodeListWithoutReferencesOrReferrers(String type) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("where", "type.id:" + type);
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return response.getBody();
    }

    // TODO: better typing for easy authorization
    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    void updateAndDeleteInternalNodes(@RequestBody JsonNode deleteAndSave) {

        Parameters params = new Parameters();
        params.add("changeset", "true");
        params.add("sync", "true");

        // TODO user authenticated user credentials

        this.restTemplate.exchange(createUrl("/nodes", params), HttpMethod.POST,
                new HttpEntity<>(deleteAndSave, createAuthHeaders(termedUsername, termedPassword)), String.class);
    }

    // TODO: better typing for easy authorization
    @RequestMapping(value = "/remove", method = RequestMethod.DELETE)
    void removeNodes(@RequestParam boolean sync,
                     @RequestBody ArrayNode identifiers) {

        Parameters params = new Parameters();
        params.add("batch", "true");
        params.add("sync", Boolean.toString(sync));

        // TODO user authenticated user credentials

        this.restTemplate.exchange(createUrl("/nodes", params), HttpMethod.DELETE,
                new HttpEntity<>(identifiers, createAuthHeaders(termedUsername, termedPassword)), String.class);
    }

    
    @RequestMapping("/nodes")
    ArrayNode getAllNodeIdentifiers(@RequestParam String graphId) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("where", "graph.id:" + graphId);
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/node-trees", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return response.getBody();
    }

    @RequestMapping(value = "/types", method = RequestMethod.GET)
    ArrayNode getTypes(@RequestParam(required = false) String graphId) {

        Parameters params = new Parameters();
        params.add("max", "-1");

        String url = graphId != null ? "/graphs/" + graphId + "/types" : "/types";

        ResponseEntity<ArrayNode> response =
                this.restTemplate.exchange(createUrl(url, params), HttpMethod.GET,
                        new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return response.getBody();
    }

    // TODO: better typing for easy authorization
    @RequestMapping(value = "/types", method = RequestMethod.POST)
    void updateTypes(@RequestParam String graphId,
                     @RequestBody ArrayNode metaNodes) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        this.restTemplate.exchange(createUrl("/graphs/" + graphId + "/types", params), HttpMethod.POST,
                new HttpEntity<>(metaNodes, createAuthHeaders(termedUsername, termedPassword)), String.class);
    }

    // TODO: better typing for easy authorization
    @RequestMapping(value = "/types", method = RequestMethod.DELETE)
    void removeTypes(@RequestParam String graphId,
                     @RequestBody ArrayNode identifiers) {

        Parameters params = new Parameters();
        params.add("batch", "true");

        this.restTemplate.exchange(createUrl("/graphs/" + graphId + "/types", params), HttpMethod.DELETE,
                new HttpEntity<>(identifiers, createAuthHeaders(termedUsername, termedPassword)), String.class);
    }

    @RequestMapping("/graphs")
    ArrayNode getGraphs() {

        Parameters params = new Parameters();
        params.add("max", "-1");

        ResponseEntity<ArrayNode> response = this.restTemplate.exchange(createUrl("/graphs", params), HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(termedUsername, termedPassword)), ArrayNode.class);

        return response.getBody();
    }

    // TODO: better typing for easy authorization
    @RequestMapping(value = "/graph", method = RequestMethod.POST)
    void createGraph(@RequestBody JsonNode graph) {

        this.restTemplate.exchange(createUrl("/graphs"), HttpMethod.POST,
                new HttpEntity<>(graph, createAuthHeaders(termedUsername, termedPassword)), String.class);
    }

    @RequestMapping(value = "/graph", method = RequestMethod.DELETE)
    void deleteGraph(@RequestParam String graphId) {
        this.restTemplate.exchange(createUrl("/graphs/" + graphId), HttpMethod.DELETE,
                new HttpEntity<>(null, createAuthHeaders(termedUsername, termedPassword)), String.class);
    }

    @RequestMapping(value = "/searchConcept", method = RequestMethod.POST)
    String searchConcept(@RequestBody JsonNode query) {

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

    private static @NotNull String responseContentAsString(@NotNull Response response) {
        try (InputStream is = response.getEntity().getContent()) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @NotNull JsonNode requireSingle(ArrayNode array) {
        return requireNonNull(findSingle(array), "One array item required, was: " + array.size());
    }

    private static @Nullable JsonNode findSingle(ArrayNode array) {

        if (array.size() > 1) {
            throw new RuntimeException("One or zero array items required, was: " + array.size());
        } else if (array.size() == 0) {
            return null;
        } else {
            return array.get(0);
        }
    }

    private @NotNull HttpHeaders createAuthHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getAuthHeader(username, password));
        return headers;
    }

    private @NotNull String getAuthHeader(String username, String password) {
        return "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    private @NotNull String createUrl(@NotNull String path) {
        return createUrl(path, new Parameters());
    }

    private @NotNull String createUrl(@NotNull String path, @NotNull Parameters parameters) {
        return termedUrl + path + parameters.toString();
    }
}
