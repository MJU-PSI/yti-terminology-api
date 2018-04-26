package fi.vm.yti.terminology.api.migration.framework;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnBean(SchemaVersionAccessor.class)
@ConditionalOnProperty(prefix = "migration", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MigrationProperties.class)
public class MigrationConfig {

    private final MigrationProperties properties;

    MigrationConfig(MigrationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MigrationInitializer migrationInitializer(Migration migration) {
        return new MigrationInitializer(migration, properties);
    }

    @PostConstruct
    public void checkLocationExists() {
        Assert.state(this.properties.getPackageLocation() != null,
                "Migration script location not configured");

    }
}
