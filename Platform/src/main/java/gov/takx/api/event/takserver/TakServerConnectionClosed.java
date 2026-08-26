package gov.takx.api.event.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;

/**
 * Event indicating that a TAK Server connection has been closed.
 *
 * @since 3.0
 */
public class TakServerConnectionClosed extends TakServerConnectionEvent
{
    public TakServerConnectionClosed(ITakServerConnection connection)
    {
        super(connection);
    }
}
