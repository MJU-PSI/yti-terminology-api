package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import static org.junit.Assert.*;
import fi.vm.yti.terminology.api.model.termed.Graph;

public class GraphTest {
//   String jsonString="{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\",\"code\":null,\"uri\":null,\"roles\":[],\"permissions\":{},\"properties\":{\"empty\":false}}";
   String jsonString="{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\",\"code\":null,\"uri\":null,\"roles\":[],\"permissions\":{\"empty\":true},\"properties\":{\"empty\":false}}";
//   String jsonString="{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\",\"code\":null,\"uri\":null,\"roles\":[],\"permissions\":{},\"properties\":{}}";

    private Graph gr =null;
    @org.junit.Before
    public void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        gr = mapper.readValue(jsonString,Graph.class);
        assertNotNull(gr);
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }
    @org.junit.Test
    public void emptyPermission() {
        System.out.println("test empty permissions");
/*        if(gr != null && gr != null ){
            System.out.println(" PrefLabel="+gr.getValue.getValue());
            assertEquals("esimerkki",gr.getPrefLabel().getValue());
        }
        else
            fail("PrefLabel test Failed");
            */
    }
}