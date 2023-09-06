package fi.vm.yti.terminology.api.migration.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.config.DatamodelProperties;
import fi.vm.yti.terminology.api.migration.AttributeIndex;
import fi.vm.yti.terminology.api.migration.MigrationService;
import fi.vm.yti.terminology.api.migration.ReferenceIndex;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.Property;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.model.termed.VocabularyNodeType;

import static fi.vm.yti.terminology.api.migration.PropertyUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

@Component
public class V26_AnnotationType implements MigrationTask {

    private final MigrationService migrationService; 
    private final AttributeIndex attributeIndex; 
    private final ReferenceIndex referenceIndex; 
    private final DatamodelProperties datamodelProperties; 

    @Autowired
    V26_AnnotationType(
        MigrationService migrationService, 
        AttributeIndex attributeIndex,
        ReferenceIndex referenceIndex,
        DatamodelProperties datamodelProperties) {
        this.migrationService = migrationService;
        this.attributeIndex = attributeIndex;
        this.referenceIndex = referenceIndex;
        this.datamodelProperties = datamodelProperties;
    }

    @Override
    public void migrate() {

        migrationService.updateTypes(VocabularyNodeType.TerminologicalVocabulary, meta -> {

            TypeId domain = meta.getDomain();

            List<Property> labelList = new ArrayList<>();
            labelList.add(new Property("en", "Annotation"));
            labelList.add(new Property("sl", "Anotacija"));

            MetaNode newMetaNode = new MetaNode(
                    "Annotation",
                    "http://uri.suomi.fi/datamodel/ns/st#annotation",
                    5L,
                    domain.getGraph(),
                    emptyMap(),
                    prefLabel(
                            labelList
                    ),
                    asList(
                            this.attributeIndex.annotationId(domain, 0),
                            this.attributeIndex.annotationValue(domain, 1)
                        
                    ),
                    emptyList()
            );

            List<MetaNode> metaNodes = new ArrayList<>();
            metaNodes.add(newMetaNode);
            migrationService.updateTypes(domain.getGraph().getId(), metaNodes);

            if (meta.isOfType(NodeType.Concept)) {
                meta.addReference(this.referenceIndex.annotation(domain, 37, meta));
            }
            if (meta.isOfType(NodeType.Collection)) {
                meta.addReference(this.referenceIndex.annotation(domain, 4, meta));
            }
            if (meta.isOfType(NodeType.TerminologicalVocabulary)) {
                meta.addReference(this.referenceIndex.annotation(domain, 27, meta));
            }
            
        });

    }

}    