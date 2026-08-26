package gov.takx.api.persist;

import gov.takx.api.annotation.Internal;

/**
 * Contract for database providers.  A database provider manages database servers, which manage connections to a
 * named database.
 *
 * @since 3.0
 */
@Internal
public interface IDatabaseProvider
{
    /**
     * @return The type (identifier) of this provider
     */
    String getType();

    /**
     * @return the core database server, creating it if needed
     */
    IDatabaseServer getCoreServer();

    /**
     * Shutdown this provider, cascading down to the core server, and all connection managers.
     */
    void shutdown();
}
