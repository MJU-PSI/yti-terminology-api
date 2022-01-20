package fi.vm.yti.terminology.api.frontend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.config.JsonConfig;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.util.RestHighLevelClientWrapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ContextParser;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.tophits.ParsedTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Import({ JsonConfig.class, FrontendElasticSearchService.class })
@ExtendWith(SpringExtension.class) // TODO: read this https://rieckpil.de/what-the-heck-is-the-springextension-used-for/
@ExtendWith(MockitoExtension.class)
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

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setup() {
        // make sure DI is functioning
        assertNotNull(service);
        assertNotNull(esClient);
        assertNotNull(objectMapper);

        this.initLogWatcher();
    }

    @AfterEach
    void cleanup() {
        this.cleanLogWatcher();
    }

    private void cleanLogWatcher() {
        var logger = (Logger) LoggerFactory.getLogger(FrontendElasticSearchService.class);
        logger.detachAndStopAllAppenders();
    }

    private void initLogWatcher() {
        this.logWatcher = new ListAppender<>();
        this.logWatcher.start();
        // cast from facade (SLF4J) to implementation class (logback)
        var logger = (Logger) LoggerFactory.getLogger(FrontendElasticSearchService.class);
        logger.addAppender(this.logWatcher);
    }

    private void assertNoLogErrors() {
        var errors = logWatcher.list.stream()
                .filter(x -> "ERROR".equals(x.getLevel().toString()))
                .collect(Collectors.toList());
        assertEquals(0, errors.size());
    }

    private YtiUser createMockUser(boolean superUser, Map<UUID, Set<Role>> roles) {
        return new YtiUser(
                "test@test.invalid",
                "firstname",
                "lastname",
                UUID.fromString("384c982c-7254-43df-ab7c-5037b6fb71c0"),
                superUser,
                false,
                LocalDateTime.of(2005, 4, 2, 1, 10),
                LocalDateTime.of(2006, 4, 2, 1, 10),
                roles,
                "",
                "");
    }

    private YtiUser createMockUser(boolean superUser) {
        return this.createMockUser(superUser, new HashMap<>());
    }

    private boolean isVocabularyQuery(SearchRequest arg)
    {
        if (arg.indices().length != 1 || !"vocabularies".equals(arg.indices()[0])) {
            return false;
        }
        return true;
    }

    private boolean isConceptQuery(SearchRequest arg)
    {
        if (arg.indices().length != 1 || !"concepts".equals(arg.indices()[0])) {
            return false;
        }
        return true;
    }

    @Test
    public void searchWithoutConcepts() throws IOException {
        // convert JSON sample data to SearchResponse for returning as mock
        // values from esClient searches
        var vocabulariesResponse =this.getMockResponse(
                "classpath:es/vocabulary_response.json");
        assertNotNull(vocabulariesResponse);

        // getUser is being used to check for superUser and organizations
        doReturn(this.createMockUser(false))
                .when(this.userProvider)
                .getUser();

        // Mock the same esClient call with a specific argument
        doReturn(vocabulariesResponse)
                .when(this.esClient)
                .search(argThat(i -> isVocabularyQuery(i)), any());

        // Actual query
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setSearchConcepts(false); // false == no additional concept search
        var response = service.searchTerminology(request);
        assertNotNull(response);

        // Make sure the mocks were called as expected
        verify(this.userProvider, times(2))
                .getUser();

        ArgumentCaptor<SearchRequest> srCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.esClient, times(1))
                .search(srCaptor.capture(), any(RequestOptions.class));

        // Check that esClient.search was called with expected kinds of arguments
        var args = srCaptor.getAllValues();
        assertEquals(1, args.size());

        var vocabulariesRequest = args.get(0);
        assertEquals("vocabularies",
                String.join(",", vocabulariesRequest.indices()));

        verifyNoMoreInteractions(esClient);
        assertNoLogErrors();
    }

    @Test
    public void searchWithConcepts() throws IOException {
        // convert JSON sample data to SearchResponse for returning as mock
        // values from esClient searches
        var conceptsResponse = this.getMockResponse(
                "classpath:es/response/concept_response.json");
        assertNotNull(conceptsResponse);
        var vocabulariesResponse = this.getMockResponse(
                "classpath:es/response/vocabulary_response.json");
        assertNotNull(vocabulariesResponse);

        // getUser is being used to check for superUser and organizations
        doReturn(this.createMockUser(false))
                .when(this.userProvider)
                .getUser();

        // Mock the same esClient call with different arguments & return values
        doReturn(conceptsResponse)
                .when(this.esClient)
                .search(argThat(i -> isConceptQuery(i)), any());
        doReturn(vocabulariesResponse)
                .when(this.esClient)
                .search(argThat(i -> isVocabularyQuery(i)), any());

        // Actual query
        var request = new TerminologySearchRequest();
        request.setQuery("test");
        request.setSearchConcepts(true); // true == additional concept search
        var response = service.searchTerminology(request);
        assertNotNull(response);

        // Make sure the mocks were called as expected

        verify(this.userProvider, times(2))
                .getUser();

        ArgumentCaptor<SearchRequest> srCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.esClient, times(2))
                .search(srCaptor.capture(), any(RequestOptions.class));

        // Check that esClient.search was called with expected kinds of arguments
        var args = srCaptor.getAllValues();
        assertEquals(2, args.size());

        var conceptsRequest = args.get(0);
        assertEquals("concepts",
                String.join(",", conceptsRequest.indices()));

        var vocabulariesRequest = args.get(1);
        assertEquals("vocabularies",
                String.join(",", vocabulariesRequest.indices()));

        // TODO: do some JSON based checks of the requests
        // maybe with JSONAssert?

        Mockito.verifyNoMoreInteractions(esClient);

        assertNoLogErrors();
    }

    @Test
    public void testOrganizationQuery() throws Exception {
        var request = new TerminologySearchRequest();
        request.setQuery("foo");

        var vocabulariesResponse = this.getMockResponse(
                "classpath:es/vocabulary_response.json");

        var orgId = UUID.randomUUID();
        Map<UUID, Set<Role>> roles = new HashMap<>();
        roles.put(orgId, Set.of(Role.ADMIN));

        doReturn(vocabulariesResponse)
                .when(this.esClient)
                .search(argThat(i -> isVocabularyQuery(i)), any());
        doReturn(this.createMockUser(false, roles))
            .when(userProvider)
            .getUser();

        service.searchTerminology(request);

        ArgumentCaptor<SearchRequest> srCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(this.esClient)
                .search(srCaptor.capture(), any(RequestOptions.class));

        var values = srCaptor.getAllValues();

        // query includes organization id
        assertTrue(values.get(0).source().toString().indexOf("references.contributor.id\":[\"" + orgId.toString()) > -1);
    }

    private SearchResponse getMockResponse(String path) throws IOException {
        return this.getSearchResponseFromJson(this.getResourceContents(path));
    }

    // read resource contents from a file, used for test data
    private String getResourceContents(String path) throws IOException {
        var stream = resourceLoader.getResource(path).getInputStream();
        var br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        var result = br.lines().collect(Collectors.joining("\n"));
        br.close();
        stream.close();
        return result;
    }

    // for use with getSearchResponseFromJson
    public static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        // Elasticsearch needs a hint to know what type of aggregation to
        // parse this as. The hint is provided by elastic when
        // adding ?typed_keys to the query.
        // e.g. "sterms#group_by_terminology"
        map.put(TopHitsAggregationBuilder.NAME, (p, c) ->
                ParsedTopHits.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) ->
                ParsedStringTerms.fromXContent(p, (String) c));
        map.put(MaxAggregationBuilder.NAME, (p, c) ->
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
