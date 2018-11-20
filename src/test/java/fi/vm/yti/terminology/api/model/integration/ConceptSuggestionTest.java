package fi.vm.yti.terminology.api.model.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.terminology.api.model.integration.ConceptSuggestion;
import static org.junit.Assert.*;

public class ConceptSuggestionTest {
   String jsonString="{\"prefLabel\":{\"lang\":\"fi\",\"value\":\"esimerkki\"},\"definition\":{\"lang\":\"fi\",\"value\":\"jotain\"},\"creator\":\"45778009-804c-4aba-a836-f5c911ea5ef1\",\"vocabulary\":\"55778009-804c-4aba-a836-f5c911ea5ef1\",\"uri\":\"http://uri.suomi.fi/terminology/kira/\"}";

    private ConceptSuggestion cs =null;
    @Before
    public void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        cs = mapper.readValue(jsonString,ConceptSuggestion.class);
        assertNotNull(cs);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getPrefLabel() {
        System.out.println("test getPrefLabel");
        if(cs != null && cs.getPrefLabel() != null ){
            System.out.println(" PrefLabel="+cs.getPrefLabel().getValue());
            assertEquals("esimerkki",cs.getPrefLabel().getValue());
        }
        else
            fail("PrefLabel test Failed");
    }

    @Test
    public void getDefinition() {
        System.out.println("test getDefinitionLabel");
        if(cs != null && cs.getDefinition() != null ){
            System.out.println(" Definition value="+cs.getDefinition().getValue());
            assertEquals("esimerkki",cs.getPrefLabel().getValue());
        }
        else
            fail("GetDefinition test Failed");
    }

    @Test
    public void getCreator() {
        System.out.println("test getCreator UUID");
        if(cs != null && cs.getCreator() != null ){
            System.out.println(" Creator UUID value="+cs.getCreator().toString());
            assertEquals("45778009-804c-4aba-a836-f5c911ea5ef1",cs.getCreator().toString());
        }
        else
            fail("GetCreator test Failed");
    }

    @Test
    public void getVocabulary() {
        if(cs != null && cs.getVocabulary() != null ){
            System.out.println(" Vocabularity UUID value="+cs.getVocabulary().toString());
            assertEquals("55778009-804c-4aba-a836-f5c911ea5ef1",cs.getVocabulary().toString());
        }
        else
            fail("GetVocabulary test Failed");
    }

    @Test
    public void getUri() {
        String expected="http://uri.suomi.fi/terminology/kira/";
        System.out.println("test getUri");
        if(cs != null && cs.getUri() != null ){
            System.out.println(" Uri value="+cs.getUri());
            assertEquals(expected,cs.getUri());
        }
        else
            fail("GetUri test Failed");
    }
}