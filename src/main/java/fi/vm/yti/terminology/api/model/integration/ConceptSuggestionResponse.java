package fi.vm.yti.terminology.api.model.integration;

import java.util.Date;
import java.util.Objects;

import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.annotation.JsonFormat;

public final class ConceptSuggestionResponse {
    /*
     * { "prefLabel":{"fi":"esimerkki"}, "definition":{"fi":"jotain"},
     * "creator":"45778009-804c-4aba-a836-f5c911ea5ef1",
     * "vocabulary":"55778009-804c-4aba-a836-f5c911ea5ef1",
     * "uri":"http://uri.suomi.fi/terminology/kira/"
     * "identifier":"e15c8009-804c-4aba-a836-f5c911ea5ef1" }
     */
    private LocalizedString prefLabel = null;
    private LocalizedString definition = null;
    private String creator = null;
    private String terminologyUri = null;
    private String uri = null;
    // '2019-09-17T09:54:30.139'
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date created;

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setTerminologyUri(String terminologyUri) {
        this.terminologyUri = terminologyUri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public ConceptSuggestionResponse() {
        // Jackson constructor
        this(new LocalizedString("", ""), new LocalizedString("", ""));
    }

    public ConceptSuggestionResponse(LocalizedString prefLabel, LocalizedString definition) {
        this(prefLabel, definition, "");
    }

    public ConceptSuggestionResponse(LocalizedString prefLabel, LocalizedString definition, String uri) {
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.uri = uri;
    }

    public static ConceptSuggestionResponse placeholder() {
        return new ConceptSuggestionResponse();
    }

    public LocalizedString getPrefLabel() {
        return prefLabel;
    }

    public LocalizedString getDefinition() {
        return definition;
    }

    public String getCreator() {
        return HtmlUtils.htmlEscape(creator);
    }

    public String getTerminologyUri() {
        return HtmlUtils.htmlEscape(terminologyUri);
    }

    public String getUri() {
        return HtmlUtils.htmlEscape(uri);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConceptSuggestionResponse))
            return false;
        final ConceptSuggestionResponse that = (ConceptSuggestionResponse) o;
        return Objects.equals(prefLabel, that.prefLabel) &&
            Objects.equals(terminologyUri, that.terminologyUri) &&
            Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefLabel, terminologyUri, uri);
    }

    /**
     * @param prefLabel the prefLabel to set
     */
    public void setPrefLabel(LocalizedString prefLabel) {
        this.prefLabel = prefLabel;
    }

    /**
     * @param definition the definition to set
     */
    public void setDefinition(LocalizedString definition) {
        this.definition = definition;
    }

    /**
     * @return Date return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }
}
