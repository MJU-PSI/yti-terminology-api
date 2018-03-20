package fi.vm.yti.terminology.api.migration.framework;

import javax.annotation.PostConstruct;

public class MigrationInitializer {

    private final Migration migration;

    MigrationInitializer(Migration migration) {
        this.migration = migration;
    }

    @PostConstruct
    public void onInit() {
        migration.migrate();
    }
}
