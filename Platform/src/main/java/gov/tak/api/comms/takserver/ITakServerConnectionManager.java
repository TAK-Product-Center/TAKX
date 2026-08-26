package gov.tak.api.comms.takserver;

import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * Contract for the service that manages TAK Server connections.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ITakServerConnectionManager
{
    /**
     * Retrieves a list of available TAK Server connections.
     *
     * @return collection of TAK Server connections
     */
    default Collection<? extends ITakServerConnection> getTakServerConnections()
    {
        return getTakServerConnections(false);
    }

    /**
     * Retrieves a list of available TAK Server connections.
     *
     * @param connectedOnly Pass {@code true} to return only connected connections; {@code false} to return all connections
     * @return collection of TAK Server connections
     * @since 5.0
     */
    Collection<? extends ITakServerConnection> getTakServerConnections(boolean connectedOnly);

    /**
     * @return The connection with the given name, or {@code null} if not found
     */
    @Nullable
    ITakServerConnection findConnectionByName(@Nonnull String name);

    /**
     * @return The connection with the given UUID, or {@code null} if not found
     * @since 3.1
     */
    @Nullable
    ITakServerConnection findConnectionByUuid(@Nonnull UUID uuid);

    /**
     * Creates and adds a TAK Server connection defined by the provided streaming connection configuration.
     *
     * @param connectionConfig configuration for the streaming connection to be added
     * @return the added TAK Server connection
     * @since 3.5.0 (added return)
     */
    @Nonnull
    ITakServerConnection addTakServerConnection(@Nonnull StreamingConnectionConfiguration connectionConfig);

    /**
     * Updates the TAK Server connection using the new values.
     *
     * @param connectionId     The unique id of the connection to update
     * @param connectionConfig The updated configuration for the streaming connection
     */
    void updateTakServerConnection(@Nonnull UUID connectionId, @Nonnull StreamingConnectionConfiguration connectionConfig);

    /**
     * Remove streaming connection interface by id as an active TAK Server connection.
     *
     * @param connectionId id of streaming connection interface to be removed
     * @return {@code true} if the connection was successfully removed, {@code false} if it wasn't or could not be found.
     * @since 3.5.0
     */
    boolean removeTakServerConnection(@Nonnull UUID connectionId);
}
