package gov.takx.api.event.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;

/**
 * Event indicating that a new TAK Server connection has been opened.
 *
 * @since 3.0
 */
public class TakServerConnectionOpened extends TakServerConnectionEvent
{
    public TakServerConnectionOpened(ITakServerConnection connection)
    {
        super(connection);
    }
}
