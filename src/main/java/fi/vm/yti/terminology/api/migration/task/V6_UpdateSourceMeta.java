package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.model.termed.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Migration for YTI-797,  Update  source to accept multiple lines. ie type="string:multiple,area"
 */
@Component
public class V6_UpdateSourceMeta implements MigrationTask {

    private final MigrationService migrationService;
    private final AttributeIndex attributeIndex;

    V6_UpdateSourceMeta(MigrationService migrationService, AttributeIndex attributeIndex) {
        this.migrationService = migrationService;
        this.attributeIndex = attributeIndex;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            // Go  through text-attributes and add descriptions to known ids
            updateTypes(meta);
            updateDescriptions(meta);
        });
    }
    /**
     * Update MetaNodes text- and reference-attributes with given description texts.
     * @param meta MetaNode to be updated
     * @return true if updated
     */
    boolean updateDescriptions(MetaNode meta){
        boolean rv = false;;
        // Print textAttributes
        String domainName=meta.getDomain().getId().name();
        if(domainName.equals("Concept")) {
            updateTextAttributeDescription(meta, "conceptClass",
                    "Sanastokohtainen luokitus jolla voidaan ryhmitellä käsitteitä",
                    "Terminology specific classification for concept categorisation");
        }
        if(domainName.equals("Term")) {
            updateTextAttributeDescription(meta, "termHomographNumber",
                    "Numero joka kuvaa homonyymin järjestyksen.",
                    "A number indicating the position of the term within a sequenceof homographs");

        }
        return rv;
    }

    /**
     * Update individual description for text-attributes
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
    boolean updateTextAttributeDescription(MetaNode meta, String attributeName, String description,String endescription){
        boolean rv = false;
        // Print textAttributes
        List<AttributeMeta> ta = meta.getTextAttributes().stream().filter(item -> item.getId().equals(attributeName)).collect(Collectors.toList());
        if(!ta.isEmpty()){
            if(ta.size()>1){
                System.err.println("Error, several example attributes in same node ");
            } else {
                AttributeMeta att = ta.get(0);
                // check if description exist and add new one
                Property desc = new Property("fi", description);
                if (att.getProperties().get(desc)!=null) {
                    System.err.println("Description property exists already");
                } else {
                    att.updateProperties(PropertyUtil.merge(att.getProperties(),PropertyUtil.description(description, endescription)));
                    rv = true;
                }
            }
        }
        return rv;
    }

    /**
     * Update MetaNodes text-attribute named type with given value string.
     * @param meta MetaNode to be updated
     * @return true if updated
     */
    boolean updateTypes(MetaNode meta){
        boolean rv = false;;
        String domainName=meta.getDomain().getId().name();
        // Both concept and term nodes
        if(domainName.equals("Term")) {
            updateTextAttributeType(meta, "source", "string:multiple,area");
            addHomographNumber(meta);
        }
        if(domainName.equals("Concept")) {
            updateTextAttributeType(meta, "source", "string:multiple,area");
            // Add new category-text-attribute YTI-1056
            addCategory(meta);
        }
        return rv;
    }

    /**
     * Update type value
     * @param meta
     * @param attributeName
     * @param typeString
     * @return
     */
    boolean updateTextAttributeType(MetaNode meta, String attributeName, String typeString){
        boolean rv = false;
        List<AttributeMeta> ta = meta.getTextAttributes().stream().filter(item -> item.getId().equals(attributeName)).collect(Collectors.toList());
        if(!ta.isEmpty()){
            if(ta.size()>1){
                System.err.println("Error, several identical attributes in same node ");
            } else {
                AttributeMeta att = ta.get(0);
                att.updateProperties(PropertyUtil.merge(att.getProperties(),PropertyUtil.type(typeString)));
                rv = true;
            }
        }
        return rv;
    }

    private void addCategory(MetaNode meta){
        //Not implemented yet
        TypeId domain = meta.getDomain();
        if (meta.isOfType(NodeType.Concept)) {
            meta.addAttribute(this.attributeIndex.conceptClass(domain, 9));
        }
    }

    private void addHomographNumber(MetaNode meta){
        //Not implemented yet
        TypeId domain = meta.getDomain();
        if (meta.isOfType(NodeType.Term)) {
            meta.addAttribute(this.attributeIndex.termHomographNumber(domain, 8));
        }
    }
}
