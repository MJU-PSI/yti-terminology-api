package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.ExceptionHandlerAdvice;
import fi.vm.yti.terminology.api.model.termed.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
        "spring.cloud.config.import-check.enabled=false"
})
@WebMvcTest(controllers = FrontendController.class)
public class FrontEndControllerInternalNodeTests {


    public static final String TEMPLATE_GRAPH_ID = "61cf6bde-46e6-40bb-b465-9b2c66bf4ad8";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FrontendTermedService termedService;

    @MockBean
    private FrontendElasticSearchService elasticSearchService;

    @MockBean
    private FrontendGroupManagementService groupManagementService;

    @MockBean
    private AuthenticatedUserProvider userProvider;

    @Autowired
    private FrontendController frontendController;

    @MockBean
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(frontendController)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    public void shouldValidateAndCreate() throws Exception {
        var termNode = constructNodeWithType(NodeType.Term, constructTermProperties(), constructTermReferences());
        var conceptNode = constructNodeWithType(NodeType.Concept, constructConceptProperties(), constructConceptReferences());
        var nodes = new GenericDeleteAndSave(Collections.emptyList(), List.of(termNode, conceptNode));

        this.mvc
                .perform(post("/api/v1/frontend/modify")
                        .contentType("application/json")
                        .content(convertObjectToJsonString(nodes)))
                .andExpect(status().isOk());

        verify(termedService).bulkChange(any(GenericDeleteAndSave.class), anyBoolean());
        verifyNoMoreInteractions(this.termedService);
    }

    @ParameterizedTest
    @MethodSource("provideMissingData")
    public void shouldFailOnMissingData(GenericDeleteAndSave node) throws Exception {
        mvc.perform(post("/api/v1/frontend/validate")
                .contentType("application/json")
                .content(convertObjectToJsonString(node)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("Object validation failed"))
            .andExpect(jsonPath("$.details").exists());
        verifyNoMoreInteractions(termedService);
    }

    private static HashMap<String, List<Attribute>> constructTermProperties() {
        var properties = new HashMap<String, List<Attribute>>();
        properties.put("prefLabel", List.of(new Attribute("en", "test label")));
        properties.put("source", Collections.emptyList());
        properties.put("scope", singletonList(new Attribute("", "")));
        properties.put("termStyle", singletonList(new Attribute("", "")));
        properties.put("termFamily", singletonList(new Attribute("", "")));
        properties.put("termConjugation", singletonList(new Attribute("", "")));
        properties.put("termInfo", singletonList(new Attribute("", "")));
        properties.put("wordClass", singletonList(new Attribute("", "")));
        properties.put("termHomographNumber", singletonList(new Attribute("", "")));
        properties.put("editorialNote", Collections.emptyList());
        properties.put("draftComment", singletonList(new Attribute("", "")));
        properties.put("historyNote", singletonList(new Attribute("", "")));
        properties.put("changeNote", singletonList(new Attribute("", "")));
        properties.put("status", singletonList(new Attribute("", "DRAFT")));
        return properties;
    }

    private static HashMap<String, List<Identifier>> constructTermReferences() {
        return new HashMap<>();
    }

    private static HashMap<String, List<Attribute>> constructConceptProperties(){
        var properties = new HashMap<String, List<Attribute>>();
        properties.put("definition", Collections.emptyList());
        properties.put("note", Collections.emptyList());
        properties.put("editorialNote", Collections.emptyList());
        properties.put("conceptScope", singletonList(new Attribute("", "")));
        properties.put("conceptClass", singletonList(new Attribute("", "")));
        properties.put("wordClass", singletonList(new Attribute("", "")));
        properties.put("changeNote", singletonList(new Attribute("", "")));
        properties.put("historyNote", singletonList(new Attribute("", "")));
        properties.put("status", singletonList(new Attribute("", "DRAFT")));
        properties.put("notation", singletonList(new Attribute("", "")));
        properties.put("source", Collections.emptyList());
        properties.put("externalLink", singletonList(new Attribute("", "")));
        properties.put("subjectArea", singletonList(new Attribute("", "")));
        return properties;
    }

    private static HashMap<String, List<Identifier>> constructConceptReferences() {
        var references = new HashMap<String, List<Identifier>>();
        var prefLabelXl = new Identifier(
                UUID.randomUUID(),
                new TypeId(
                        NodeType.Term,
                        new GraphId(UUID.randomUUID())));
        references.put("prefLabelXl", singletonList(prefLabelXl));
        return references;
    }

    private static GenericNode constructNodeWithType(
            NodeType type,
            Map<String, List<Attribute>> properties,
            Map<String, List<Identifier>> references){
        TypeId typeid = new TypeId(type, new GraphId(UUID.fromString(TEMPLATE_GRAPH_ID)));
        return new GenericNode(typeid, properties, references);
    }

    private static Stream<Arguments> provideMissingData() {
        var args = new ArrayList<GenericDeleteAndSave>();

        // without properties
        var genericNode = constructNodeWithType(NodeType.Term, Collections.emptyMap(), constructTermReferences());
        args.add(new GenericDeleteAndSave(Collections.emptyList(), List.of(genericNode)));

        // without a prefLabel
        var properties = constructTermProperties();
        properties.remove("prefLabel");
        genericNode = constructNodeWithType(NodeType.Term, properties, constructTermReferences());
        args.add(new GenericDeleteAndSave(Collections.emptyList(), List.of(genericNode)));

        // with no status term node
        properties = constructTermProperties();
        properties.remove("status");
        genericNode = constructNodeWithType(NodeType.Term, properties, constructTermReferences());
        args.add(new GenericDeleteAndSave(Collections.emptyList(), List.of(genericNode)));

        // with no status term node
        properties = constructConceptProperties();
        properties.remove("status");
        genericNode = constructNodeWithType(NodeType.Concept, properties, constructConceptReferences());
        args.add(new GenericDeleteAndSave(Collections.emptyList(), List.of(genericNode)));

        // without a prefLabelXLReferences
        var references = constructConceptReferences();
        references.remove("prefLabelXl");
        genericNode = constructNodeWithType(NodeType.Concept, constructConceptProperties(), references);
        args.add(new GenericDeleteAndSave(Collections.emptyList(), List.of(genericNode)));

        return args.stream().map(Arguments::of);
    }

    private String convertObjectToJsonString(GenericDeleteAndSave node) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(node);
    }

}
