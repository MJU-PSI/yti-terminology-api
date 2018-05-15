package fi.vm.yti.terminology.api.resolve;

final class ResolvedResource {

    private final String frontEndPath;
    private final String apiResourcePath;

    ResolvedResource(String frontEndPath, String apiResourcePath) {
        this.frontEndPath = frontEndPath;
        this.apiResourcePath = apiResourcePath;
    }

    String getFrontEndPath() {
        return frontEndPath;
    }

    String getApiResourcePath() {
        return apiResourcePath;
    }
}
