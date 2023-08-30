package fi.vm.yti.terminology.api.model.termed;

import java.io.Serializable;

public enum NodeType implements Serializable {

    Vocabulary,
    TerminologicalVocabulary,
    Concept,
    Term,
    ConceptLink,
    Collection,
    Group,
    Organization,
    Schema,
    Annotation;

    public static NodeType placeholder() {
        return Vocabulary;
    }
}
