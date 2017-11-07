package fi.vm.yti.terminology.api.model.termed;

import java.util.UUID;

import static java.util.UUID.randomUUID;

public final class TermedGraphId {

    private final UUID id;

    // Jackson constructor
    private TermedGraphId() {
        this(randomUUID());
    }

    public TermedGraphId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TermedGraphId graphId = (TermedGraphId) o;

        return id.equals(graphId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
