package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V22_LinkAndSubjectAreaProperties implements MigrationTask  {
    private final MigrationService migrationService;
    private final AttributeIndex attributeIndex;

    V22_LinkAndSubjectAreaProperties(MigrationService migrationService, AttributeIndex attributeIndex) {
        this.migrationService = migrationService;
        this.attributeIndex = attributeIndex;
    }

    @Override
    public void migrate() {

        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            NodeType type = meta.getDomain().getId();

            if (type.equals(NodeType.Concept)) {
                meta.addAttribute(this.attributeIndex.externalLink(meta.getDomain(), 30));
                meta.addAttribute(this.attributeIndex.subjectArea(meta.getDomain(), 35));
            }

        });
    }
}
