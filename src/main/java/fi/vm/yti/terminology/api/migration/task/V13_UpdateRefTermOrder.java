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

import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Migration for YTI-1106, classification->information domain and HomographNumber fix."
 */
@Component
public class V13_UpdateRefTermOrder implements MigrationTask {

    public enum REF_ORDER {
       prefLabelXl,
       altLabelXl,
       notRecommendedSynonym,
       hiddenTerm,
       broader,
       narrower,
       closeMatch,
       related,
       isPartOf,
       hasPart,
       relatedMatch,
       exactMatch,
       searchTerm
    };

    private final MigrationService migrationService;

    V13_UpdateRefTermOrder(MigrationService migrationService) {
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
            // ReferenceAttributes
            List<ReferenceMeta>  ra=meta.getReferenceAttributes();            
            ra.forEach(o->{
                System.out.println("Name="+o.getId()+ " index="+o.getIndex()); 
            }); 
            // Change item order
            changeRefOrder(meta);
            System.out.println("-------- After orderChange");
            meta.getReferenceAttributes().forEach(o->{
                System.out.println("Name="+o.getId()+ " index="+o.getIndex()); 
            });
        }
        return rv;
    }

    void changeRefOrder(MetaNode meta){

        List<ReferenceMeta>  ra=meta.getReferenceAttributes();
        Map<String, ReferenceMeta> raMap = new LinkedHashMap<>();
        List<ReferenceMeta>  ordered = new LinkedList<>();

        // Create map for  order
        ra.forEach(o->{
            raMap.put(o.getId(),o);
        }); 

        // Contruct new List with specified order
        for(REF_ORDER s:REF_ORDER.values()){
            ordered.add(raMap.get(s.toString()));
        }
        // Replace original with given order 
        ra.clear();
        ra.addAll(ordered);
    }
}
