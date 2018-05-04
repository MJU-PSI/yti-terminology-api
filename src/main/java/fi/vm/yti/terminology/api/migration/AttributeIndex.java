package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.model.termed.AttributeMeta;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import org.jetbrains.annotations.NotNull;

import static fi.vm.yti.terminology.api.migration.PropertyIndex.type;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.merge;
import static java.util.Collections.emptyMap;

public final class AttributeIndex {

    @NotNull
    public static AttributeMeta prefLabelTerm(TypeId domain, long index, String fi, String en) {

        return new AttributeMeta(
                "prefLabel",
                "http://www.w3.org/2004/02/skos/core#prefLabel",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(fi, en),
                        type("localizable:single")
                )
        );
    }

    @NotNull
    public static AttributeMeta altLabel(TypeId domain, long index) {

        return new AttributeMeta(
                "altLabel",
                "http://www.w3.org/2004/02/skos/core#altLabel",
                index,
                domain,
                emptyMap(),
                PropertyIndex.prefLabel(
                        "Vaihtoehtoinen termi",
                        "Alternative label"
                )
        );
    }

    @NotNull
    public static AttributeMeta hiddenLabel(TypeId domain, long index) {

        return new AttributeMeta(
                "hiddenLabel",
                "http://www.w3.org/2004/02/skos/core#hiddenLabel",
                index,
                domain,
                emptyMap(),
                PropertyIndex.prefLabel(
                        "Piilotettu termi",
                        "Hidden label"
                )
        );
    }

    @NotNull
    public static AttributeMeta definition(TypeId domain, long index, boolean linkable) {
        return new AttributeMeta(
                "definition",
                "http://www.w3.org/2004/02/skos/core#definition",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Määritelmä",
                                "Definition"
                        ),
                        type("localizable:multiple,area" + (linkable ? ",xml" : ""))
                )
        );
    }

    @NotNull
    public static AttributeMeta note(TypeId domain, long index, boolean linkable) {
        return new AttributeMeta(
                "note",
                "http://www.w3.org/2004/02/skos/core#note",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Huomautus",
                                "Note"
                        ),
                        type("localizable:multiple,area" + (linkable ? ",xml" : ""))
                )
        );
    }

    @NotNull
    public static AttributeMeta description(TypeId domain, long index) {
        return new AttributeMeta(
                "description",
                "http://purl.org/dc/terms/description",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Kuvaus",
                                "Description"
                        ),
                        type("localizable:area")
                )
        );
    }

    @NotNull
    public static AttributeMeta language(TypeId domain, long index) {
        return new AttributeMeta(
                "language",
                "http://purl.org/dc/terms/language",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Kieli",
                                "Language"
                        ),
                        type("language:multiple")
                )
        );
    }

    @NotNull
    public static AttributeMeta status(TypeId domain, long index, String fi, String en) {
        return new AttributeMeta(
                "status",
                "http://www.w3.org/2003/06/sw-vocab-status/ns#term_status",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(fi, en),
                        type("status")
                )
        );
    }

    @NotNull
    public static AttributeMeta draftComment(TypeId domain, long index) {
        return new AttributeMeta(
                "draftComment",
                "http://www.w3.org/2000/01/rdf-schema#comment",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Luonnosvaiheen kommentti",
                                "Draft comment"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta source(TypeId domain, long index) {
        return new AttributeMeta(
                "source",
                "http://purl.org/dc/terms/source",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Lähde",
                                "Source"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta scope(TypeId domain, long index) {
        return new AttributeMeta(
                "scope",
                "http://uri.suomi.fi/datamodel/ns/iow#scope",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Käyttöala",
                                "Scope"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta priority(TypeId domain, long index) {
        return new AttributeMeta(
                "priority",
                "http://uri.suomi.fi/datamodel/ns/iow#priority",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Ensisijainen järjestysnimi",
                                "Priority name"
                        ),
                        type("string:single")
                )
        );
    }

    @NotNull
    public static AttributeMeta scopeNote(TypeId domain, long index) {
        return new AttributeMeta(
                "scopeNote",
                "http://www.w3.org/2004/02/skos/core#scopeNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Käyttöala",
                                "Scope note"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta example(TypeId domain, long index) {
        return new AttributeMeta(
                "example",
                "http://www.w3.org/2004/02/skos/core#example",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Käyttöesimerkki",
                                "Example"
                        ),
                        type("localizable:multiple,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta editorialNote(TypeId domain, long index) {
        return new AttributeMeta(
                "editorialNote",
                "http://www.w3.org/2004/02/skos/core#editorialNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Ylläpitäjän muistiinpano",
                                "Editorial note"
                        ),
                        type("string:multiple,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta historyNote(TypeId domain, long index) {
        return new AttributeMeta(
                "historyNote",
                "http://www.w3.org/2004/02/skos/core#historyNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Käytön historiatieto",
                                "History note"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta changeNote(TypeId domain, long index) {
        return new AttributeMeta(
                "changeNote",
                "http://www.w3.org/2004/02/skos/core#changeNote",
                index,
                domain,
                emptyMap(),
                merge(
                        PropertyIndex.prefLabel(
                                "Muutoshistoriatieto",
                                "Change note"
                        ),
                        type("string:single,area")
                )
        );
    }

    @NotNull
    public static AttributeMeta notation(TypeId domain, long index, String fi, String en) {
        return new AttributeMeta(
                "notation",
                "http://www.w3.org/2004/02/skos/core#notation",
                index,
                domain,
                emptyMap(),
                PropertyIndex.prefLabel(fi, en)
        );
    }

    @NotNull
    public static AttributeMeta order(TypeId domain, long index) {
        return new AttributeMeta(
                "order",
                "http://www.w3.org/ns/shacl#order",
                index,
                domain,
                emptyMap(),
                PropertyIndex.prefLabel(
                        "Järjestysnumero",
                        "Order index"
                )
        );
    }

    // prevent construction
    private AttributeIndex() {
    }
}
