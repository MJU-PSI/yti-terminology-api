package fi.vm.yti.terminology.api.migration.framework;

public interface SchemaVersionAccessor {

    boolean isInitialized();
    void initialize();
    int getSchemaVersion();
    void setSchemaVersion(int version);
}
