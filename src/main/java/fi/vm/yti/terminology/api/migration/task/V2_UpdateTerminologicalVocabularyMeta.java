package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.ReferenceIndex;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V2_UpdateTerminologicalVocabularyMeta implements MigrationTask {

    private final MigrationService migrationService;

    V2_UpdateTerminologicalVocabularyMeta(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {

            TypeId domain = meta.getDomain();

            if (meta.isOfType(NodeType.Concept)) {

                meta.addAttribute(AttributeIndex.conceptWordClass(domain, 6));

                meta.addReference(ReferenceIndex.notRecommendedSynonym(domain, 2));
                meta.addReference(ReferenceIndex.hiddenTerm(domain, 3));

                meta.getReference("isPartOf")
                        .updateLabel("Koostumussuhteinen yläkäsite", "Is part of concept");

            } else if (meta.isOfType(NodeType.Term)) {

                meta.addAttribute(AttributeIndex.termStyle(domain, 3));
                meta.addAttribute(AttributeIndex.termFamily(domain, 4));
                meta.addAttribute(AttributeIndex.termConjugation(domain, 5));
                meta.addAttribute(AttributeIndex.termEquivalency(domain, 6));
                meta.addAttribute(AttributeIndex.termWordClass(domain, 7));
            }
        });
    }
}
