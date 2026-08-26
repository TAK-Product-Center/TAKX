package gov.takx.api.event.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;

/**
 * Event indicating that a new TAK Server connection has been added.
 *
 * @since 3.0
 */
public class TakServerConnectionAdded extends TakServerConnectionEvent
{
    public TakServerConnectionAdded(ITakServerConnection connection)
    {
        super(connection);
    }
}