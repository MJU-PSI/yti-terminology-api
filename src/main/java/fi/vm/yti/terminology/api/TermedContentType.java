package fi.vm.yti.terminology.api;

public enum TermedContentType {

    JSON("application/json"),
    JSON_LD("application/ld+json"),
    RDF_XML("application/rdf+xml");

    private final String contentType;

    TermedContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static TermedContentType fromString(String contentType) {

        for (TermedContentType value : values()) {
            if (contentType.contains(value.contentType)) {
                return value;
            }
        }

        return JSON;
    }
}
