package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import static org.junit.Assert.*;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.util.JsonUtils;

public class GenericNodeTest {

//    String jsonString = "{\"id\":\"6a9b0937-f9f7-4043-a8b6-d941deb15ac0\",\"type\":{\"id\":\"Schema\",\"graph\":{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\"},\"graphId\":\"9d9d546a-221f-44ed-b047-481653eb3192\"},\"code\":null,\"uri\":null,\"number\":1,\"createdBy\":\"admin\",\"createdDate\":\"2018-05-04T09:23:35.044+0000\",\"lastModifiedBy\":\"admin\",\"lastModifiedDate\":\"2018-12-19T13:52:36.639+0000\",\"properties\":{\"empty\":false},\"references\":{\"empty\":true},\"referrers\":{\"empty\":true},\"typeGraphId\":\"9d9d546a-221f-44ed-b047-481653eb3192\",\"typeId\":\"Schema\",\"typeGraph\":{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\"}}";
    String jsonString = "{\"id\":\"6a9b0937-f9f7-4043-a8b6-d941deb15ac0\",\"type\":{\"id\":\"Schema\",\"graph\":{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\"},\"graphId\":\"9d9d546a-221f-44ed-b047-481653eb3192\"},\"code\":null,\"uri\":null,\"number\":1,\"createdBy\":\"admin\",\"createdDate\":\"2018-05-04T09:23:35.044+0000\",\"lastModifiedBy\":\"admin\",\"lastModifiedDate\":\"2018-12-19T13:52:36.639+0000\",\"properties\":{},\"references\":{},\"referrers\":{},\"typeGraphId\":\"9d9d546a-221f-44ed-b047-481653eb3192\",\"typeId\":\"Schema\",\"typeGraph\":{\"id\":\"9d9d546a-221f-44ed-b047-481653eb3192\"}}";
    private GenericNode gn = null;

    @org.junit.Before
    public void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("demarshall "+jsonString);
        gn = mapper.readValue(jsonString, GenericNode.class);
        assertNotNull(gn);
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void emptyPermission() {
        System.out.println("test empty attributes");
        /*
         * if(gr != null && gr != null ){
         * System.out.println(" PrefLabel="+gr.getValue.getValue());
         * assertEquals("esimerkki",gr.getPrefLabel().getValue()); } else
         * fail("PrefLabel test Failed");
         */
    }
}