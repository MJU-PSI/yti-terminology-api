package fi.vm.yti.terminology.api.model.termed;

public enum VocabularyNodeType {

    Vocabulary(NodeType.Vocabulary),
    TerminologicalVocabulary(NodeType.TerminologicalVocabulary);

    private NodeType nodeType;

    VocabularyNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public NodeType asNodeType() {
        return this.nodeType;
    }
}
