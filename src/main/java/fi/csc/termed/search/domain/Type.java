package fi.csc.termed.search.domain;

public class Type {

    private TypeId id;
    private Graph graph;

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public TypeId getId() {
        return id;
    }

    public void setId(TypeId id) {
        this.id = id;
    }

    public enum TypeId {
        Term,
        Concept
    }
}
