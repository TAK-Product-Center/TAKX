package gov.takx.api.event.connection;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.plugin.commservices.ICommPath;

/**
 * Abstract connection event
 *
 * @since 2.0.0
 */
public abstract class ConnectionEvent implements PlatformEvent
{
    private ICommPath commManager;

    public ConnectionEvent(ICommPath commManager)
    {
        this.commManager = commManager;
    }

    public ICommPath getCommManager()
    {
        return commManager;
    }
}
