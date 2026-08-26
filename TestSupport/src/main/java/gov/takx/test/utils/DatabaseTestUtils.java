package gov.takx.test.utils;

import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.api.persist.IDatabaseProvider;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.database.DatabaseManager;
import gov.takx.core.database.provider.hypersonic.HypersonicDatabaseProvider;
import gov.takx.core.persist.MessagePersistHelper;
import gov.takx.core.persist.PersistService;
import gov.takx.dataservices.persist.StructureAssociation;
import gov.takx.platform.cdi.InjectableContainerSupport;
import gov.takx.test.AContainerTest;

/**
 * Shared utility to set up our database provider.
 *
 * @since 2.0.0
 */
public class DatabaseTestUtils
{
    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    public static IPersistService setupDatabase(boolean createNew)
    {
        final IDatabaseProvider provider = BEAN_ACCESSOR.getBean(DatabaseManager.class)
                .getDatabaseProvider(HypersonicDatabaseProvider.PROVIDER_TYPE);

        PersistService.setDatabaseProvider(provider);

        final IPersistService service = BEAN_ACCESSOR.getBean(IPersistServiceProvider.class).getPersistService();

        if (service != null) initializeDatabase(createNew, service.getConnectionMgr());

        return service;
    }

    /**
     * Drop all the tables from the database and shutdown the PersistServiceProvider.
     * This provides a clean DB for subsequent tests.
     *
     * @param containerTest The container test running (since this class can't inject it)
     */
    public static void dropDatabaseTables(AContainerTest containerTest)
    {
        final IPersistService service = BEAN_ACCESSOR.getBean(IPersistServiceProvider.class).getPersistService();
        final IDatabaseConnectionManager mgr = service.getConnectionMgr();

        mgr.dropTables();

        // Don't close the database provider.  Since most of the tests use in-memory storage, that would
        // cause the content to be lost, and any tests that use a "reboot" will fail.

        // And ensure that the PersistServiceProvider's state is cleared also or we'll get funky assertions
        // from the PersistService.
        containerTest.stopService(IPersistServiceProvider.class);
    }

    /**
     * Common DB initialization for primary and secondary DBs.  Create the tables not managed by hibernate.
     *
     * @param createNew         Pass {@code true} if this is a "new" database (i.e., new project)
     * @param connectionManager DB Connection manager
     * @since 2.29
     */
    private static void initializeDatabase(boolean createNew, IDatabaseConnectionManager connectionManager)
    {
        if (createNew)
        {
            final MessagePersistHelper messagePersistHelper = BEAN_ACCESSOR.getBean(MessagePersistHelper.class);

            // Create the tables we manage directly
            connectionManager.performUpgradeStatements(messagePersistHelper.buildRDMCreateStatements());
            connectionManager.performUpgradeStatements(StructureAssociation.buildTableDefinition());
        }
    }
}
