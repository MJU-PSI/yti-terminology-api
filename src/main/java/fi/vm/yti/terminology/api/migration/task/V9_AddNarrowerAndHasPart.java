package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.migration.ReferenceIndex;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.ReferenceMeta;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;

import fi.vm.yti.terminology.api.migration.DomainIndex;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Migration for YTI-1106, classification->information domain and HomographNumber fix."
 */
@Component
public class V9_AddNarrowerAndHasPart implements MigrationTask {

    private final MigrationService migrationService;

    V9_AddNarrowerAndHasPart(MigrationService migrationService) {
        this.migrationService = migrationService;
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
            updateReferenceAttribute(meta,"narrower","Tietoalue","Information domain");
        }

        return rv;
    }


    /**
     * Update Group reference-attribute labels
     * @param meta
     * @param attributeName
     * @return
     */
    boolean updateReferenceAttribute(MetaNode meta, String attributeName,  String fival, String enval){
        boolean rv = false;
        TypeId domain = DomainIndex.TERMINOLOGICAL_CONCEPT_TEMPLATE_DOMAIN;

        List<ReferenceMeta> ra = meta.getReferenceAttributes().stream().filter(item -> item.getId().equals(attributeName)).collect(Collectors.toList());

        meta.getReferenceAttributes().add(ReferenceIndex.narrower(domain, 17, "Hierarkkinen alakäsite","Narrower concept"));
        meta.getReferenceAttributes().add(ReferenceIndex.hasPartConcept(domain, 18));
        return rv;
    }

}
