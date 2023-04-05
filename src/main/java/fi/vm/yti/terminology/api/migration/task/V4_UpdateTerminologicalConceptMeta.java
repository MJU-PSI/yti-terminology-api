package fi.vm.yti.terminology.api.migration.task;

import org.springframework.stereotype.Component;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;

@Component
public class V4_UpdateTerminologicalConceptMeta implements MigrationTask {

    private final MigrationService migrationService;
    private final AttributeIndex attributeIndex;

    V4_UpdateTerminologicalConceptMeta(MigrationService migrationService, AttributeIndex attributeIndex) {
        this.migrationService = migrationService;
        this.attributeIndex = attributeIndex;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {

            TypeId domain = meta.getDomain();
            if (meta.isOfType(NodeType.Concept)) {
                meta.addAttribute(this.attributeIndex.conceptScope(domain, 8));
            }
        });
    }
}
