package fi.vm.yti.terminology.api.resolve;

public enum ResolvableContentType {

    HTML("text/html"),
    JSON("application/json"),
    JSON_LD("application/ld+json"),
    RDF_XML("application/rdf+xml");

    private final String mediaType;

    ResolvableContentType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public boolean isHandledByFrontend() {
        return this == HTML;
    }

    public static ResolvableContentType fromString(String contentType) {

        for (ResolvableContentType value : values()) {
            if (contentType.contains(value.mediaType)) {
                return value;
            }
        }

        throw new RuntimeException("Unsupported content type: " + contentType);
    }
}
