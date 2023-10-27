package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.model.termed.*;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class V27_SlovenianTranslationsFix implements MigrationTask {

    private final MigrationService migrationService;

    V27_SlovenianTranslationsFix(MigrationService migrationService) {
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
      if (domainName.equals("TerminologicalVocabulary")) {
        Property property = new Property("sl", "Odgovorna organizacija");
        updateReferenceAttributePrefLabel(meta, "contributor", property);
      }

      if (domainName.equals("Concept")) {
        Property property = new Property("sl", "Nepriporočen sinonim");
        updateReferenceAttributePrefLabel(meta, "notRecommendedSynonym", property);
      }

      if (domainName.equals("Collection")) {
        Property property = new Property("sl", "Širša zbirka");
        updateReferenceAttributePrefLabel(meta, "broader", property);
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
}
