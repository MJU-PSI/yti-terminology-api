package fi.vm.yti.terminology.api.migration.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@Service
@ConditionalOnBean(MigrationTask.class)
public class Migration {

    private final Package migrationsTasksPackage;
    private final SchemaVersionAccessor schemaVersionAccessor;
    private final List<MigrationTask> migrationTasks;

    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(?<version>\\d+)_.*");

    private static final Logger log = LoggerFactory.getLogger(Migration.class);

    @Autowired
    Migration(MigrationProperties properties,
              SchemaVersionAccessor schemaVersionAccessor,
              List<MigrationTask> migrationTasks) {

        this.migrationsTasksPackage = Package.getPackage(properties.getPackageLocation());
        this.schemaVersionAccessor = schemaVersionAccessor;
        this.migrationTasks = ensureGapLessMigrations(
                migrationTasks.stream()
                        .filter(this::isInConfiguredPackage)
                        .sorted(sortByVersion())
                        .collect(toList())
        );
    }

    public void migrate() {

        log.info("Running migrations");

        if (!isSchemaInitialized()) {
            log.info("Schema version not initialized, initializing");
            schemaVersionAccessor.initialize();
            log.info("Schema version initialized");
            runAllMigrations();
        } else {
            int schemaVersion = schemaVersionAccessor.getSchemaVersion();
            log.info("Current schema at version: " + schemaVersion);
            runMigrationsAfter(schemaVersion);
        }

        log.info("Migration finished");
    }

    private boolean isSchemaInitialized() {

        for (int retryCount = 0; retryCount < 10; retryCount++) {
            try {

                if (retryCount > 0) {
                    log.info("Retrying");
                }

                return schemaVersionAccessor.isInitialized();

            } catch (InitializationException e) {
                log.warn("Initialization failed (" + retryCount + "): " + e.getMessage(), e);
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }

        throw new RuntimeException("Cannot initialize");

    }

    private boolean isInConfiguredPackage(MigrationTask task) {
        return task.getClass().getPackage().equals(migrationsTasksPackage);
    }

    private void runAllMigrations() {
        migrationTasks.forEach(this::runMigration);
    }

    private void runMigrationsAfter(int schemaVersion) {
        migrationTasks.stream().skip(schemaVersion).forEach(this::runMigration);
    }

    private void runMigration(MigrationTask task) {
        log.info("Running migration: " + task.getClass().getSimpleName());
        task.migrate();
        schemaVersionAccessor.setSchemaVersion(parseVersion(task));
    }

    private static Comparator<MigrationTask> sortByVersion() {
        return Comparator.comparing(Migration::parseVersion);
    }

    private static List<MigrationTask> ensureGapLessMigrations(List<MigrationTask> tasks) {

        for (int i = 0; i < tasks.size(); i++) {
            int version = parseVersion(tasks.get(i));
            Assert.state(version == i + 1, "Migration scripts not sequential");
        }

        return tasks;
    }

    private static int parseVersion(MigrationTask task) {

        String className = task.getClass().getSimpleName();
        Matcher matcher = VERSION_PATTERN.matcher(className);
        Assert.state(matcher.find(), "Cannot parse version from: " + className);
        String group = matcher.group("version");
        return Integer.parseInt(group);
    }
}
