package fi.csc.termed.search;

public class Notification {
    Identifier node;
}

class Identifier {
    String id;
    Type type;
}

class Type {
    String id;
    Graph graph;
}

class Graph {
    String id;
}
