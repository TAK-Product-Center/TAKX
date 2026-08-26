package gov.tak.core.comms.takserver;

import com.atakmap.commoncommo.StreamingNetInterface;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.takx.commons.utils.UUIDUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Instance of a connection to a TAK Server instance.  Instances may not actually be connected.
 *
 * @since 3.0
 */
public class TakServerConnection implements ITakServerConnection
{
    private final UUID id;
    private StreamingNetInterface streamingNetInterface;
    private StreamingConnectionConfiguration connectionConfig;
    private final Instant creationTime;
    private boolean connected;

    /**
     * Creates a TAK Server Connection for the provided commo streaming interface using the supplied id and configuration parameters.
     *
     * @param id The unique id for the connection (may be {@code null} to have one generated)
     * @param streamingNetInterface The commo streaming interface associated with the TAK Server connection (may be {@code null}
     *                              if the connection isn't enabled)
     * @param connectionConfig The configuration parameters for the connection
     */
    public TakServerConnection(@Nullable UUID id, @Nullable StreamingNetInterface streamingNetInterface,
                               @Nonnull StreamingConnectionConfiguration connectionConfig)
    {
        this.id = (id == null) ? UUIDUtils.generateType1UUID() : id;
        this.streamingNetInterface = streamingNetInterface;
        this.connectionConfig = connectionConfig;
        this.creationTime = Instant.ofEpochMilli(connectionConfig.getCreationTimestampMs());
        connected = false;
    }

    /**
     * Copy constructor (used for editing in UI so the original isn't modified accidentally).
     *
     * @param connection The connection to copy
     */
    public TakServerConnection(@Nonnull TakServerConnection connection)
    {
        id = connection.id;
        streamingNetInterface = connection.streamingNetInterface; // Shallow copy should be ok; if it ever changes, commo will give us a new one
        connectionConfig = new StreamingConnectionConfiguration(connection.connectionConfig);
        connected = connection.connected;
        creationTime = connection.creationTime;
    }

    @Nonnull
    @Override
    public UUID getId()
    {
        return id;
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return connectionConfig.getDisplayName();
    }

    @Nonnull
    @Override
    public URI getUri()
    {
        return connectionConfig.getUri();
    }

    @Override
    public boolean isConnected()
    {
        return connected;
    }

    public void setConnected(boolean connected)
    {
        this.connected = connected;
    }

    @Override
    public boolean isEnabled()
    {
        return connectionConfig.isEnabled();
    }

    @Nullable
    @Override
    public StreamingNetInterface getStreamingNetInterface()
    {
        return streamingNetInterface;
    }

    public void setStreamingNetInterface(@Nullable StreamingNetInterface streamingNetInterface)
    {
        this.streamingNetInterface = streamingNetInterface;
    }

    /**
     * @return The preference node describing the connection.
     */
    @Nonnull
    @Override
    public StreamingConnectionConfiguration getConnectionConfig()
    {
        return connectionConfig;
    }

    @Nonnull
    @Override
    public Instant getCreationTime()
    {
        return creationTime;
    }

    public void setConnectionConfig(@Nonnull StreamingConnectionConfiguration connectionConfig)
    {
        this.connectionConfig = connectionConfig;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TakServerConnection that = (TakServerConnection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString()
    {
        return "TakServerConnection{" +
                "name=" + getDisplayName() +
                ", uri=" + getUri() +
                ", connected=" + connected +
                ", enabled=" + isEnabled() +
                '}';
    }
}
