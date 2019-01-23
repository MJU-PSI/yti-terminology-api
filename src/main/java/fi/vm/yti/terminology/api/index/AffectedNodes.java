package fi.vm.yti.terminology.api.index;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.unmodifiableList;

final class AffectedNodes {

    private final UUID graphId;
    private final List<UUID> vocabularyIds;
    private final List<UUID> conceptsIds;

    AffectedNodes(@NotNull UUID graphId, @NotNull List<UUID> vocabularyIds, @NotNull List<UUID> conceptsIds) {
        this.graphId = graphId;
        this.vocabularyIds = vocabularyIds;
        this.conceptsIds = conceptsIds;
    }

    @NotNull UUID getGraphId() {
        return graphId;
    }

    @NotNull List<UUID> getConceptsIds() {
        return unmodifiableList(conceptsIds);
    }

    @NotNull List<UUID> getVocabularyIds() {
        return unmodifiableList(vocabularyIds);
    }

    boolean hasVocabulary() {
        return !this.vocabularyIds.isEmpty();
    }
}
