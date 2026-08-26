package gov.takx.api.event.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.takx.api.event.PlatformEvent;

/**
 * Abstract base for all TAK Server connection events.  This is a {@link PlatformEvent},
 * so it is visible to all plugins.
 *
 * @since 3.0
 */
public abstract class TakServerConnectionEvent implements PlatformEvent
{
    private final ITakServerConnection connection;

    protected TakServerConnectionEvent(ITakServerConnection connection)
    {
        this.connection = connection;
    }

    public ITakServerConnection getConnection()
    {
        return connection;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[connection=" + connection + "]";
    }
}
