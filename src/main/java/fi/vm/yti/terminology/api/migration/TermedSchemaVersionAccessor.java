package fi.vm.yti.terminology.api.migration;

import fi.vm.yti.terminology.api.exception.TermedEndpointException;
import fi.vm.yti.terminology.api.migration.framework.InitializationException;
import fi.vm.yti.terminology.api.migration.framework.SchemaVersionAccessor;
import fi.vm.yti.terminology.api.model.termed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static fi.vm.yti.terminology.api.migration.DomainIndex.SCHEMA_DOMAIN;
import static fi.vm.yti.terminology.api.migration.PropertyIndex.prefLabel;
import static java.util.Collections.*;

@Service
public class TermedSchemaVersionAccessor implements SchemaVersionAccessor {

    private final MigrationService migrationService;

    private static final UUID VERSION_NODE_ID = UUID.fromString("6a9b0937-f9f7-4043-a8b6-d941deb15ac0");
    private static Logger log = LoggerFactory.getLogger(TermedSchemaVersionAccessor.class);

    @Autowired
    TermedSchemaVersionAccessor(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public boolean isInitialized() {
        try {
            return migrationService.isSchemaInitialized();
        } catch (TermedEndpointException e) {
            throw new InitializationException("Termed API has not started yet", e);
        }
    }

    @Override
    public void initialize() {

        TypeId domain = SCHEMA_DOMAIN;

        Graph graph = new Graph(domain.getGraphId(), null, null, emptyList(), emptyMap(), prefLabel("Schema"));

        migrationService.createGraph(graph);
        migrationService.updateTypes(graph.getId(), singletonList(new MetaNode(
                "Schema",
                null,
                1L,
                domain.getGraph(),
                emptyMap(),
                emptyMap(),
                singletonList(
                        new AttributeMeta(
                                "version",
                                null,
                                1L,
                                domain,
                                emptyMap(),
                                emptyMap()
                        )
                ),
                emptyList()
        )));

        setSchemaVersion(0);
    }

    @Override
    public int getSchemaVersion() {
        GenericNode node = migrationService.getNode(SCHEMA_DOMAIN, VERSION_NODE_ID);
        return Integer.parseInt(node.getProperties().get("version").get(0).getValue());
    }

    @Override
    public void setSchemaVersion(int version) {

        log.info("Setting schema version: " + version);

        migrationService.updateAndDeleteInternalNodes(new GenericDeleteAndSave(emptyList(), singletonList(
                new GenericNode(
                        VERSION_NODE_ID,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        SCHEMA_DOMAIN,
                        singletonMap("version", singletonList(new Attribute("", String.valueOf(version)))),
                        emptyMap(),
                        emptyMap()
                )
        )));
    }
}
