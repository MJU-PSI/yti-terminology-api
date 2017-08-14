package fi.csc.termed.api.index;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class TermedNotification {

    static class Graph {
        @NotNull public String id;
    }

    public static class Body {
        @NotNull public String user;
        @NotNull public Date date;
        @NotNull public List<Node> nodes;
    }

    public static class Node {
        @NotNull public String id;
        @NotNull public Type type;
    }

    public static class Type {
        @NotNull public String id;
        @NotNull public Graph graph;
    }

    public enum EventType {
        NodeSavedEvent,
        NodeDeletedEvent,
        ApplicationReadyEvent,
        ApplicationShutdownEvent
    }

    @NotNull public EventType type;
    @NotNull public Body body;
}
