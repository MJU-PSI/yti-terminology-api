package fi.vm.yti.terminology.api.importapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

// Don't marshall null values
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportStatusResponse {

    // Enum states for status
    public enum Status {
        QUEUED, PREPROCESSING, PROCESSING, POSTPROCESSING, SUCCESS, SUCCESS_WITH_ERRORS, FAILURE, NOT_FOUND
    };

    /**
     * Current job status. This is not required to go through all phases, but is
     * required to end up in either SUCCESS or FAILURE.
     */
    Status status;
    /**
     * For phase PROCESSING the current progress, between 0 and processingTotal.
     * Maybe null, meaning "unknoen". Unspecified for other phases.
     */
    int processingProgress;
    /**
     * For phase PROCESSING the total goal. May change during processing. Must be
     * given if processingProgress is given. Unspecified for other phases.
     */
    int processingTotal;

    /**
     * Total number of created entities. May differ from processingTotal for various
     * reasons, e.g., different level of abstraction, dropped (unsupported)
     * entities, and erroneous entities (resultsError).
     */
    int resultsCreated;
    /**
     * Total number of generated warnings. These most likely concern the created
     * entities, but indicate that the source data did not meet requirements.
     */
    int resultsWarning;
    /**
     * Total number of generated errors. These should relate to erroneous entities
     * which could not be created, although error number is not required to match
     * entity count.
     */
    int resultsError;

    /**
     * Actual human readable explanation message
     */
    List<ImportStatusMessage> statusMessages = new ArrayList<>();

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<ImportStatusMessage> getStatusMessage() {
        return statusMessages;
    }

    public void setStatusMessage(ImportStatusMessage statusMessage) {
        this.statusMessages.add( statusMessage);
    }

    public void clearStatusMessages() {
        this.statusMessages.clear();
    }

    public int getProgress() {
        return processingProgress;

    }

    public void setProgress(int progress) {
        this.processingProgress = progress;
    }

    public int getProcessingTotal() {
        return processingTotal;
    }

    public void setProcessingTotal(int total) {
        this.processingTotal = total;
    }

    public int getResultsError() {
        return resultsError;
    }
    public void setSesultsError(int total) {
        this.resultsError = total;
    }

    public int getResultsWarning() {
        return resultsWarning;
    }
    public void setResultsWarning(int total) {
        this.resultsError = total;
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static ImportStatusResponse fromString(String objStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(objStr, ImportStatusResponse.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}
