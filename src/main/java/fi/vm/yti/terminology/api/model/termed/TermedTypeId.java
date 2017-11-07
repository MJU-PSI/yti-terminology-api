package fi.vm.yti.terminology.api.model.termed;

import java.util.UUID;

import static java.util.UUID.randomUUID;

public final class TermedTypeId {

    private final String id;
    private final TermedGraphId graph;

    // Jackson constructor
    private TermedTypeId() {
        this("", new TermedGraphId(randomUUID()));
    }

    public TermedTypeId(String id, TermedGraphId graph) {
        this.id = id;
        this.graph = graph;
    }

    public String getId() {
        return id;
    }

    public TermedGraphId getGraph() {
        return graph;
    }

    public UUID getGraphId() {
        return graph != null ? graph.getId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TermedTypeId that = (TermedTypeId) o;

        return id.equals(that.id) && graph.equals(that.graph);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + graph.hashCode();
        return result;
    }
}
