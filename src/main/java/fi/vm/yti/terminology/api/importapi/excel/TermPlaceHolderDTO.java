package fi.vm.yti.terminology.api.importapi.excel;

import java.util.UUID;

public class TermPlaceHolderDTO {
    private UUID uuid;
    private String language;

    public TermPlaceHolderDTO(UUID uuid, String language) {
        this.uuid = uuid;
        this.language = language;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLanguage() {
        return language;
    }
}
