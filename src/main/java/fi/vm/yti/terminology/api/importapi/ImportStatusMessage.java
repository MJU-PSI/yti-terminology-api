package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.util.JsonUtils;

import java.io.IOException;

// Don't marshall null values
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportStatusMessage {
    ObjectMapper mapper = new ObjectMapper();
    /**
     * Arbitrary identifier about the entity the message is about. Maybe null for,
     * e.g., incorrect file format or other general errors.
     */
    String targetIdentifier = null;
    /**
     * At least somewhat human readable message.
     */
    String message = null;

    ImportStatusMessage() {
    }

    ImportStatusMessage(String identifier, String explanation) {
        targetIdentifier = identifier;
        message = explanation;
    }

    public String getTargetIdentifier(){
        return this.targetIdentifier;
    }

    public void setTargetIdentifier(String identifier){
        this.targetIdentifier = identifier;
    }

    public String getMessage(){
        return message;
    }
    public  void setMessage(String message){
        this.message = message;
    }

    public static ImportStatusMessage fromString(String objStr) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(objStr, ImportStatusMessage.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}
