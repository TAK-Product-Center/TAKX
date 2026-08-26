package gov.takx.api.event.location;

import gov.tak.api.location.ILocationProvider;
import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Event fired by a {@link ILocationProvider location provider} when its connection state changes.
 * <p>
 * Some location providers must make a connection to an external device/service in order to
 * obtain location updates (e.g., to a USB connected GPS puck).  If that connection is lost, or errors out in
 * some fashion, then an instance of this event should be fired by the location provider to alert the system of
 * that change.  This state change will be presented to the user in an appropriate manner (like a popup or toast).
 *
 * @since 3.0
 */
public class ConnectionStateChanged implements PlatformEvent
{
    /**
     * Enum specifying the state of a location provider connection.
     */
    public enum ConnectionState
    {
        Connected,
        Disconnected,
        Error
    }

    private final ILocationProvider source;
    private final ConnectionState connectionState;
    private final String message;

    /**
     * @param source          Source location provider of this event
     * @param connectionState New connection state
     * @param message         Message for use in logging and to present to the user describing the new state
     */
    public ConnectionStateChanged(@Nonnull ILocationProvider source, @Nonnull ConnectionState connectionState, @Nullable String message)
    {
        this.source = source;
        this.connectionState = connectionState;
        this.message = message;
    }

    /**
     * @return Source location provider of this event
     */
    @Nonnull
    public ILocationProvider getSource()
    {
        return source;
    }

    /**
     * @return The connection state for the provider
     */
    @Nonnull
    public ConnectionState getConnectionState()
    {
        return connectionState;
    }

    /**
     * @return Display message associated with the connection state change
     */
    @Nullable
    public String getMessage()
    {
        return message;
    }

    @Override
    public String toString()
    {
        return "ConnectionStateChanged{" +
                "source=" + source +
                ", connectionState=" + connectionState +
                ", message='" + message + '\'' +
                '}';
    }
}
