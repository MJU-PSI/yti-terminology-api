package fi.vm.yti.terminology.api.model.integration;

import fi.vm.yti.terminology.api.model.termed.*;

import java.util.Objects;
import java.util.UUID;

public final class ConceptSuggestion {
/*
    {
        "prefLabel":{"fi":"esimerkki"},
        "definition":{"fi":"jotain"},
        "creator":"45778009-804c-4aba-a836-f5c911ea5ef1",
        "vocabulary":"55778009-804c-4aba-a836-f5c911ea5ef1",
        "uri":"http://uri.suomi.fi/terminology/kira/"
        "identifier":"e15c8009-804c-4aba-a836-f5c911ea5ef1"
    }
*/
    private Attribute prefLabel=null;
    private Attribute definition=null;
    private String creator=null;
    private UUID identifier=null;
    private String terminologyUri=null;
    private String uri=null;

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setTerminologyUri(String terminologyUri) {
        this.terminologyUri = terminologyUri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setIdentifier(UUID id) {
        this.identifier = id;
    }


  

    // Jackson constructor
    private ConceptSuggestion() {
        // Jackson constructor
        this(new Attribute("",""), new Attribute("",""));
    }

    public ConceptSuggestion(Attribute prefLabel, Attribute definition) {
        this(prefLabel, definition, "");
    }

    public ConceptSuggestion(Attribute preflabel, Attribute definition, String uri) {
        this.prefLabel = preflabel;
        this.definition = definition;
        this.uri = uri;
    }

    public static ConceptSuggestion placeholder() {
        return new ConceptSuggestion();
    }


    public Attribute getPrefLabel() {
        return prefLabel;
    }

    public Attribute getDefinition() {
        return definition;
    }

    public String getCreator() {
        return creator;
    }

    public String getTerminologyUri() {
        return terminologyUri;
    }

    public String getUri() {
        return uri;
    }

    public UUID getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ConceptSuggestion)) return false;
        final ConceptSuggestion that = (ConceptSuggestion) o;
        return Objects.equals(prefLabel, that.prefLabel) &&
            Objects.equals(terminologyUri, that.terminologyUri) &&
            Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefLabel, terminologyUri, uri);
    }
}
