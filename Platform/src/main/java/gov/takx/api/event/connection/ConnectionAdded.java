package gov.takx.api.event.connection;

import gov.takx.api.commservices.ICommService;
import gov.takx.api.plugin.commservices.ICommPath;

/**
 * Connection added event, fired whenever a new connection is added.
 *
 * @since 2.0.0
 */
public class ConnectionAdded extends ConnectionEvent
{
    public ConnectionAdded(ICommPath commManager)
    {
        super(commManager);
    }

    public String toString()
    {
        return "ConnectionAdded[" + ((ICommService) getCommManager()).getType().toString() + ", " + getCommManager().getName() + "]";
    }
}
