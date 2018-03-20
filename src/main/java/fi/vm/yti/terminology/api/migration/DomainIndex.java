package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.model.termed.GraphId;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import fi.vm.yti.terminology.api.model.termed.TypeId;

import java.util.UUID;

public final class DomainIndex {

    public static final UUID TERMINOLOGICAL_VOCABULARY_GRAPH_ID = UUID.fromString("61cf6bde-46e6-40bb-b465-9b2c66bf4ad8");
    public static final UUID VOCABULARY_GRAPH_ID = UUID.fromString("b387af64-4c66-4542-95f3-4c33c6831fcc");
    public static final UUID SCHEMA_GRAPH_ID = UUID.fromString("9d9d546a-221f-44ed-b047-481653eb3192");

    public static final TypeId ORGANIZATION_DOMAIN =
            new TypeId(NodeType.Organization, new GraphId(UUID.fromString("228cce1e-8360-4039-a3f7-725df5643354")));

    public static final TypeId GROUP_DOMAIN =
            new TypeId(NodeType.Group, new GraphId(UUID.fromString("7f4cb68f-31f6-4bf9-b699-9d72dd110c4c")));

    public static final TypeId CONCEPT_DOMAIN =
            new TypeId(NodeType.Concept, new GraphId(VOCABULARY_GRAPH_ID));

    public static final TypeId VOCABULARY_DOMAIN =
            new TypeId(NodeType.Concept, new GraphId(VOCABULARY_GRAPH_ID));

    public static final TypeId COLLECTION_DOMAIN =
            new TypeId(NodeType.Concept, new GraphId(VOCABULARY_GRAPH_ID));

    public static final TypeId TERMINOLOGICAL_CONCEPT_DOMAIN =
            new TypeId(NodeType.Concept, new GraphId(TERMINOLOGICAL_VOCABULARY_GRAPH_ID));

    public static final TypeId TERMINOLOGICAL_CONCEPT_LINK_DOMAIN =
            new TypeId(NodeType.ConceptLink, new GraphId(TERMINOLOGICAL_VOCABULARY_GRAPH_ID));

    public static final TypeId TERMINOLOGICAL_VOCABULARY_DOMAIN =
            new TypeId(NodeType.TerminologicalVocabulary, new GraphId(TERMINOLOGICAL_VOCABULARY_GRAPH_ID));

    public static final TypeId TERMINOLOGICAL_COLLECTION_DOMAIN =
            new TypeId(NodeType.Collection, new GraphId(TERMINOLOGICAL_VOCABULARY_GRAPH_ID));

    public static final TypeId TERM_DOMAIN =
            new TypeId(NodeType.Term, new GraphId(TERMINOLOGICAL_VOCABULARY_GRAPH_ID));

    public static final TypeId SCHEMA_DOMAIN =
            new TypeId(NodeType.Schema, new GraphId(SCHEMA_GRAPH_ID));
    
    // prevent construction
    private DomainIndex() {
    }
}
