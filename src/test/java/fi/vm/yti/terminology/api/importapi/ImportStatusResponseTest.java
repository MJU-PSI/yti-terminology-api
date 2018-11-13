package fi.vm.yti.terminology.api.importapi;

import org.junit.Test;

import static org.junit.Assert.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImportStatusResponseTest {

    String jsonString = "{\"status\":\"SUCCESS_WITH_ERRORS\",\"processingTotal\":910,\"resultsWarning\":0,\"resultsError\":744,\"progress\":910,\"statusMessage\":[{\"targetIdentifier\":\"tmpOKSAID116\",\"message\":\"[Not matching reference found for :KatriSeppala]\"},{\"targetIdentifier\":\"tmpOKSAID117\",\"message\":\"[Not matching reference found for :KatriSeppala]\"},{\"targetIdentifier\":\"tmpOKSAID118\",\"message\":\"[Not matching reference found for :tr63]\"}]}";
    ObjectMapper mapper;

    @org.junit.Before
    public void setUp() throws Exception {
        System.out.println("Setup");
    }

    @org.junit.After
    public void tearDown() throws Exception {
        System.out.println("teardown");
    }

    @Test
    public void toStringTest() {
        ObjectMapper mapper = new ObjectMapper();
        ImportStatusResponse status=null;
        try {
        String result = mapper.writeValueAsString(status);
        System.out.println("toString() Result="+result);
        } catch(JsonProcessingException jex){
            jex.printStackTrace();
        }
    }

   @Test
    public void fromString() {
        // Read json
        System.out.println("test fromString: ");
        ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println("Map result from json: "+jsonString);
            ImportStatusResponse resp = mapper.readValue(jsonString,ImportStatusResponse.class);
        assertNotNull(resp);
        } catch(JsonMappingException jme){
            jme.printStackTrace();
        } catch(JsonProcessingException jex){
            jex.printStackTrace();
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
    }      
}