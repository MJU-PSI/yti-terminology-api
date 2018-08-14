package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.model.termed.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.merge;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.prefLabel;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.type;

/**
 * Migration for YTI-797, add descriptions to meta model and fix on typo in prefLabel.
 */
@Component
public class V5_UpdateTerminologicalDescriptionMeta implements MigrationTask {

    private final MigrationService migrationService;

    V5_UpdateTerminologicalDescriptionMeta(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {
            // Go  through text-attributes and add descriptions to known ids
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
        if(domainName.equals("TerminologicalVocabulary")){
            updateTextAttributeDescription(meta,"language","Kielet, joilla sanaston sisältöä toteutetaan");
            updateTextAttributeDescription(meta,"status","Sanastomäärittelyn valmiusastetta kuvaava tilatieto");
            updateTextAttributeDescription(meta,"description","Laaja kuvaus sanaston sisällöstä, kohderyhmästä yms.");
            updateTextAttributeDescription(meta,"prefLabel","Sanaston nimi, joka näkyy otsikossa ja hakutuloksissa");
            // ReferenceAttributes
            updateReferenceAttributeDescription(meta,"inGroup","Sanaston aihealue julkisten palvelujen luokituksen mukaan");
            updateReferenceAttributeDescription(meta,"contributor","Organisaatio, joka vastaa  sanaston  sisällöstä ja ylläpidosta");
        }
        if(domainName.equals("Collection")) {
            updateTextAttributeDescription(meta,"prefLabel","Käsitevalikoiman nimi valitsemallasi kielellä");
            updateTextAttributeDescription(meta,"definition","Kuvaus valikoiman käyttötarkoituksesta ja mitä käsitteitä valikoimaan kuuluu");
            // ReferenceAttributes
            updateReferenceAttributeDescription(meta,"broader","Laajempi käsitevalikoima, johon tämä valikoima liittyy");
            updateReferenceAttributeDescription(meta,"member","Käsite, joka on poimittu mukaan valikoimaan");
        }
        if(domainName.equals("Term")) {
            updateTextAttributeDescription(meta, "prefLabel", "Termin tekstimuotoinen kuvaus/nimi (merkkijono)");
            updateTextAttributeDescription(meta, "status", "Termin valmiusastetta kuvaava tila");
            // should we use term instead of käsite???'
            updateTextAttributeDescription(meta, "source", "Käsitteen määrittelyssä käytetyt lähteet");
            updateTextAttributeDescription(meta, "scope", "Ala jossa termi on käytössä");
            updateTextAttributeDescription(meta, "termStyle", "Tyylilaji (esim. puhekieli)");
            updateTextAttributeDescription(meta, "termFamily", "maskuliini/neutri/feminiini");
            updateTextAttributeDescription(meta, "termConjugation", "Yksikkö tai monikko");
            updateTextAttributeDescription(meta, "termEquivalency", "<,>,~");
            updateTextAttributeDescription(meta, "wordClass", "Merkitään, jos termi on eri  sanaluokasta kuin muunkieliset termit");
            updateTextAttributeDescription(meta, "editorialNote", "Ylläpitäjille tai kääntäjille tarkoitettu huomio");
            updateTextAttributeDescription(meta, "draftComment", "Luonnosvaiheessa näkyväksi tarkoitettu kommentti");
            updateTextAttributeDescription(meta, "historyNote", "Termin aiempi merkitys tai käyttö");
            updateTextAttributeDescription(meta, "changeNote", "Merkintä termiin tehdystä yksittäisestä muutoksesta");
            // ReferenceAttributes
        }
        if(domainName.equals("Concept")) {
            updateTextAttributeDescription(meta, "definition", "Kuvaa käsitteen sisällön ja erottaa sen muista käsitteistä");
            updateTextAttributeDescription(meta, "note", "Käsitteen käyttöön liittyviä yleisiä huomioita");
            updateTextAttributeDescription(meta, "editorialNote", "Ylläpitäjille tai kääntäjille tarkoitettu huomio");
            updateTextAttributeDescription(meta, "example", "Esimerkki käsitteen käytöstä");
            updateTextAttributeDescription(meta, "conceptScope", "Ala, jossa käsite on käytössä");
            updateTextAttributeDescription(meta, "status", "Käsitteen valmiusastetta kuvaava tila");
            updateTextAttributeDescription(meta, "source", "Käsitteen määrittelyssä käytetyt  lähteet");
            updateTextAttributeDescription(meta, "wordClass", "Merkitään tarvittaessa käsitteelle, jos se on adjektiivi tai verbi");
            updateTextAttributeDescription(meta, "changeNote", "Merkintä käsitteeseen tehdystä yksittäisestä muutoksesta");
            updateTextAttributeDescription(meta, "historyNote", "Käsitteen aiempi merkitys tai käyttö");
            updateNotationTextAttribute(meta, "notation", "Merkintä, jolla käsitteet voidaan jäsentää eri järjestykseen tai joukkoihin");

            // ReferenceAttributes
            updateReferenceAttributeDescription(meta, "prefLabelXl", "Termi, joka on sopivin kuvaamaan kyseistä käsitettä");
            updateReferenceAttributeDescription(meta, "altLabelXl", "Termi jolla on (lähes) sama merkitys kuin tällä termillä");
            updateReferenceAttributeDescription(meta, "notRecommendedSynonym", "Termi, joka ei ole kielellisesti hyvä");
            updateReferenceAttributeDescription(meta, "hiddenTerm", "Termi, jolla halutaan ohjata käyttämään toista termiä");
            updateReferenceAttributeDescription(meta, "broader", "Laajempi käsite, johon tämä käsite liittyy");
            updateReferenceAttributeDescription(meta, "relatedConcept", "Käsite jota voidaan käyttää tämän käsitteen sijaan");
            updateReferenceAttributeDescription(meta, "related", "Käsite joka liittyy tähän käsitteeseen");
            updateReferenceAttributeDescription(meta, "isPartOf", "Käsite, johon tämä käsite kuuluu (on osa)");
            updateReferenceAttributeDescription(meta, "relatedMatch", "Käsite, joka liittyy tähän käsitteeseen, mutta eri sanastossa");
            updateReferenceAttributeDescription(meta, "exactMatch", "Käsite, jota voidaan käyttää tämän käsitteen sijaan");
            updateReferenceAttributeDescription(meta, "closeMatch", "Osittain tätä käsitettä vastaava käsite, mutta niiden käyttöala on eri");
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
    boolean updateTextAttributeDescription(MetaNode meta, String attributeName, String description){
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
                    att.updateProperties(PropertyUtil.merge(att.getProperties(),PropertyUtil.description(description)));
                    rv = true;
                }
            }
        }
        return rv;
    }

    /**
     * Update individual description for reference-attributes
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
    boolean updateReferenceAttributeDescription(MetaNode meta, String attributeName, String description){
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
                    att.updateProperties(PropertyUtil.merge(att.getProperties(),PropertyUtil.description(description)));
                    rv = true;
                }
            }
        }
        return rv;
    }

    /**
     * Update concept textAttribute named notation. Fix type in  button label and add description
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
    boolean updateNotationTextAttribute(MetaNode meta, String attributeName, String description){
        boolean rv = false;
        // Add description
        rv = updateTextAttributeDescription(meta, "notation", "Merkintä, jolla käsitteet voidaan jäsentää eri järjestykseen tai joukkoihin");

        // Fix notatation- string from prefLabel

        List<AttributeMeta> ta = meta.getTextAttributes().stream().filter(item -> item.getId().equals("notation")).collect(Collectors.toList());
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
                    att.updateProperties(PropertyUtil.merge(att.getProperties(),PropertyUtil.prefLabel("Systemaattinen merkintätapa","Notation")));
                    rv = true;
                }
            }
        }
        return rv;
    }
}
