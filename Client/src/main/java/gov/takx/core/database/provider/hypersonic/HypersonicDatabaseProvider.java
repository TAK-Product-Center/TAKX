package gov.takx.core.database.provider.hypersonic;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.persist.IDatabaseServer;
import gov.takx.api.project.IProjectManager;
import gov.takx.core.database.provider.ADatabaseProvider;
import gov.takx.core.database.provider.DatabaseProviderType;
import gov.takx.core.database.provider.ServerSpecification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Database provider for HSQLDB.
 *
 * @see HypersonicDatabaseServer
 * @see HypersonicDatabaseConnectionManager
 * @since 3.0
 */
@ApplicationScoped
@DatabaseProviderType(HypersonicDatabaseProvider.PROVIDER_TYPE)
public class HypersonicDatabaseProvider extends ADatabaseProvider
{
    public static final String PROVIDER_TYPE = "hsqldb";

    static
    {
        CustomSqlFunctions.configureAccess();
    }

    @Inject
    protected HypersonicDatabaseProvider(IProgramEnvironment programEnvironment, IProjectManager projectManager)
    {
        super(programEnvironment, projectManager, PROVIDER_TYPE);
    }

    @Override
    protected IDatabaseServer createServer(ServerSpecification serverSpecification)
    {
        final var server = new HypersonicDatabaseServer(serverSpecification);
        server.start();
        return server;
    }

    /**
     * Overridden to provide special handling while under test.
     */
    @Override
    protected void handleProjectClosed()
    {
        // As much as I hate infecting production code with modified behavior when under test, in this case
        // I really can't find a good alternative.  Closing the stopping the core server stops the DB,
        // which causes the in-memory data to be lost.  This breaks all tests that rely on "rebooting" takx.
        //
        // One option would be to force the unit tests to use file-based DB storage instead of in-memory,
        // but that adds a ~25% penalty in execution time, which seems unacceptable.
        //
        // So, for now, we'll simply avoid closing on project boundaries when under test.

        if (!programEnvironment.isPlatformUnderTest()) shutdown();
    }
}
