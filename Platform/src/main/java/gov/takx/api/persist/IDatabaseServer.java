package gov.takx.api.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.platform.preferences.PreferencesNode;

import java.util.List;

/**
 * Contract for database servers.  Servers may or may not need to actually "stand up" an external server process.
 * Database servers are responsible for creating {@link IDatabaseConnectionManager connection managers}, which
 * provide sessions/connections to a specific database.
 *
 * @since 3.0
 */
@NoPluginImplementations
@Internal
public interface IDatabaseServer
{
    /**
     * @return The configuration being used by this server.
     */
    PreferencesNode getConfiguration();

    /**
     * @return The "core" connection manager, creating it if it doesn't exist.
     */
    IDatabaseConnectionManager getConnectionManager();

    /**
     * Start the database server.  This is a logical operation, and it may not actually create an external server.
     *
     * @return Process id of the server, &lt; 0 if no process was created
     */
    long start();

    /**
     * Dump the contents of the database to a file.
     */
    void dumpData();

    /**
     * Restore the contents of a dump file to the database.
     */
    void restoreData();

    /**
     * Shutdown the server, stopping any external process that may have been started.
     */
    void shutdown();

    /**
     * @return {@code true} if the database storage was upgraded due to a major DB version change
     */
    boolean isUpgraded();

    /**
     * @return List of file/directory names to exclude when creating a project package.
     * @since 5.4
     */
    default List<String> getPackageExcludes()
    {
        return List.of();
    }
}
