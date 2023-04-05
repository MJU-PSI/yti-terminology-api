package fi.vm.yti.terminology.api.migration.task;

import org.springframework.stereotype.Component;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.ReferenceIndex;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;

/**
 * Migration for YTI-1106, classification->information domain and HomographNumber fix."
 */
@Component
public class V11_AddSearchTerm implements MigrationTask {

    private final MigrationService migrationService;
    private final ReferenceIndex referenceIndex;

    V11_AddSearchTerm(MigrationService migrationService, ReferenceIndex referenceIndex) {
        this.migrationService = migrationService;
        this.referenceIndex = referenceIndex;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            // Go  through text-attributes and add descriptions to known ids
            updateMeta(meta);
        });
    }

    /**
     * Update MetaNodes text- and reference-attributes with given description texts.
     * @param meta MetaNode to be updated
     * @return true if updated
     */
    boolean updateMeta(MetaNode meta){
        boolean rv = false;;
        // Print textAttributes

        String domainName=meta.getDomain().getId().name();
        if(domainName.equals("Concept")){
            // ReferenceAttributes
            updateReferenceMeta(meta);
        }
        return rv;
    }


    /**
     * Update Group reference-attribute labels
     * @param meta
     * @param attributeName
     * @return
     */
    boolean updateReferenceMeta(MetaNode meta){
        boolean rv = false;        
        meta.getReferenceAttributes().add(this.referenceIndex.searchTerm(meta.getDomain(), 19));
        return rv;
    }
}
