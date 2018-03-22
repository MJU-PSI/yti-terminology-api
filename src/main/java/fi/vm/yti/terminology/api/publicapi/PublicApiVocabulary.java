package fi.vm.yti.terminology.api.publicapi;

import java.util.HashMap;
import java.util.UUID;

public class PublicApiVocabulary {

    private UUID id;
    private HashMap<String, String> prefLabel;

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
}
