package fi.vm.yti.terminology.api.migration.task;

import fi.vm.yti.migration.MigrationTask;
import fi.vm.yti.terminology.api.migration.MigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;


@Component
public class V25_GroupsTranslation implements MigrationTask {

        private final MigrationService migrationService;

        @Autowired
        V25_GroupsTranslation(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void migrate() {
        addGroupsTranslations();
    }

    private void addGroupsTranslations() {
        migrationService.updateNodesWithJson(new ClassPathResource("migration/newGroupNodes1.json"));
    }
}
