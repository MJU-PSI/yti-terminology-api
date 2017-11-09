package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(value = { "properties", "references", "referrers" })
public final class Identifier {

    private final UUID id;
    private final TypeId type;

    // Jackson constructor
    private Identifier() {
        this(randomUUID(), TypeId.placeholder());
    }

    public Identifier(UUID id, TypeId type) {
        this.id = id;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public TypeId getType() {
        return type;
    }
}
