package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.Collections;
import java.util.Map;

public class CountDTO {

    private Map<String, Long> indices;
    private Map<String, Long> statuses;
    private Map<String, Long> groups;


    public CountDTO() {
        this.indices = Collections.emptyMap();
        this.statuses = Collections.emptyMap();
        this.groups = Collections.emptyMap();
    }

    public CountDTO(
            final Map<String, Long> indices,
            final Map<String, Long> statuses,
            final Map<String, Long> groups) {
        this.indices = indices;
        this.statuses = statuses;
        this.groups = groups;
    }

    public Map<String, Long> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, Long> statuses) {
        this.statuses = statuses;
    }

    public Map<String, Long> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Long> groups) {
        this.groups = groups;
    }

    public Map<String, Long> getIndices() {
        return indices;
    }

    public void setIndices(Map<String, Long> indices) {
        this.indices = indices;
    }
}
