package fi.csc.termed.api.common;

import javax.validation.constraints.NotNull;

public class Type {
    @NotNull public String id;
    @NotNull public Graph graph;
}
