package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.Parameters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        FrontendTermedService.class
})
class FrontendTermedServiceTest {

    @MockBean
    FrontendGroupManagementService groupManagementService;

    @MockBean
    AuthorizationManager authorizationManager;

    @MockBean
    AuthenticatedUserProvider authenticatedUserProvider;

    @MockBean
    TermedRequester termedRequester;

    @Autowired
    FrontendTermedService frontEndTermedService;

    ObjectMapper mapper = new ObjectMapper();

    final String groupsJsonData = "[" +
            "{\"id\": \"123\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Services for families\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\"}" +
            "]}}," +
            "{\"id\": \"789\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Housing\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Asuminen\"}" +
            "]}}," +
            "{\"id\": \"456\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Public order\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Järjestys\"}" +
            "]}} " +
            "]";

    final String groupsJsonMissingData = "[" +
            "{\"id\": \"123\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Services for families\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\"}" +
            "]}}," +
            "{\"id\": \"789\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Housing\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Asuminen\"}" +
            "]}}," +
            "{\"id\": \"456\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Public order\"}" +
            "]}} " +
            "]";

    final String organizationsJsonData = "[" +
            "{\"id\": \"321\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\"}" +
            "]}}," +
            "{\"id\": \"654\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Test-organization\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\"}" +
            "]}}" +
            "]";

    final String organizationsJsonMissingData = "[" +
            "{\"id\": \"321\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\"}," +
            "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\"}" +
            "]}}," +
            "{\"id\": \"654\"," +
            "\"properties\": " +
            "{ \"prefLabel\": [" +
            "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\"}" +
            "]}}" +
            "]";

    final JsonNode initGroupsNode = mapper.readTree(groupsJsonData);
    final JsonNode initGroupsNodeMissing = mapper.readTree(groupsJsonMissingData);
    final JsonNode initOrgsNode = mapper.readTree(organizationsJsonData);
    final JsonNode initOrgsNodeMissing = mapper.readTree(organizationsJsonMissingData);

    FrontendTermedServiceTest() throws JsonProcessingException {
    }

    @Test
    public void testReturnsGroupsOrderedInFiWhenUnknownLanguage() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Asuminen\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Järjestys\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Group, "random_string");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedInFi() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Asuminen\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Järjestys\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Group, "fi");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedInEn() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Housing\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Public order\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Services for families\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Group, "en");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInFiWhenUnknownLanguages() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "random_string");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInFi() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Yhteentoimivuusalustan yllapito\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "fi");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedInEn() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Test-organization\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNode);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "en");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsOrganizationsOrderedWhenMissingLang() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"321\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Interoperability platform developers\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"654\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Testi-organisaatio (fi)\", \"regex\":\"(?s)^.*$\"}" +
                "}}" +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initOrgsNodeMissing);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "en");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void testReturnsGroupsOrderedWhenMissingLang() throws JsonProcessingException {
        String jsonData = "[" +
                "{\"id\": \"789\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Asuminen\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"123\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"fi\", \"value\": \"Perheiden palvelut\", \"regex\":\"(?s)^.*$\"}" +
                "}}," +
                "{\"id\": \"456\"," +
                "\"properties\": " +
                "{ \"prefLabel\": " +
                "{ \"lang\": \"en\", \"value\": \"Public order (en)\", \"regex\":\"(?s)^.*$\"}" +
                "}} " +
                "]";
        JsonNode expectedNode = mapper.readTree(jsonData);

        when(termedRequester.exchange(eq("/node-trees"), eq(HttpMethod.GET), any(Parameters.class), eq(JsonNode.class))).thenReturn(initGroupsNodeMissing);

        JsonNode gotten = frontEndTermedService.getNodeListWithoutReferencesOrReferrersV2(NodeType.Organization, "fi");

        assertEquals(expectedNode, gotten);
    }

    @Test
    public void modifyValidStatusesShouldSucceed() {
        var graphId = UUID.fromString("0973d8f3-a129-4545-8d0d-d452e8acbc55");
        var oldStatus = "DRAFT";

        doReturn(true)
                .when(this.authorizationManager)
                .canModifyAllGraphs(any());

        frontEndTermedService.modifyStatuses(
                graphId,
                new HashSet<>(Arrays.asList("Concept", "Term")),
                oldStatus,
                "VALID");

        ArgumentCaptor<Map<String, Map<String, List<Map<String, String>>>>> propertiesCaptor =
                ArgumentCaptor.forClass(Map.class);

        // verify termed call to patch Concepts
        verify(this.termedRequester).exchange(
                eq("/graphs/" + graphId + "/types/Concept/nodes"),
                eq(HttpMethod.PATCH),
                argThat(i -> i.toString().equals(
                        "?append=false&where=properties.status:" + oldStatus)),
                eq(String.class),
                propertiesCaptor.capture());

        // verify termed call to patch Terms
        verify(this.termedRequester).exchange(
                eq("/graphs/" + graphId + "/types/Term/nodes"),
                eq(HttpMethod.PATCH),
                argThat(i -> i.toString().equals(
                        "?append=false&where=properties.status:" + oldStatus)),
                eq(String.class),
                propertiesCaptor.capture());

        // verify properties sent to termed
        var capturedProperties = propertiesCaptor
                .getAllValues()
                .stream()
                .map(properties -> properties
                        .get("properties")
                        .get("status")
                        .get(0)
                        .get("value"))
                .collect(Collectors.toList())
                .toArray();
        assertArrayEquals(
                new String[] { "VALID", "VALID" },
                capturedProperties);
    }

    @ParameterizedTest
    @CsvSource({ "FOO,VALID", "VALID,FOO" })
    public void modifyInvalidStatusesShouldFail(String oldStatus, String newStatus) {
        var graphId = UUID.fromString("0973d8f3-a129-4545-8d0d-d452e8acbc55");

        doReturn(true)
                .when(this.authorizationManager)
                .canModifyAllGraphs(any());

        var ex = assertThrows(
                IllegalArgumentException.class, () ->
                        frontEndTermedService.modifyStatuses(
                                graphId,
                                new HashSet<>(Arrays.asList("Concept", "Term")),
                                oldStatus,
                                newStatus));

        assertTrue(ex.getMessage().matches("^Invalid (old|new)Status: FOO$"));

        verify(this.termedRequester, times(0)).exchange(
                any(),
                any(),
                any(),
                eq(String.class),
                anyMap());
    }

    @Test
    public void modifyInvalidTypesShouldFail() {
        var graphId = UUID.fromString("0973d8f3-a129-4545-8d0d-d452e8acbc55");

        doReturn(true)
                .when(this.authorizationManager)
                .canModifyAllGraphs(any());

        var ex = assertThrows(
                IllegalArgumentException.class, () ->
                        frontEndTermedService.modifyStatuses(
                                graphId,
                                new HashSet<>(Arrays.asList("Concept", "Foo")),
                                "DRAFT",
                                "VALID"));
        assertEquals("Invalid types: Concept, Foo", ex.getMessage());

        verify(this.termedRequester, times(0)).exchange(
                any(),
                any(),
                any(),
                eq(String.class),
                anyMap());
    }
}
