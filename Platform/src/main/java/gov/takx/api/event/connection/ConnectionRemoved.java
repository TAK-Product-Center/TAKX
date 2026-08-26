package gov.takx.api.event.connection;

import gov.takx.api.commservices.ICommService;
import gov.takx.api.plugin.commservices.ICommPath;

/**
 * Connection removed event, fired whenever a connection is removed.
 *
 * @since 2.0.0
 */
public class ConnectionRemoved extends ConnectionEvent
{
    public ConnectionRemoved(ICommPath commManager)
    {
        super(commManager);
    }

    public String toString()
    {
        return "ConnectionRemoved[" + ((ICommService) getCommManager()).getType().toString() + ", " + getCommManager().getName() + "]";
    }
}
