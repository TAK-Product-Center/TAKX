package gov.takx.api.event.app;

import gov.takx.api.plugin.app.IImmutableAppInstance;

/**
 * Fired when an app is installed.
 * Event fired when an App plugin is installed.
 * <p/>
 * Note that this event is never fired during service startup.  It is only fired when a plugin jar is hot deployed.
 *
 * @since 2.0.0
 */
public class AppInstalled extends AppEvent
{
    /**
     * Constructor.
     *
     * @param instance App instance targeted by this event
     */
    public AppInstalled(IImmutableAppInstance instance)
    {
        super(instance);
    }
}
