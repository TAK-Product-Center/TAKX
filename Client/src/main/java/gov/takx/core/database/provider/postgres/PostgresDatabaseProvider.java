package gov.takx.core.database.provider.postgres;

import gov.takx.api.annotation.InstanceNumber;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.persist.IDatabaseServer;
import gov.takx.api.processmanager.IProcessManager;
import gov.takx.api.project.IProjectManager;
import gov.takx.core.database.provider.ADatabaseProvider;
import gov.takx.core.database.provider.DatabaseProviderType;
import gov.takx.core.database.provider.ServerSpecification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Database provider for PostgreSQL.
 *
 * @see PostgresDatabaseServer
 * @see PostgresDatabaseConnectionManager
 * @since 3.0
 */
@ApplicationScoped
@DatabaseProviderType(PostgresDatabaseProvider.PROVIDER_TYPE)
public class PostgresDatabaseProvider extends ADatabaseProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PROVIDER_TYPE = "postgres";
    public static final String BASE_PORT = "basePort";

    private final IProcessManager processManager;
    private final int instanceNumber;

    @Inject
    protected PostgresDatabaseProvider(IProgramEnvironment programEnvironment, IProjectManager projectManager,
                                       IProcessManager processManager, @InstanceNumber int instanceNumber)
    {
        super(programEnvironment, projectManager, PROVIDER_TYPE);

        this.processManager = processManager;
        this.instanceNumber = instanceNumber;
    }

    @Override
    protected IDatabaseServer createServer(ServerSpecification serverSpecification)
    {
        final int basePort = getConfiguration().getInt(BASE_PORT, -1);

        if (basePort < 0) throw new IllegalStateException("No '" + BASE_PORT + "' found in postgres configuration");

        final int port = basePort + instanceNumber;

        logger.debug("Use port {}", port);

        final PostgresDatabaseServer server = new PostgresDatabaseServer(serverSpecification, port);

        // Get the DB server started
        final long pid = server.start();
        if (pid > 0)
        {
            processManager.registerExternalProcess(pid);
        }

        return server;
    }
}
