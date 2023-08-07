package fi.vm.yti.terminology.api.system;

public class Configuration {
    private String codeListUrl;
    private String dataModelUrl;
    private String commentsUrl;
    private String groupmanagementUrl;
    private boolean messagingEnabled;
    private String namespaceRoot;
    private String env;
    private boolean restrictFilterOptions;

    public String getCodeListUrl() {
        return codeListUrl;
    }
    public void setCodeListUrl(String codeListUrl) {
        this.codeListUrl = codeListUrl;
    }
    public String getDataModelUrl() {
        return dataModelUrl;
    }
    public void setDataModelUrl(String dataModelUrl) {
        this.dataModelUrl = dataModelUrl;
    }
    public String getCommentsUrl() {
        return commentsUrl;
    }
    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }
    public String getGroupmanagementUrl() {
        return groupmanagementUrl;
    }
    public void setGroupmanagementUrl(String groupmanagementUrl) {
        this.groupmanagementUrl = groupmanagementUrl;
    }
    public boolean isMessagingEnabled() {
        return messagingEnabled;
    }
    public void setMessagingEnabled(boolean messagingEnabled) {
        this.messagingEnabled = messagingEnabled;
    }
    public String getNamespaceRoot() {
        return namespaceRoot;
    }
    public void setNamespaceRoot(String namespaceRoot) {
        this.namespaceRoot = namespaceRoot;
    }
    public String getEnv() {
        return env;
    }
    public void setEnv(String env) {
        this.env = env;
    }
    public boolean isRestrictFilterOptions() {
        return restrictFilterOptions;
    }
    public void setRestrictFilterOptions(boolean restrictFilterOptions) {
        this.restrictFilterOptions = restrictFilterOptions;
    }
}
