package gov.takx.core.database.provider;

import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.api.persist.IDatabaseServer;
import gov.takx.platform.preferences.PreferencesNode;

import java.io.File;

/**
 * Abstract base for database servers.
 *
 * @since 3.0
 */
public abstract class ADatabaseServer implements IDatabaseServer
{
    /**
     * Name of the database used for persistence by the core.
     * @since 5.4
     */
    public static final String CORE_DB_NAME = "takx_core";

    protected static final int NO_PID = -1; // Indicates no PID was created/available for a server

    private final ServerSpecification serverSpecification;

    private IDatabaseConnectionManager connectionManager;

    protected ADatabaseServer(ServerSpecification serverSpecification)
    {
        this.serverSpecification = serverSpecification;
    }

    protected ServerSpecification getServerSpecification()
    {
        return serverSpecification;
    }

    /**
     * @return The root directory (the directory in which DB data directories should be created/located)
     */
    protected File getRootDirectory()
    {
        return serverSpecification.rootDir();
    }

    @Override
    public boolean isUpgraded()
    {
        return false;
    }

    @Override
    public PreferencesNode getConfiguration()
    {
        return serverSpecification.configuration();
    }

    @Override
    public synchronized IDatabaseConnectionManager getConnectionManager()
    {
        if (connectionManager == null)
        {
            connectionManager = createDatabaseConnectionManager();
            createDatabase();
        }
        return connectionManager;
    }

    protected abstract void createDatabase();

    /**
     * Instantiates a new connection manager for the core database.
     *
     * @return New instance of an IDatabaseConnectionManager
     */
    protected abstract IDatabaseConnectionManager createDatabaseConnectionManager();

    @Override
    public void shutdown()
    {
        if (connectionManager != null) connectionManager.close();
        connectionManager = null;
    }
}
