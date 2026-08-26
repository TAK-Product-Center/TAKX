package gov.takx.api.event.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;

/**
 * Event indicating that a TAK Server connection has been changed.
 *
 * @since 3.0
 */
public class TakServerConnectionChanged extends TakServerConnectionEvent
{
    public TakServerConnectionChanged(ITakServerConnection connection)
    {
        super(connection);
    }
}