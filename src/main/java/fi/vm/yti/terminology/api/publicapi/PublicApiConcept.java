package fi.vm.yti.terminology.api.publicapi;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PublicApiConcept {

    private UUID id;
    private Map<String, List<String>> label;

    public PublicApiConcept(UUID id,
                    Map<String, List<String>> label) {
        this.id = id;
        this.label = label;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, List<String>> getLabel() {
        return label;
    }

    public void setLabel(Map<String, List<String>> label) {
        this.label = label;
    }
}
