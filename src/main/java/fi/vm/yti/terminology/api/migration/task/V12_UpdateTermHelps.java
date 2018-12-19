package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.migration.ReferenceIndex;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.ReferenceMeta;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.Property;
import fi.vm.yti.terminology.api.model.termed.AttributeMeta;
import fi.vm.yti.terminology.api.model.termed.NodeType;

import fi.vm.yti.terminology.api.migration.DomainIndex;
import fi.vm.yti.terminology.api.migration.AttributeIndex;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Migration for YTI-1106, classification->information domain and HomographNumber fix."
 */
@Component
public class V12_UpdateTermHelps implements MigrationTask {

    private final MigrationService migrationService;

    V12_UpdateTermHelps(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
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

        String domainName=meta.getDomain().getId().name();
        if(domainName.equals("Concept")){
            // remove old and update new one
            ReferenceMeta ref = meta.getReference("hasPart");
            if(ref!= null ){
                System.out.println("hasPart ref="+ref);
                System.out.println("hasPart index="+ref.getIndex());
            }

            // ReferenceAttributes
            ReferenceMeta rm = meta.getReference("narrower");
            if(rm != null){
                rm.updateProperties(PropertyUtil.merge(rm.getProperties(),
                    PropertyUtil.description("Käsite, joka on hierarkkisessa suhteessa tähän käsitteeseen ja jonka sisältöön kuuluu tämän käsitteen sisältö",
                                             "Concept that is in hierarchical relation with this concept and contains semantic meaning of this concept")));
            }

            //  indeksi 11 pitäisi olla 7
//            updateReferenceAttributeDescription(meta, "hasPart",

            rm = meta.getReference("hasPart");
            if(rm != null){
                rm.updateProperties(PropertyUtil.merge(rm.getProperties(),PropertyUtil.description("Koostumussuhteinen käsite, joka vastaa kokonaisuuden osaa", "Narrower concept that is part of this concept")));
            }

            rm = meta.getReference("searchTerm");
            if(rm != null){
                rm.updateProperties(PropertyUtil.merge(rm.getProperties(),PropertyUtil.description("Termi, jolla käyttäjä ohjataan katsomaan tiettyyn käsitteeseen liitettyjä tietoja",
                "Term not designating the concept, but which may be useful for search or reference purposes")));                
            }
            List<ReferenceMeta>  ra=meta.getReferenceAttributes();            
            ra.forEach(o->{
                System.out.println("Name="+o.getId()+ " index="+o.getIndex()); 
            }); 
         }
         if(domainName.equals("Term")){
             // termInfo
            TypeId domain = meta.getDomain();
            if (meta.isOfType(NodeType.Term)) {
                meta.addAttribute(AttributeIndex.termInfo(domain, 7));
            }
            // Add info texts
            AttributeMeta att = meta.getAttribute("termInfo");
            att.updateProperties(PropertyUtil.merge(att.getProperties(),PropertyUtil.description("Lisätietoa termin käytöstä", "Additional information about the term")));
         }
        return rv;
    }


/**
     * Update individual description for reference-attributes
     * @param meta
     * @param attributeName
     * @param description (FI)
     * @param endescription (EN)
     * @return
     */
    boolean updateReferenceAttributeDescription(MetaNode meta, String attributeName, String description, String endescription ){
        boolean rv = false;
        // Print textAttributes
        List<ReferenceMeta> ra = meta.getReferenceAttributes().stream().filter(item -> item.getId().equals(attributeName)).collect(Collectors.toList());
        if(!ra.isEmpty()){
            if(ra.size()>1){
                System.err.println("Error, several "+ attributeName+" attributes in same node ");
            } else {
                ReferenceMeta att = ra.get(0);
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
 *
Hakutermi (searchTerm)
Termi, jolla käyttäjä ohjataan katsomaan tiettyyn käsitteeseen liitettyjä tietoja
Term not designating the concept, but which may be useful for search or reference purposes

 

Koostumussuhteinen alakäsite (hasPart) 
Koostumussuhteinen käsite, joka vastaa kokonaisuuden osaa
Narrower concept that is part of this concept
Järjestys: Koostumussuhteisen yläkäsitteen jälkeen (isPartOf index 18)

 

Hierarkkinen alakäsite (narrower)
Käsite, joka on hierarkkisessa suhteessa tähän käsitteeseen ja jonka sisältöön kuuluu tämän käsitteen sisältö
Concept that is in hierarchical relation with this concept and contains semantic meaning of this concept
Järjestys: Hierarkkisen yläkäsitteen jälkeen (BroaderConcept index 16)

 

Uusi attribuutti termille: Termin lisätieto
Nimi: Term info / Termin lisätieto
URI: http://uri.suomi.fi/datamodel/ns/st#termInfo
Määritelmä: Additional information about the term / Lisätietoa termin käytöstä
Järjestys: Termin vastaavuus jälkeen

 */
}
