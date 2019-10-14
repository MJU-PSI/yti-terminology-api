
package fi.vm.yti.terminology.api.model.integration.containers;

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
public class PrefLabel implements Serializable
{

    @JsonProperty("fi")
    private String fi;
    @JsonProperty("sv")
    private String sv;
    @JsonProperty("en")
    private String en;
    private final static long serialVersionUID = 5139726622575406541L;

    /**
     * No args constructor for use in serialization
     * 
     */
    public PrefLabel() {
    }

    /**
     * 
     * @param fi
     * @param sv
     * @param en
     */
    public PrefLabel(String fi, String sv, String en) {
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
