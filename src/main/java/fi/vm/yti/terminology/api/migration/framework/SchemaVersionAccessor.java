package fi.vm.yti.terminology.api.migration.framework;

public interface SchemaVersionAccessor {

    boolean isInitialized() throws InitializationException;
    void initialize();
    int getSchemaVersion();
    void setSchemaVersion(int version);
}
