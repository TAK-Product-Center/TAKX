package gov.takx.api.event.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;

/**
 * Event indicating that a TAK Server connection has been removed.
 *
 * @since 3.0
 */
public class TakServerConnectionRemoved extends TakServerConnectionEvent
{
    public TakServerConnectionRemoved(ITakServerConnection connection)
    {
        super(connection);
    }
}