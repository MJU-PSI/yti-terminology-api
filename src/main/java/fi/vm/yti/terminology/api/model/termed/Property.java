package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonRawValue;

public final class Property {

    private final String lang;
    private final String value;

    // Jackson constructor
    private Property() {
        this("", "");
    }

    public Property(String lang, String value) {
        this.lang = lang;
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }
}
