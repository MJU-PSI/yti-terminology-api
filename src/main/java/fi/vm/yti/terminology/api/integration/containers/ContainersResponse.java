
package fi.vm.yti.terminology.api.integration.containers;

import java.io.Serializable;
import java.util.List;

import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import fi.vm.yti.terminology.api.util.JsonUtils;

@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({ "uri", "prefLabel", "description", "status", "modified" })
public class ContainersResponse implements Serializable {

    @JsonProperty("uri")
    private String uri = null;
    @JsonProperty("prefLabel")
    @Valid
    private PrefLabel prefLabel = null;
    @JsonProperty("description")
    @JsonInclude(Include.NON_EMPTY)
    @Valid
    private Description description = null;
    @JsonProperty("status")
    private String status = null;
    @JsonProperty("modified")
    private String modified = null;

    private List<String> language = null;

    private final static long serialVersionUID = 306028529823257143L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ContainersResponse() {
    }

    /**
     * 
     * @param status
     * @param description
     * @param prefLabel
     * @param uri
     * @param modified
     */
    public ContainersResponse(String uri, PrefLabel prefLabel, Description description, String status, String modified) {
        super();
        this.uri = uri;
        this.prefLabel = prefLabel;
        this.description = description;
        this.status = status;
        this.modified = modified;
    }

    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }

    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    @JsonProperty("prefLabel")
    public PrefLabel getPrefLabel() {
        return prefLabel;
    }

    @JsonProperty("prefLabel")
    public void setPrefLabel(PrefLabel prefLabel) {
        this.prefLabel = prefLabel;
    }

    @JsonProperty("description")
    public Description getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(Description description) {
        this.description = description;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("modified")
    public String getModified() {
        return modified;
    }

    @JsonProperty("modified")
    public void setModified(String modified) {
        this.modified = modified;
    }

    @JsonProperty("language")
    public List<String> getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(List<String> language) {
        this.language = language;
    }

    @Override
    public String toString() {
        String value = "{\"uri\":\""+this.uri+"\","+
        "\"language\":\""+this.language+"\","+
        "\"prefLabel\":\""+this.prefLabel+"\","+
        "\"description\":\""+this.description+"\","+ 
        "\"status\":\""+this.status+"\","+
        "\"modified\":\""+this.modified+"\"}";     
        return value;
    }
}
