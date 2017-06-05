package fi.csc.termed.search.dto;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class NodeChanges {

    private final String graphId;
    private final List<String> vocabularyIds;
    private final List<String> conceptsIds;

    public NodeChanges(String graphId, List<String> vocabularyIds, List<String> conceptsIds) {
        this.graphId = graphId;
        this.vocabularyIds = vocabularyIds;
        this.conceptsIds = conceptsIds;
    }

    public String getGraphId() {
        return graphId;
    }

    public List<String> getConceptsIds() {
        return unmodifiableList(conceptsIds);
    }

    public boolean hasVocabulary() {
        return !this.vocabularyIds.isEmpty();
    }
}
