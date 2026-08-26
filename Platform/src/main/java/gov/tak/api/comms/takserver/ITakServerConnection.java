package gov.tak.api.comms.takserver;

import com.atakmap.commoncommo.StreamingNetInterface;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Contract for a connection to a TAK Server instance.
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface ITakServerConnection
{
    /**
     * @return The unique id for the connection for the current project session.
     */
    @Nonnull
    UUID getId();

    /**
     * @return Human-readable name to display for this connection.
     */
    @Nonnull
    String getDisplayName();

    /**
     * @return The full URI for the connection, e.g. ssl://takserver:8089.
     */
    @Nonnull
    URI getUri();

    /**
     * @return {@code true} if this connection is currently connected.
     */
    boolean isConnected();

    /**
     * @return {@code true} if this connection is enabled.
     */
    boolean isEnabled();

    /**
     * @return The underlying streaming connection configuration for this server connection.
     * @since 3.1
     */
    @Nonnull
    StreamingConnectionConfiguration getConnectionConfig();

    /**
     * @return The time which this connection was created at.
     * @since 5.5.1
     */
    @Nonnull
    Instant getCreationTime();

    /**
     * @return The streaming interface or {@code null} if the server connection is not enabled
     * @since 5.7
     */
    @Nullable
    StreamingNetInterface getStreamingNetInterface();
}
