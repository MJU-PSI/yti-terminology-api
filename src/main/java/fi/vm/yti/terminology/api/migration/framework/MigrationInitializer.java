package fi.vm.yti.terminology.api.migration.framework;

public class MigrationInitializer {

    private final Migration migration;

    MigrationInitializer(Migration migration) {
        this.migration = migration;
    }

    public void onInit() {
        migration.migrate();
    }
}
