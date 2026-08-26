package gov.takx.api.event.app;

import gov.takx.api.plugin.app.IImmutableAppInstance;

/**
 * Fired when an app is uninstalled.
 *
 * @since 2.0.0
 */
public class AppUninstalled extends AppEvent
{
    /**
     * Constructor.
     *
     * @param instance App instance targeted by this event
     */
    public AppUninstalled(IImmutableAppInstance instance)
    {
        super(instance);
    }
}
