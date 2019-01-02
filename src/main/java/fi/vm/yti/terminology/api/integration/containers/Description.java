
package fi.vm.yti.terminology.api.integration.containers;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fi",
    "sv",
    "en"
})
public class Description implements Serializable
{

    @JsonProperty("fi")
    private String fi;
    @JsonProperty("sv")
    private String sv;
    @JsonProperty("en")
    private String en;
    private final static long serialVersionUID = -4140189429255523970L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Description() {
    }

    /**
     * 
     * @param fi
     * @param sv
     * @param en
     */
    public Description(String fi, String sv, String en) {
        super();
        this.fi = fi;
        this.sv = sv;
        this.en = en;
    }

    @JsonProperty("fi")
    public String getFi() {
        return fi;
    }

    @JsonProperty("fi")
    public void setFi(String fi) {
        this.fi = fi;
    }

    @JsonProperty("sv")
    public String getSv() {
        return sv;
    }

    @JsonProperty("sv")
    public void setSv(String sv) {
        this.sv = sv;
    }

    @JsonProperty("en")
    public String getEn() {
        return en;
    }

    @JsonProperty("en")
    public void setEn(String en) {
        this.en = en;
    }

}
