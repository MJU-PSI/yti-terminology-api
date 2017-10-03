package fi.vm.yti.terminology.api.index;

public class BrokenTermedDataLinkException extends RuntimeException {
    BrokenTermedDataLinkException(Vocabulary vocabulary, String nodeId) {
        super("Node (" + nodeId + ") not in graph (" + vocabulary.getGraphId() + ")");
    }
}
