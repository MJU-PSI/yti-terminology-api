package fi.vm.yti.terminology.api.index;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Collections.unmodifiableList;

final class AffectedNodes {

    private final String graphId;
    private final List<String> vocabularyIds;
    private final List<String> conceptsIds;

    AffectedNodes(@NotNull String graphId, @NotNull List<String> vocabularyIds, @NotNull List<String> conceptsIds) {
        this.graphId = graphId;
        this.vocabularyIds = vocabularyIds;
        this.conceptsIds = conceptsIds;
    }

    @NotNull String getGraphId() {
        return graphId;
    }

    @NotNull List<String> getConceptsIds() {
        return unmodifiableList(conceptsIds);
    }

    boolean hasVocabulary() {
        return !this.vocabularyIds.isEmpty();
    }
}
