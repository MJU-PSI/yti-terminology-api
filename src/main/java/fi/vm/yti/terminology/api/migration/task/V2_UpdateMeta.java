package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V2_UpdateMeta implements MigrationTask {

    private final MigrationService migrationService;

    V2_UpdateMeta(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {

        migrationService.changeMetaLabel(
                VocabularyNodeType.TerminologicalVocabulary,
                NodeType.Concept,
                "isPartOf",
                "Koostumussuhteinen yläkäsite",
                "Is part of concept");
    }
}
