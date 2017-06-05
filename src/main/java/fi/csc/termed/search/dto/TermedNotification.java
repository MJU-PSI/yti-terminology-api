package fi.csc.termed.search.dto;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class TermedNotification {

    public static class Graph {

        private String id;

        @NotNull
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Body {

        private String user;
        private Date date;
        private List<Node> nodes;

        public List<Node> getNodes() {
            return nodes;
        }

        public void setNodes(List<Node> node) {
            this.nodes = node;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public static class Node {

        private String id;
        private Type type;

        @NotNull
        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        @NotNull
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Type {

        private TypeId id;
        private Graph graph;

        @NotNull
        public Graph getGraph() {
            return graph;
        }

        public void setGraph(Graph graph) {
            this.graph = graph;
        }

        @NotNull
        public TypeId getId() {
            return id;
        }

        public void setId(TypeId id) {
            this.id = id;
        }
    }

    public enum TypeId {
        Term,
        Concept,
        TerminologicalVocabulary,
        Vocabulary
    }

    public enum EventType {
        NodeSavedEvent,
        NodeDeletedEvent,
        ApplicationReadyEvent,
        ApplicationShutdownEvent
    }

    private EventType type;
    private Body body;

    @NotNull
    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    @NotNull
    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}
