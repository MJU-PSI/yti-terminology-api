package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.config.JsonConfig;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.util.RestHighLevelClientWrapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ContextParser;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.max.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.tophits.ParsedTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

// TODO: we can manage without the lowlevel client now in these tests
// but this might be nice code for some other case
/*
@TestConfiguration
class TestConfig {

    @Bean
    public RestHighLevelClientWrapper getElasticSearchRestHighLevelClient() throws IOException {

        var llc = Mockito.mock(RestClient.class);

        // prepare a response for lowlevel calls
        var response = Mockito.mock(org.elasticsearch.client.Response.class);
        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1),
                200,
                "OK"));

        // mock whichever calls are being used during the app initialization
        Mockito.when(llc.performRequest(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any()))
                .thenReturn(response);

        var hlc = Mockito.mock(RestHighLevelClientWrapperImpl.class);
        Mockito.when(hlc.getLowLevelClient()).thenReturn(llc);

        return hlc;
    }
}
*/

@Import({ JsonConfig.class, FrontendElasticSearchService.class })
@ExtendWith(SpringExtension.class) // TODO: read this https://rieckpil.de/what-the-heck-is-the-springextension-used-for/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS) // TODO: doesn't work
@TestPropertySource(properties = {
        "search.index.name=testIndex"
})
public class FrontendElasticSearchServiceTest {

    // provided by org.springframework.context.support.GenericApplicationContext
    @Autowired
    ResourceLoader resourceLoader;

    @MockBean()
    RestHighLevelClientWrapper esClient;

    @MockBean
    AuthenticatedUserProvider userProvider;

    // not a mock, using the same Bean as the application from JsonConfig
    @Autowired
    ObjectMapper objectMapper;

    // service under test
    @Autowired
    FrontendElasticSearchService service;

    @Before
    public void testInit()
    {
        Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();

    }
    private YtiUser createMockUser(boolean superUser) {
        return new YtiUser(
                "test@test.invalid",
                "firstname",
                "lastname",
                UUID.fromString("384c982c-7254-43df-ab7c-5037b6fb71c0"),
                superUser,
                false,
                LocalDateTime.of(2005, 4, 2, 1, 10),
                LocalDateTime.of(2006, 4, 2, 1, 10),
                new HashMap<>(),
                "",
                "");
    }

    private boolean isVocabularyQuery(SearchRequest arg)
    {
        if (arg.indices().length < 1 || arg.indices()[0] != "vocabularies") {
            return false;
        }
        return true;
    }

    private boolean isConceptQuery(SearchRequest arg)
    {
        if (arg.indices().length < 1 || arg.indices()[0] != "concepts") {
            return false;
        }
        return true;
    }

    @Test
    public void test1() throws IOException {

        // TODO: capture logger errors
        // var logger = Mockito.mock(Logger.class);
        // Mockito.when(LoggerFactory.getLogger(Mockito.any(Class.class))).thenReturn(logger);
        // Mockito.verify(logger).warn(Mockito.anyString());
        // Mockito.verify(logger).error(Mockito.anyString());

        // make sure DI is functioning
        assertNotNull(service);
        assertNotNull(esClient);
        assertNotNull(objectMapper);

        // convert JSON sample data to SearchResponse for returning as mock
        // values from esClient searches
        var conceptsResponse = this.getSearchResponseFromJson(this.getResourceContents("classpath:es/test_response_1.json"));
        var vocabulariesResponse = this.getSearchResponseFromJson(this.getResourceContents("classpath:es/test_response_2.json"));

        // getUser is being used to check for superUser and organizations
        Mockito.when(this.userProvider.getUser())
                .thenReturn(this.createMockUser(false));

        // Mock the same esClient call with different arguments & return values
        // TODO: unstubbed calls are not being caught
        Mockito.when(this.esClient.search(
                    Mockito.argThat(i -> isConceptQuery(i)),
                    Mockito.any()))
                .thenReturn(conceptsResponse);
        Mockito.when(this.esClient.search(
                    Mockito.argThat(i -> isVocabularyQuery(i)),
                    Mockito.any()))
                .thenReturn(vocabulariesResponse);

        // Actual query
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setSearchConcepts(true); // true == additional concept search
        var response = service.searchTerminology(request);

        assertNotNull(response);

        // TODO: try ArgumentCaptor for verifying esClient calls
        // https://stackoverflow.com/a/29169875
        // var argument = ArgumentCaptor.forClass(SearchRequest.class);

        // Make sure the mocks were called as expected
        Mockito.verify(this.userProvider, Mockito.times(2))
                .getUser();
        Mockito.verify(this.esClient, Mockito.times(1))
                .search(
                        Mockito.argThat(i -> isVocabularyQuery(i)),
                        Mockito.any(org.elasticsearch.client.RequestOptions.class));
        Mockito.verify(this.esClient, Mockito.times(1))
                .search(
                        Mockito.argThat(i -> isConceptQuery(i)),
                        Mockito.any(org.elasticsearch.client.RequestOptions.class));
        Mockito.verifyNoMoreInteractions(esClient);
    }

    // read resource contents from a file, used for test data
    private String getResourceContents(String path) throws IOException {
        var stream = resourceLoader.getResource(path).getInputStream();
        var br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        var result =  br
                .lines()
                .collect(Collectors.joining("\n"));
        br.close();
        stream.close();
        return result;
    }

    // for use with getSearchResponseFromJson
    // TODO: Could not parse aggregation keyed as [group_by_terminology]
    // https://stackoverflow.com/questions/49798654/how-do-you-convert-an-elasticsearch-json-string-response-with-an-aggregation-t
    public static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(TopHitsAggregationBuilder.NAME, (p, c) ->
                ParsedTopHits.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) ->
                ParsedStringTerms.fromXContent(p, (String) c));
        map.put("group_by_terminology", (p, c) ->
                ParsedStringTerms.fromXContent(p, (String) c));
        map.put("top_concept_hits", (p, c) ->
                ParsedTopHits.fromXContent(p, (String) c));
        map.put("best_concept_hit", (p, c) ->
                ParsedMax.fromXContent(p, (String) c));
        return map.entrySet().stream()
                .map(entry -> new NamedXContentRegistry.Entry(
                        Aggregation.class,
                        new ParseField(entry.getKey()),
                        entry.getValue()))
                .collect(Collectors.toList());
    }

    // helper method for generating elasticsearch SearchResponse from JSON
    private SearchResponse getSearchResponseFromJson(String jsonResponse) throws IOException {
        NamedXContentRegistry registry = new NamedXContentRegistry(
                getDefaultNamedXContents());
        XContentParser parser = JsonXContent.jsonXContent.createParser(
                registry,
                DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                jsonResponse);
        SearchResponse searchResponse = SearchResponse.fromXContent(parser);
        return searchResponse;
    }
}
