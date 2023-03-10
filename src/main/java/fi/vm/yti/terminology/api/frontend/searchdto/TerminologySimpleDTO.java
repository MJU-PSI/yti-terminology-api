package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.Map;

public class TerminologySimpleDTO {

    private String id;
    private String code;
    private String uri;
    private String status;
    private String type;
    private Map<String, String> label;

    public TerminologySimpleDTO(final String id,
                                final String code,
                                final String uri,
                                final String status,
                                final String type,
                                final Map<String, String> label) {
        this.id = id;
        this.code = code;
        this.uri = uri;
        this.status = status;
        this.type = type;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(final Map<String, String> label) {
        this.label = label;
    }
}
