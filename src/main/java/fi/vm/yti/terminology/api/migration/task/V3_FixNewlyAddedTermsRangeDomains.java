package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import org.springframework.stereotype.Component;

@Component
public class V3_FixNewlyAddedTermsRangeDomains implements MigrationTask {

    private final MigrationService migrationService;

    V3_FixNewlyAddedTermsRangeDomains(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {

            TypeId domain = meta.getDomain();

            if (meta.isOfType(NodeType.Concept)) {

                TypeId termDomain = new TypeId(NodeType.Term, domain.getGraph());

                meta.getReference("notRecommendedSynonym").setRange(termDomain);
                meta.getReference("hiddenTerm").setRange(termDomain);
            }
        });
    }
}
