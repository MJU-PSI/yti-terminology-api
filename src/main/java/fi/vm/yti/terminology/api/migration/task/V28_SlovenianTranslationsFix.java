package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.model.termed.*;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class V28_SlovenianTranslationsFix implements MigrationTask {

    private final MigrationService migrationService;

    V28_SlovenianTranslationsFix(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            // Go through text-attributes and add descriptions to known ids
            updateMeta(meta);
        });
    }

    /**
     * Update MetaNodes text- and reference-attributes with slovenian translations
     * 
     * @param meta MetaNode to be updated
     * @return true if updated
     */
    boolean updateMeta(MetaNode meta) {
        boolean rv = false;

        String domainName = meta.getDomain().getId().name();
        if (domainName.equals("Concept")) {
            Property property = new Property("sl",
                    "Pojem, ki je konceptualno ožji od tega pojma in vsebuje semantični pomen tega pojma");
            updateReferenceAttributeDescription(meta, "narrower", property);

            property = new Property("en",
                    "A concept that is conceptually narrower than this concept and contains the semantic meaning of this concept");
            updateReferenceAttributeDescription(meta, "narrower", property);

            property = new Property("sl", "Pojem, ki je enak vendar sta področje ali obseg uporabe različna");
            updateReferenceAttributeDescription(meta, "closeMatch", property);

            property = new Property("sl", "Širši pojem, ki vključuje ta pojem");
            updateReferenceAttributeDescription(meta, "isPartOf", property);

            property = new Property("en", "A broader concept that includes this concept");
            updateReferenceAttributeDescription(meta, "isPartOf", property);

            property = new Property("sl", "Sorodni pojem v drugem besedišču");
            updateReferenceAttributeDescription(meta, "relatedMatch", property);

            property = new Property("sl", "Enak pojem v drugem besedišču");
            updateReferenceAttributePrefLabel(meta, "exactMatch", property);
            property = new Property("sl", "Natančna kopija tega pojma v drugem besedišču");
            updateReferenceAttributeDescription(meta, "exactMatch", property);
        }

        return rv;
    }

    /**
     * Update individual prefLabels for reference-attributes
     * 
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
    boolean updateReferenceAttributePrefLabel(MetaNode meta, String attributeName, Property property) {
        boolean rv = false;
        // Print textAttributes
        List<ReferenceMeta> ra = meta.getReferenceAttributes().stream()
                .filter(item -> item.getId().equals(attributeName)).collect(Collectors.toList());
        if (!ra.isEmpty()) {
            if (ra.size() > 1) {
                System.err.println("Error, several " + attributeName + " attributes in same node ");
            } else {
                ReferenceMeta att = ra.get(0);
                // check if description exist and add new one
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.prefLabel(property)));
                rv = true;
            }
        }
        return rv;
    }

    /**
     * Update individual descriptions for reference-attributes
     * 
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
    boolean updateReferenceAttributeDescription(MetaNode meta, String attributeName, Property property) {
        boolean rv = false;
        // Print textAttributes
        List<ReferenceMeta> ra = meta.getReferenceAttributes().stream()
                .filter(item -> item.getId().equals(attributeName)).collect(Collectors.toList());
        if (!ra.isEmpty()) {
            if (ra.size() > 1) {
                System.err.println("Error, several " + attributeName + " attributes in same node ");
            } else {
                ReferenceMeta att = ra.get(0);
                // check if description exist and add new one
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.description(property)));
                rv = true;
            }
        }
        return rv;
    }
}
