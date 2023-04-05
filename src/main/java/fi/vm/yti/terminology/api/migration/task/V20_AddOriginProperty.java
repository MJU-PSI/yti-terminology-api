package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V20_AddOriginProperty implements MigrationTask {

    private final MigrationService migrationService;
    private final AttributeIndex attributeIndex;

    V20_AddOriginProperty(MigrationService migrationService, AttributeIndex attributeIndex) {
        this.migrationService = migrationService;
        this.attributeIndex = attributeIndex;
    }

    @Override
    public void migrate() {

        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            NodeType type = meta.getDomain().getId();

            if (type.equals(NodeType.TerminologicalVocabulary)) {
                meta.addAttribute(this.attributeIndex.origin(meta.getDomain(), 21));
            }
        });
    }
}
