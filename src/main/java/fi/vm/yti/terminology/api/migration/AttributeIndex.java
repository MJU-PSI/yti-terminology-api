package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.config.DatamodelProperties;
import fi.vm.yti.terminology.api.model.termed.AttributeMeta;
import fi.vm.yti.terminology.api.model.termed.Property;
import fi.vm.yti.terminology.api.model.termed.TypeId;

import org.elasticsearch.common.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.*;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.List;

@Service
public final class AttributeIndex {
 
    private final DatamodelProperties datamodelProperties;

    @Inject
    private AttributeIndex(DatamodelProperties datamodelProperties) {
        this.datamodelProperties = datamodelProperties;
    }


    @NotNull
    public AttributeMeta prefLabel(TypeId domain, long index, String fi, String en) {

        return new AttributeMeta(
                "prefLabel",
                "http://www.w3.org/2004/02/skos/core#prefLabel",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(fi, en),
                        type("localizable:single,required")
                )
        );
    }

    @NotNull
    public AttributeMeta prefLabel(TypeId domain, long index, String fi, String en, String sv) {

        return new AttributeMeta(
                "prefLabel",
                "http://www.w3.org/2004/02/skos/core#prefLabel",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(fi, en, sv),
                        type("localizable:single,required")
                )
        );
    }

    @NotNull
    public AttributeMeta prefLabelXl(TypeId domain, long index) {

        return new AttributeMeta(
                "prefLabel",
                "http://www.w3.org/2008/05/skos-xl#literalForm",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termin arvo",
                                "Term literal value"
                        ),
                        type("localizable:single,required")
                )
        );
    }

    @NotNull
    public AttributeMeta altLabel(TypeId domain, long index) {

        return new AttributeMeta(
                "altLabel",
                "http://www.w3.org/2004/02/skos/core#altLabel",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Vaihtoehtoinen termi",
                                "Alternative label"
                        ),
                        type("localizable:multiple")
                )
        );
    }

    @NotNull
    public AttributeMeta hiddenLabel(TypeId domain, long index) {

        return new AttributeMeta(
                "hiddenLabel",
                "http://www.w3.org/2004/02/skos/core#hiddenLabel",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Piilotettu termi",
                                "Hidden label"
                        ),
                        type("localizable:multiple")
                )
        );
    }

    @NotNull
    public AttributeMeta definition(TypeId domain, long index, boolean linkable) {
        return new AttributeMeta(
                "definition",
                "http://www.w3.org/2004/02/skos/core#definition",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Määritelmä",
                                "Definition"
                        ),
                        type("localizable:multiple,area" + (linkable ? ",xml" : ""))
                )
        );
    }

    @NotNull
    public AttributeMeta note(TypeId domain, long index, boolean linkable) {
        return new AttributeMeta(
                "note",
                "http://www.w3.org/2004/02/skos/core#note",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Huomautus",
                                "Note"
                        ),
                        type("localizable:multiple,area" + (linkable ? ",xml" : ""))
                )
        );
    }

    @NotNull
    public AttributeMeta description(TypeId domain, long index) {
        return new AttributeMeta(
                "description",
                "http://purl.org/dc/terms/description",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Kuvaus",
                                "Description"
                        ),
                        type("localizable:area")
                )
        );
    }

    @NotNull
    public AttributeMeta language(TypeId domain, long index) {
        return new AttributeMeta(
                "language",
                "http://purl.org/dc/terms/language",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Kieli",
                                "Language"
                        ),
                        type("language:multiple")
                )
        );
    }

    @NotNull
    public AttributeMeta status(TypeId domain, long index, String fi, String en) {
        return new AttributeMeta(
                "status",
                "http://www.w3.org/2003/06/sw-vocab-status/ns#term_status",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(fi, en),
                        type("status")
                )
        );
    }

    @NotNull
    public AttributeMeta draftComment(TypeId domain, long index) {
        return new AttributeMeta(
                "draftComment",
                "http://www.w3.org/2000/01/rdf-schema#comment",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Luonnosvaiheen kommentti",
                                "Draft comment"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta source(TypeId domain, long index) {
        return new AttributeMeta(
                "source",
                "http://purl.org/dc/terms/source",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Lähde",
                                "Source"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta scope(TypeId domain, long index) {
        return new AttributeMeta(
                "scope",
                this.datamodelProperties.getUri().getUriHostAddress() + "/datamodel/ns/iow#scope",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Käyttöala",
                                "Scope"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta conceptScope(TypeId domain, long index) {
        return new AttributeMeta(
                "conceptScope",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/iow#conceptScope",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Käyttöala",
                                "Concept scope"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta conceptClass(TypeId domain, long index) {
        return new AttributeMeta(
                "conceptClass",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/iow#conceptClass",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Käsitteen luokka",
                                "Concept class"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta priority(TypeId domain, long index) {
        return new AttributeMeta(
                "priority",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/iow#priority",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Ensisijainen järjestysnimi",
                                "Priority name"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta scopeNote(TypeId domain, long index) {
        return new AttributeMeta(
                "scopeNote",
                "http://www.w3.org/2004/02/skos/core#scopeNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Käyttöala",
                                "Scope note"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta example(TypeId domain, long index) {
        return new AttributeMeta(
                "example",
                "http://www.w3.org/2004/02/skos/core#example",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Käyttöesimerkki",
                                "Example"
                        ),
                        type("localizable:multiple,area")
                )
        );
    }

    @NotNull
    public AttributeMeta editorialNote(TypeId domain, long index) {
        return new AttributeMeta(
                "editorialNote",
                "http://www.w3.org/2004/02/skos/core#editorialNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Ylläpitäjän muistiinpano",
                                "Editorial note"
                        ),
                        type("string:multiple,area")
                )
        );
    }

    @NotNull
    public AttributeMeta historyNote(TypeId domain, long index) {
        return new AttributeMeta(
                "historyNote",
                "http://www.w3.org/2004/02/skos/core#historyNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Käytön historiatieto",
                                "History note"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta changeNote(TypeId domain, long index) {
        return new AttributeMeta(
                "changeNote",
                "http://www.w3.org/2004/02/skos/core#changeNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Muutoshistoriatieto",
                                "Change note"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta notation(TypeId domain, long index, String fi, String en) {
        return new AttributeMeta(
                "notation",
                "http://www.w3.org/2004/02/skos/core#notation",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(fi, en),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta order(TypeId domain, long index) {
        return new AttributeMeta(
                "order",
                "http://www.w3.org/ns/shacl#order",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Järjestysnumero",
                                "Order index"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta conceptWordClass(TypeId domain, long index) {
        return new AttributeMeta(
                "wordClass",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#conceptWordClass",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Sanaluokka",
                                "Word class"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta termWordClass(TypeId domain, long index) {
        return new AttributeMeta(
                "wordClass",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termWordClass",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Sanaluokka",
                                "Word class"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta termStyle(TypeId domain, long index) {
        return new AttributeMeta(
                "termStyle",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termStyle",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termin tyyli",
                                "Term style"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta termFamily(TypeId domain, long index) {
        return new AttributeMeta(
                "termFamily",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termFamily",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termin suku",
                                "Term family"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta termConjugation(TypeId domain, long index) {
        return new AttributeMeta(
                "termConjugation",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termConjugation",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termin luku",
                                "Term conjugation"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta termEquivalency(TypeId domain, long index) {
        return new AttributeMeta(
                "termEquivalency",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termEquivalency",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termin vastaavuus",
                                "Term equivalency"
                        ),
                        type("string:single")
                )
        );
    }

    // Termin lisätieto / Term info.  Additional information about the term / Lisätietoa termin käytöstä
    @NotNull
    public AttributeMeta termInfo(TypeId domain, long index) {
        return new AttributeMeta(
                "termInfo",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termInfo",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termin lisätieto",
                                "Term info"
                        ),
                        type("string:single")
                )
        );
    }

    // homonyymin järjestysnumero / homograph number.  Numero joka kuvaa homonyymin järjestyksen. A number indicating the position of the term within a sequence
    //of homographs
    @NotNull
    public AttributeMeta termHomographNumber(TypeId domain, long index) {
        return new AttributeMeta(
                "termHomographNumber",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#termHomographNumber",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "homonyymin järjestysnumero",
                                "Homograph number"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta contact(TypeId domain, long index) {
        return new AttributeMeta(
                "contact",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#contact",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Yhteydenotto",
                                "Contact"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public AttributeMeta origin(TypeId domain, long index) {
        return new AttributeMeta(
                "origin",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/st#origin",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Kopioitu sanastosta",
                                "Copied from"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta terminologyType(TypeId domain, long index) {
        return new AttributeMeta(
                "terminologyType",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/term/#terminologyType",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Sanaston tyyppi",
                                "Terminology type"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta externalLink(TypeId domain, long index) {
        return new AttributeMeta(
                "externalLink",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/term#ExternalLink",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Ulkoinen linkki",
                                "External link"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta subjectArea(TypeId domain, long index) {
        return new AttributeMeta(
                "subjectArea",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/term#subjectArea",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Aihealue",
                                "Subject area"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta termEquivalencyRelation(TypeId domain, long index) {
        return new AttributeMeta(
                "termEquivalencyRelation",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/term#termEquivalencyRelation",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(
                                "Termi, johon vastaavuus liittyy",
                                "Term equivalency is related to"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public AttributeMeta annotationId(TypeId domain, long index) {

        List<Property> labelList = new ArrayList<>();
        labelList.add(new Property("en", "Annotation ID"));
        labelList.add(new Property("sl", "ID anotacije"));

        return new AttributeMeta(
                "annotationId",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/iow#annotationId",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(labelList),
                        type("primary:single,required")
                )
        );
    }

    @NotNull
    public AttributeMeta annotationValue(TypeId domain, long index) {

        List<Property> labelList = new ArrayList<>();
        labelList.add(new Property("en", "Annotation value"));
        labelList.add(new Property("sl", "Vrednost anotacije"));

        return new AttributeMeta(
                "annotationValue",
                this.datamodelProperties.getUri().getUriHostAddress()  + "/datamodel/ns/iow#annotationValue",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyUtil.prefLabel(labelList),
                        type("localizable:single,area")
                )
        );
    }
}
