package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.PropertyUtil;
import fi.vm.yti.terminology.api.model.termed.*;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Migration for YTI-46.
 */
@Component
public class V24_SlovenianTranslations implements MigrationTask {

    private final MigrationService migrationService;

    V24_SlovenianTranslations(MigrationService migrationService) {
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

            Property property = new Property("sl", "Terminološki slovar");
            meta.updateProperties(PropertyUtil.prefLabel(property));

            updateTextAttributePrefLabel(meta, "prefLabel", new Property("sl", "Ime"));
            property = new Property("sl", "Ime terminologije, ki je prikazano v glavi in rezultatih iskanja");
            updateTextAttributeDescription(meta, "prefLabel", property);
            
            property = new Property("sl", "Opis");
            updateTextAttributePrefLabel(meta, "description", property);
            property = new Property("sl", "Opis terminologije in področja uporabe");
            updateTextAttributeDescription(meta, "description", property);

            property = new Property("sl", "Jezik");
            updateTextAttributePrefLabel(meta, "language", property);
            property = new Property("sl", "Jeziki, ki se uporabljajo za opredelitev terminologije");
            updateTextAttributeDescription(meta, "language", property);

            property = new Property("sl", "Status terminologije");
            updateTextAttributePrefLabel(meta, "status", property);
            property = new Property("sl", "Status terminologije");
            updateTextAttributeDescription(meta, "status", property);

            property = new Property("sl", "Kontakt");                    
            updateTextAttributePrefLabel(meta, "contact", property);
            property = new Property("sl", " Opis kanala za povratne informacije. Ne uporabljajte osebnih e-poštnih naslovov. Prednostna oblika je na primer: \"Delovna skupina terminologija: vzdrzevanje@example.org\"");
            updateTextAttributeDescription(meta, "contact", property);

            property = new Property("sl", "Prednostno ime");
            updateTextAttributePrefLabel(meta, "priority", property);

            property = new Property("sl", "Kopirano iz");
            updateTextAttributePrefLabel(meta, "origin", property);

            property = new Property("sl", "Tip terminologije");
            updateTextAttributePrefLabel(meta, "terminologyType", property);

            // reference attributes
            property = new Property("sl", "Prispeval");
            updateReferenceAttributePrefLabel(meta, "contributor", property);
            property = new Property("sl", "Organizacija, ki je odgovorna za vsebino in vzdrževanje terminologije");
            updateReferenceAttributeDescription(meta, "contributor", property);

            property = new Property("sl", "Informacijsko področje");                    
            updateReferenceAttributePrefLabel(meta, "inGroup", property);
            property = new Property("sl", "Informacijsko področje terminologije po klasifikaciji javnih storitev");
            updateReferenceAttributeDescription(meta, "inGroup", property);
        }

        if (domainName.equals("Collection")) {
            Property property = new Property("sl", "Collection-sl");
            meta.updateProperties(PropertyUtil.prefLabel(property));

            property = new Property("sl", "Ime");
            updateTextAttributePrefLabel(meta, "prefLabel", property);
            property = new Property("sl", "Ime zbirke");
            updateTextAttributeDescription(meta, "prefLabel", property);

            property = new Property("sl", "Definicija");
            updateTextAttributePrefLabel(meta, "definition", property);
            property = new Property("sl", "Opis primerov uporabe in vsebine zbirke");
            updateTextAttributeDescription(meta, "definition", property);

            // reference attributes
            property = new Property("sl", "Zbirka širša");
            updateReferenceAttributePrefLabel(meta, "broader", property);
            property = new Property("sl", "Širša zbirka, na katero se sklicuje ta zbirka");                    
            updateReferenceAttributeDescription(meta, "broader", property);

            property = new Property("sl", "Član");
            updateReferenceAttributePrefLabel(meta, "member", property);
            property = new Property("sl", "Pojem, ki je element te zbirke");                    
            updateReferenceAttributeDescription(meta, "member", property);
        }

        if (domainName.equals("Term")) {
            Property property = new Property("sl", "Termin");
            meta.updateProperties(PropertyUtil.prefLabel(property));

            property = new Property("sl", "Dobesedna vrednost izraza");
            updateTextAttributePrefLabel(meta, "prefLabel", property);
            property = new Property("sl", "Dobesedna vrednost izraza");
            updateTextAttributeDescription(meta, "prefLabel", property);

            property = new Property("sl", "Vir");
            updateTextAttributePrefLabel(meta, "source", property);
            property = new Property("sl", "Viri, ki so bili uporabljeni za definiranje pojma");
            updateTextAttributeDescription(meta, "source", property);

            property = new Property("sl", "Obseg");
            updateTextAttributePrefLabel(meta, "scope", property);
            property = new Property("sl", "Obseg ali področje, kjer se izraz uporablja");
            updateTextAttributeDescription(meta, "scope", property);

            property = new Property("sl", "Slog termina");
            updateTextAttributePrefLabel(meta, "termStyle", property);
            property = new Property("sl", "Slog termina (na primer pogovorni jezik)");
            updateTextAttributeDescription(meta, "termStyle", property);

            property = new Property("sl", "Družina termina");
            updateTextAttributePrefLabel(meta, "termFamily", property);
            property = new Property("sl", "moški/nevtralno/ženska");
            updateTextAttributeDescription(meta, "termFamily", property);

            property = new Property("sl", "Spregatev termina");
            updateTextAttributePrefLabel(meta, "termConjugation", property);
            property = new Property("sl", "Enota ali mno\u017Eina");
            updateTextAttributeDescription(meta, "termConjugation", property);

            property = new Property("sl", "Enakovrednost termina");
            updateTextAttributePrefLabel(meta, "termEquivalency", property);
            property = new Property("sl", "<,>,~");                    
            updateTextAttributeDescription(meta, "termEquivalency", property);

            property = new Property("sl", "Informacije o terminu");
            updateTextAttributePrefLabel(meta, "termInfo", property);
            property = new Property("sl", "Dodatne informacije o terminu");
            updateTextAttributeDescription(meta, "termInfo", property);

            property = new Property("sl", "Razred besede ");                    
            updateTextAttributePrefLabel(meta, "wordClass", property);
            property = new Property("sl", "Če je izraz iz drugega razreda kot izrazi v drugem jeziku, se uporablja razred besede");
            updateTextAttributeDescription(meta, "wordClass", property);

            property = new Property("sl", "Številka pomenskega razmerja med termini");                    
            updateTextAttributePrefLabel(meta, "termHomographNumber", property);
            property = new Property("sl", "Številka, ki označuje položaj izraza znotraj zaporedja pomenskega razmerja med termini");
            updateTextAttributeDescription(meta, "termHomographNumber", property);

            property = new Property("sl", "Opomba uredništva");
            updateTextAttributePrefLabel(meta, "editorialNote", property);
            property = new Property("sl", "Opomba, ki je namenjena prevajalcem ali vzdrževalcem");
            updateTextAttributeDescription(meta, "editorialNote", property);

            property = new Property("sl", "Osnutek komentarja");                    
            updateTextAttributePrefLabel(meta, "draftComment", property);
            property = new Property("sl", "Komentar, ki naj bi bil prikazan samo v fazi osnutka");
            updateTextAttributeDescription(meta, "draftComment", property);

            property = new Property("sl", "Zgodovinska opomba");                    
            updateTextAttributePrefLabel(meta, "historyNote", property);
            property = new Property("sl", "Opis predhodne uporabe ali zgodovine izraza");
            updateTextAttributeDescription(meta, "historyNote", property);

            property = new Property("sl", "Opomba spremembe");
            updateTextAttributePrefLabel(meta, "changeNote", property);
            property = new Property("sl", "Opomba o spremembah termina");
            updateTextAttributeDescription(meta, "changeNote", property);

            property = new Property("sl", "Stanje termina");
            updateTextAttributePrefLabel(meta, "status", property);
            property = new Property("sl", "Stanje termina");
            updateTextAttributeDescription(meta, "status", property);
        }

        if (domainName.equals("Concept")) {
            Property property = new Property("sl", "Pojem");
            meta.updateProperties(PropertyUtil.prefLabel(property));

            property = new Property("sl", "Definicija");
            updateTextAttributePrefLabel(meta, "definition", property);
            property = new Property("sl", "Definira pojem tako, da je razumljiv v kontekstu terminologije in drugih pojmov");
            updateTextAttributeDescription(meta, "definition", property);

            property = new Property("sl", "Opomba");                    
            updateTextAttributePrefLabel(meta, "note", property);
            property = new Property("sl", "Skupne opombe o uporabi pojma");
            updateTextAttributeDescription(meta, "note", property);

            property = new Property("sl", "Opomba uredništva");
            updateTextAttributePrefLabel(meta, "editorialNote", property);
            property = new Property("sl", "Opomba, ki je namenjena prevajalcem ali vzdrževalcem");
            updateTextAttributeDescription(meta, "editorialNote", property);

            property = new Property("sl", "Primer");
            updateTextAttributePrefLabel(meta, "example", property);
            property = new Property("sl", "Primer uporabe pojma v kontekstu");
            updateTextAttributeDescription(meta, "example", property);

            property = new Property("sl", "Področje uporabe pojma");
            updateTextAttributePrefLabel(meta, "conceptScope", property);
            property = new Property("sl", "Obseg ali področje, na katerem se pojem uporablja");
            updateTextAttributeDescription(meta, "conceptScope", property);

            property = new Property("sl", "Razred pojma");
            updateTextAttributePrefLabel(meta, "conceptClass", property);
            property = new Property("sl", "Terminološka specifična klasifikacija za kategorizacijo pojmov");
            updateTextAttributeDescription(meta, "conceptClass", property);

            property = new Property("sl", "Razred besede");
            updateTextAttributePrefLabel(meta, "wordClass", property);
            property = new Property("sl", "Razred besede se uporablja, če je pojem pridevnik ali glagol");
            updateTextAttributeDescription(meta, "wordClass", property);

            property = new Property("sl", "Opomba spremembe");
            updateTextAttributePrefLabel(meta, "changeNote", property);
            property = new Property("sl", "Opomba spremembe o pojmu");
            updateTextAttributeDescription(meta, "changeNote", property);

            property = new Property("sl", "Opomba o zgodovini");
            updateTextAttributePrefLabel(meta, "historyNote", property);
            property = new Property("sl", "Opombe o zgodovini uporabe pojma");
            updateTextAttributeDescription(meta, "historyNote", property);

            property = new Property("sl", "Status pojma");
            updateTextAttributePrefLabel(meta, "status", property);
            property = new Property("sl", "Status pojma");
            updateTextAttributeDescription(meta, "status", property);

            property = new Property("sl", "Opis");                    
            updateTextAttributePrefLabel(meta, "notation", property);
            property = new Property("sl", "Sistematičen zapis, ki se uporablja za razvrščanje pojmov");
            updateTextAttributeDescription(meta, "notation", property);

            property = new Property("sl", "Vir");
            updateTextAttributePrefLabel(meta, "source", property);
            property = new Property("sl", "Viri, uporabljeni za definicijo pojma");
            updateTextAttributeDescription(meta, "source", property);

            property = new Property("sl", "Zunanja povezava");
            updateTextAttributePrefLabel(meta, "externalLink", property);

            property = new Property("sl", "Predmetno področje");
            updateTextAttributePrefLabel(meta, "subjectArea", property);

            // reference attributes
            property = new Property("sl", "Prednostni termin");
            updateReferenceAttributePrefLabel(meta, "prefLabelXl", property);
            property = new Property("sl", "Prednostni izraz, ki je dodeljen pojmu");
            updateReferenceAttributeDescription(meta, "prefLabelXl", property);

            property = new Property("sl", "Sinonim");
            updateReferenceAttributePrefLabel(meta, "altLabelXl", property);
            property = new Property("sl", "Sinonim za termin");
            updateReferenceAttributeDescription(meta, "altLabelXl", property);

            property = new Property("sl", "Nepriporočljiv sinonim");
            updateReferenceAttributePrefLabel(meta, "notRecommendedSynonym", property);
            property = new Property("sl", "Termin, katerega uporaba se ne priporoča");
            updateReferenceAttributeDescription(meta, "notRecommendedSynonym", property);

            property = new Property("sl", "Skrit termin");
            updateReferenceAttributePrefLabel(meta, "hiddenTerm", property);
            property = new Property("sl", "Skrit termin, ki se uporablja za usmerjanje uporabnikov k uporabi prednostnega termina");
            updateReferenceAttributeDescription(meta, "hiddenTerm", property);

            property = new Property("sl", "Širši pojem");
            updateReferenceAttributePrefLabel(meta, "broader", property);
            property = new Property("sl", "Pojem, ki je konceptualno širši od tega pojma");
            updateReferenceAttributeDescription(meta, "broader", property);

            property = new Property("sl", "Ožji pojem");
            updateReferenceAttributePrefLabel(meta, "narrower", property);
            property = new Property("sl", "Pojem, ki je v hierarhični zvezi s tem pojmom in vsebuje semantični pomen tega pojma");
            updateReferenceAttributeDescription(meta, "narrower", property);

            property = new Property("sl", "Tesno ujemanje v drugem besedišču");
            updateReferenceAttributePrefLabel(meta, "closeMatch", property);
            property = new Property("sl", "Pojem, ki je delno enak, vendar sta področje ali obseg uporabe drugačna");
            updateReferenceAttributeDescription(meta, "closeMatch", property);

            property = new Property("sl", "Sorodni pojem");
            updateReferenceAttributePrefLabel(meta, "related", property);
            property = new Property("sl", "Pojem, ki je soroden temu pojmu");
            updateReferenceAttributeDescription(meta, "related", property);

            property = new Property("sl", "Je del pojma");
            updateReferenceAttributePrefLabel(meta, "isPartOf", property);
            property = new Property("sl", "Pojem, ki je hierarhično povezan s tem pojmom");
            updateReferenceAttributeDescription(meta, "isPartOf", property);

            property = new Property("sl", "Ima del pojma");
            updateReferenceAttributePrefLabel(meta, "hasPart", property);
            property = new Property("sl", "Ožji pojem, ki je del tega pojma");
            updateReferenceAttributeDescription(meta, "hasPart", property);

            property = new Property("sl", "Sorodni pojem v drugem besedišču");
            updateReferenceAttributePrefLabel(meta, "relatedMatch", property);
            property = new Property("sl", "Sorodni pojem, ki je v drugačni terminologiji");
            updateReferenceAttributeDescription(meta, "relatedMatch", property);

            property = new Property("sl", "Ujemanje pojma v drugem besedišču");
            updateReferenceAttributePrefLabel(meta, "exactMatch", property);
            property = new Property("sl", "Natančna kopija tega pojma v drugi terminologiji");
            updateReferenceAttributeDescription(meta, "exactMatch", property);

            property = new Property("sl", "Iskalni termin");
            updateReferenceAttributePrefLabel(meta, "searchTerm", property);
            property = new Property("sl", "Termin, ki ne označuje pojma, je pa lahko uporaben za iskanje ali referenčne namene");
            updateReferenceAttributeDescription(meta, "searchTerm", property);
        }

        if (domainName.equals("ConceptLink")) {
            Property property = new Property("sl", "Linked concept-sl");
            meta.updateProperties(PropertyUtil.prefLabel(property));
            property = new Property("sl", "Prednostno ime");
            updateTextAttributePrefLabel(meta, "prefLabel", property);
            property = new Property("sl", "Ciljni uri");
            updateTextAttributePrefLabel(meta, "targetGraph",property);
            property = new Property("sl", "Ciljni identifikator");
            updateTextAttributePrefLabel(meta, "targetId", property);
            property = new Property("sl", "Ime besedišča");
            updateTextAttributePrefLabel(meta, "vocabularyLabel", property);
        }
        return rv;
    }

    /**
     * Update individual preflabel for text-attributes
     * 
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
     boolean updateTextAttributePrefLabel(MetaNode meta, String attributeName, Property propertyValue) {
        boolean rv = false;
        // Print textAttributes
        List<AttributeMeta> ta = meta.getTextAttributes().stream().filter(item -> item.getId().equals(attributeName))
                .collect(Collectors.toList());
        if (!ta.isEmpty()) {
            if (ta.size() > 1) {
                System.err.println("Error, several " + attributeName + " attributes in same node ");
            } else {
                AttributeMeta att = ta.get(0);
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.prefLabel(propertyValue)));
                rv = true;
            }
        }
        return rv;
    }

    /**
     * Update individual description for text-attributes
     * 
     * @param meta
     * @param attributeName
     * @param description
     * @return
     */
    boolean updateTextAttributeDescription(MetaNode meta, String attributeName, List<Property> properties) {
        boolean rv = false;
        // Print textAttributes
        List<AttributeMeta> ta = meta.getTextAttributes().stream().filter(item -> item.getId().equals(attributeName))
                .collect(Collectors.toList());
        if (!ta.isEmpty()) {
            if (ta.size() > 1) {
                System.err.println("Error, several " + attributeName + " attributes in same node ");
            } else {
                AttributeMeta att = ta.get(0);
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.description(properties)));
                rv = true;
            }
        }
        return rv;
    }

    boolean updateTextAttributeDescription(MetaNode meta, String attributeName, Property property) {
        boolean rv = false;
        // Print textAttributes
        List<AttributeMeta> ta = meta.getTextAttributes().stream().filter(item -> item.getId().equals(attributeName))
                .collect(Collectors.toList());
        if (!ta.isEmpty()) {
            if (ta.size() > 1) {
                System.err.println("Error, several " + attributeName + " attributes in same node ");
            } else {
                AttributeMeta att = ta.get(0);
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.description(property)));
                rv = true;
            }
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

    boolean updateReferenceAttributePrefLabel(MetaNode meta, String attributeName, List<Property> properties) {
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
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.prefLabel(properties)));
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

    boolean updateReferenceAttributeDescription(MetaNode meta, String attributeName, List<Property> properties) {
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
                att.updateProperties(PropertyUtil.merge(att.getProperties(), PropertyUtil.description(properties)));
                rv = true;
            }
        }
        return rv;
    }
}
