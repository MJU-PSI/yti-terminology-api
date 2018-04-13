package fi.vm.yti.terminology.api.frontend;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public class GroupManagementUser {

    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
    @Nullable
    private final LocalDateTime removalDateTime;

    // Jackson constructor
    private GroupManagementUser() {
        this(UUID.randomUUID(), "", "", "", null);
    }

    public GroupManagementUser(UUID id,
                               String email,
                               String firstName,
                               String lastName,
                               @Nullable LocalDateTime removalDateTime) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.removalDateTime = removalDateTime;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDateTime getRemovalDateTime() {
        return removalDateTime;
    }
}