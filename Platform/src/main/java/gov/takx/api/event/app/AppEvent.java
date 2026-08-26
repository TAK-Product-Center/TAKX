package gov.takx.api.event.app;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.plugin.app.IImmutableAppInstance;

/**
 * Base event for app activities.  The {@link IImmutableAppInstance app instance} that is the subject of this event
 * may be obtained using {@link #getInstance()}.
 *
 * @since 2.0.0
 */
public class AppEvent implements PlatformEvent
{
    protected final IImmutableAppInstance instance;

    /**
     * Constructor.
     *
     * @param instance App instance targeted by this event
     */
    protected AppEvent(IImmutableAppInstance instance)
    {
        this.instance = instance;
    }

    /**
     * @return The App instance that is the subject of this event
     */
    public IImmutableAppInstance getInstance()
    {
        return instance;
    }

    public String toString()
    {
        return getClass().getSimpleName() + "[" + instance + "]";
    }
}
