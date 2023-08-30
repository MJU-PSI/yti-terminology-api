package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.config.DatamodelProperties;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.Property;
import fi.vm.yti.terminology.api.model.termed.ReferenceMeta;
import fi.vm.yti.terminology.api.model.termed.TypeId;

import org.elasticsearch.common.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static fi.vm.yti.terminology.api.migration.DomainIndex.GROUP_DOMAIN;
import static fi.vm.yti.terminology.api.migration.DomainIndex.ORGANIZATION_DOMAIN;
import static fi.vm.yti.terminology.api.migration.PropertyUtil.*;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public final class ReferenceIndex {

    private final DatamodelProperties datamodelProperties;

    @Inject
    private ReferenceIndex(DatamodelProperties datamodelProperties){
        this.datamodelProperties = datamodelProperties;
    }

    private final TypeId termDomainFromConceptDomain(TypeId conceptDomain) {
        return new TypeId(NodeType.Term, conceptDomain.getGraph());
    }

   private final TypeId annotationDomainFromConceptDomain(TypeId conceptDomain) {
        return new TypeId(NodeType.Annotation, conceptDomain.getGraph());
    }

    @NotNull
    public ReferenceMeta contributor(TypeId domain, long index) {
        return new ReferenceMeta(
                ORGANIZATION_DOMAIN,
                "contributor",
                "http://purl.org/dc/terms/contributor",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Sisällöntuottaja",
                        "Contributor"
                )
        );
    }

    @NotNull
    public ReferenceMeta group(TypeId domain, long index) {
        return new ReferenceMeta(
                GROUP_DOMAIN,
                "inGroup",
                "http://purl.org/dc/terms/isPartOf",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Luokitus",
                        "Classification"
                )
        );
    }

    @NotNull
    public ReferenceMeta broader(TypeId domain, long index, String fi, String en) {
        return new ReferenceMeta(
                domain,
                "broader",
                "http://www.w3.org/2004/02/skos/core#broader",
                index,
                domain,
                emptyMap(),
                prefLabel(fi, en)
        );
    }

    @NotNull
    public ReferenceMeta narrower(TypeId domain, long index, String fi, String en) {
        return new ReferenceMeta(
                domain,
                "narrower",
                "http://www.w3.org/2004/02/skos/core#narrower",
                index,
                domain,
                emptyMap(),
                prefLabel(fi, en)
        );
    }

    @NotNull
    public ReferenceMeta relatedConcept(TypeId domain, long index) {
        return new ReferenceMeta(
                domain,
                "related",
                "http://www.w3.org/2004/02/skos/core#related",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Liittyvä käsite",
                        "Related concept"
                )
        );
    }

    @NotNull
    public ReferenceMeta partOfConcept(TypeId domain, long index) {
        return new ReferenceMeta(
                domain,
                "isPartOf",
                "http://purl.org/dc/terms/partOf",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Koostumussuhteinen yläkäsite",
                        "Is part of concept"
                )
        );
    }

    @NotNull
    public ReferenceMeta hasPartConcept(TypeId domain, long index) {
        return new ReferenceMeta(
                domain,
                "hasPart",
                "http://purl.org/dc/terms/hasPart",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Koostumussuhteinen alakäsite",
                        "Has part concept"
                )
        );
    }

    @NotNull
    public ReferenceMeta relatedMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "relatedMatch",
                "http://www.w3.org/2004/02/skos/core#relatedMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Liittyvä käsite toisessa sanastossa",
                                "Related concept in other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public ReferenceMeta exactMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "exactMatch",
                "http://www.w3.org/2004/02/skos/core#exactMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Vastaava käsite toisessa sanastossa",
                                "Related concept from other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public ReferenceMeta closeMatch(TypeId domain, TypeId externalLinkDomain, long index) {
        return new ReferenceMeta(
                externalLinkDomain,
                "closeMatch",
                "http://www.w3.org/2004/02/skos/core#closeMatch",
                index,
                domain,
                emptyMap(),
                merge(
                        prefLabel(
                                "Lähes vastaava käsite toisessa sanastossa",
                                "Close match in other vocabulary"
                        ),
                        type("link")
                )
        );
    }

    @NotNull
    public ReferenceMeta member(TypeId domain, TypeId targetDomain, long index) {
        return new ReferenceMeta(
                targetDomain,
                "member",
                "http://www.w3.org/2004/02/skos/core#member",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Valikoimaan kuuluva käsite",
                        "Member"
                )
        );
    }

    @NotNull
    public ReferenceMeta prefLabelXl(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "prefLabelXl",
                "http://www.w3.org/2008/05/skos-xl#prefLabel",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Suositettava termi",
                        "Preferred term"
                )
        );
    }

    @NotNull
    public ReferenceMeta altLabelXl(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "altLabelXl",
                this.datamodelProperties.getUri().getUriHostAddress() + "/datamodel/ns/st#synonym",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Synonyymi",
                        "Synonym"
                )
        );
    }

    @NotNull
    public ReferenceMeta notRecommendedSynonym(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "notRecommendedSynonym",
                this.datamodelProperties.getUri().getUriHostAddress() + "/datamodel/ns/st#notRecommendedSynonym",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Ei-suositeltava synonyymi",
                        "Non-recommended synonym"
                )
        );
    }

    @NotNull
    public ReferenceMeta hiddenTerm(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "hiddenTerm",
                this.datamodelProperties.getUri().getUriHostAddress() + "/datamodel/ns/st#hiddenTerm",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Ohjaustermi",
                        "Hidden term"
                )
        );
    }

    @NotNull
    public ReferenceMeta searchTerm(TypeId domain, long index) {
        return new ReferenceMeta(
                termDomainFromConceptDomain(domain),
                "searchTerm",
                this.datamodelProperties.getUri().getUriHostAddress() + "/datamodel/ns/st#searchTerm",
                index,
                domain,
                emptyMap(),
                prefLabel(
                        "Hakutermi",
                        "Search term"
                )
        );
    }

        @NotNull
    public ReferenceMeta annotation(TypeId domain, long index, MetaNode meta) {
        List<Property> prefLabelList = new ArrayList<>();
        prefLabelList.add(new Property("en", "Annotation"));
        prefLabelList.add(new Property("sl", "Anotacija"));

        List<Property> descriptionList = new ArrayList<>();
        if(meta.isOfType(NodeType.Concept)){
          descriptionList.add(new Property("en", "Annotation that is assigned for the concept"));
          descriptionList.add(new Property("sl", "Anotacija, ki je dodeljena pojmu"));   
        } else if(meta.isOfType(NodeType.Collection)) {
          descriptionList.add(new Property("en", "Annotation that is assigned for this collection"));
          descriptionList.add(new Property("sl", "Anotacija, ki je dodeljena zbirki"));
        } else if(meta.isOfType(NodeType.TerminologicalVocabulary)) {
          descriptionList.add(new Property("en", "Annotation that is assigned for the terminology"));
          descriptionList.add(new Property("sl", "Anotacija, ki je dodeljena terminologiji"));
        }

        HashMap<String, List<Property>> map = new HashMap<String, List<Property>>();
        map.put("prefLabel", prefLabelList);
        map.put("description", descriptionList);

        return new ReferenceMeta(
                annotationDomainFromConceptDomain(domain),
                "annotation",
                this.datamodelProperties.getUri().getUriHostAddress() + "/datamodel/ns/st#annotation",
                index,
                domain,
                emptyMap(),
                map
        );
    }
}
