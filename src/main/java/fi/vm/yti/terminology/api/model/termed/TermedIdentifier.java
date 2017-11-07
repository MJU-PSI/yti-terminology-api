package fi.vm.yti.terminology.api.model.termed;

import java.util.UUID;

import static java.util.UUID.randomUUID;

public final class TermedIdentifier {

    private final UUID id;
    private final TermedTypeId type;

    // Jackson constructor
    private TermedIdentifier() {
        this(randomUUID(), new TermedTypeId("", new TermedGraphId(randomUUID())));
    }

    public TermedIdentifier(UUID id, TermedTypeId type) {
        this.id = id;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public TermedTypeId getType() {
        return type;
    }
}
