package fi.csc.termed.api.common;

import javax.validation.constraints.NotNull;

public class NodeIdentifier {
    @NotNull public String id;
    @NotNull public Type type;
}
