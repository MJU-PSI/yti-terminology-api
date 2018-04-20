package fi.vm.yti.terminology.api.publicapi;

import java.util.HashMap;
import java.util.UUID;

public class PublicApiConcept {

    private UUID id;
    private HashMap<String, String> prefLabel;
    private HashMap<String, String> definition;
    private UUID vocabularyId;
    private String uri;
    private String url;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public HashMap<String, String> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(HashMap<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }

    public UUID getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(UUID vocabularyId) {
        this.vocabularyId = vocabularyId;
    }

    public HashMap<String, String> getDefinition() {
        return definition;
    }

    public void setDefinition(HashMap<String, String> definition) {
        this.definition = definition;
    }
}
