package fi.vm.yti.terminology.api.model.termed;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/*public enum Permission {
    READ,
    INSERT,
    UPDATE,
    DELETE,
    EMPTY
}
*/
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Permission {
    final String READ="READ";
    final String INSERT="INSERT";
    final String UPDATE="UPDATE";
    final String DELETE="DELETE";

    @JsonProperty("empty")
    private Boolean empty;

    /**
     *
     * @param empty
     */
    public Permission(Boolean empty) {
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

}
