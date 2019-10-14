package fi.vm.yti.terminology.api.model.integration;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

public class ConceptSuggestionTest {
   String jsonString="{\"prefLabel\":{\"lang\":\"fi\",\"value\":\"esimerkki\"},\"definition\":{\"lang\":\"fi\",\"value\":\"jotain\"},\"creator\":\"45778009-804c-4aba-a836-f5c911ea5ef1\",\"terminologyUri\":\"http://uri.suomi.fi/terminology/kira/\",\"uri\":\"http://uri.suomi.fi/terminology/kira/\", \"createdDate\":\"20191015\"}";

    private ConceptSuggestion cs =null;
    @org.junit.Before
    public void setUp() throws Exception {
        System.out.println("incoming"+jsonString);
        ObjectMapper mapper = new ObjectMapper();
        cs = mapper.readValue(jsonString,ConceptSuggestion.class);
        assertNotNull(cs);
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void getPrefLabel() {
        System.out.println("test getPrefLabel");
        if(cs != null && cs.getPrefLabel() != null ){
            System.out.println(" PrefLabel="+cs.getPrefLabel().getValue());
            assertEquals("esimerkki",cs.getPrefLabel().getValue());
        }
        else
            fail("PrefLabel test Failed");
    }

    @org.junit.Test
    public void getDefinition() {
        System.out.println("test getDefinitionLabel");
        if(cs != null && cs.getDefinition() != null ){
            System.out.println(" Definition value="+cs.getDefinition().getValue());
            assertEquals("esimerkki",cs.getPrefLabel().getValue());
        }
        else
            fail("GetDefinition test Failed");
    }

    @org.junit.Test
    public void getCreator() {
        System.out.println("test getCreator UUID");
        if(cs != null && cs.getCreator() != null ){
            System.out.println(" Creator UUID value="+cs.getCreator().toString());
            assertEquals("45778009-804c-4aba-a836-f5c911ea5ef1",cs.getCreator().toString());
        }
        else
            fail("GetCreator test Failed");
    }

    @org.junit.Test
    public void getterminologyUri() {
        if(cs != null && cs.getTerminologyUri() != null ){
            assertEquals("http://uri.suomi.fi/terminology/kira/",cs.getTerminologyUri());
        }
        else
            fail("GetTerminologyUri test Failed");
    }

    @org.junit.Test
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