package fi.csc.termed.api.dto;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class AffectedNodes {

    private final String graphId;
    private final List<String> vocabularyIds;
    private final List<String> conceptsIds;

    public AffectedNodes(@NotNull String graphId, @NotNull List<String> vocabularyIds, @NotNull List<String> conceptsIds) {
        this.graphId = graphId;
        this.vocabularyIds = vocabularyIds;
        this.conceptsIds = conceptsIds;
    }

    public @NotNull String getGraphId() {
        return graphId;
    }

    public @NotNull List<String> getConceptsIds() {
        return unmodifiableList(conceptsIds);
    }

    public boolean hasVocabulary() {
        return !this.vocabularyIds.isEmpty();
    }
}
