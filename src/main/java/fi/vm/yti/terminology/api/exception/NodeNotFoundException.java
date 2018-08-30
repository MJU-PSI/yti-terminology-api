package fi.vm.yti.terminology.api.exception;

import fi.vm.yti.terminology.api.model.termed.NodeType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collection;
import java.util.UUID;

import static java.util.stream.Collectors.joining;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NodeNotFoundException extends RuntimeException {

    public NodeNotFoundException(UUID graphId, UUID nodeId) {
        super("Node " + nodeId + " not found in graph " + graphId);
    }

    public NodeNotFoundException(UUID graphId, Collection<NodeType> nodeTypes) {
        super("Node type " + nodeTypes.stream().map(Enum::name).collect(joining(",")) + " not found in graph " + graphId);
    }
}
