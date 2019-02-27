package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "empty" })
public final class Property {
    @JsonProperty("empty")
    private Boolean empty;

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

    /**
     *
     * @param empty
     */
    public Property(Boolean empty) {
        this("", "");
        this.empty = empty;
    }

    @JsonProperty("empty")
    public Boolean getEmpty() {
        return empty;
    }

    @JsonProperty("empty")
    public void setEmpty(Boolean empty) {
        this.empty = empty;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }
}
