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
    private final AttributeIndex attributeIndex;
    private final ReferenceIndex referenceIndex;

    V2_UpdateTerminologicalVocabularyMeta(
        MigrationService migrationService, 
        AttributeIndex attributeIndex,
        ReferenceIndex referenceIndex
    ) {
        this.migrationService = migrationService;
        this.attributeIndex = attributeIndex;
        this.referenceIndex = referenceIndex;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {

            TypeId domain = meta.getDomain();

            if (meta.isOfType(NodeType.Concept)) {

                meta.addAttribute(this.attributeIndex.conceptWordClass(domain, 6));

                meta.addReference(this.referenceIndex.notRecommendedSynonym(domain, 2));
                meta.addReference(this.referenceIndex.hiddenTerm(domain, 3));

                meta.getReference("isPartOf")
                        .updateLabel("Koostumussuhteinen yläkäsite", "Is part of concept");

            } else if (meta.isOfType(NodeType.Term)) {

                meta.addAttribute(this.attributeIndex.termStyle(domain, 3));
                meta.addAttribute(this.attributeIndex.termFamily(domain, 4));
                meta.addAttribute(this.attributeIndex.termConjugation(domain, 5));
                meta.addAttribute(this.attributeIndex.termEquivalency(domain, 6));
                meta.addAttribute(this.attributeIndex.termWordClass(domain, 7));

            } else if (meta.isOfType(NodeType.TerminologicalVocabulary)) {

                meta.changeAttributeIndex("description", 1);
            }
        });
    }
}
