package gov.takx.api.event.app;

import gov.takx.api.plugin.app.IImmutableAppInstance;

/**
 * Fired when an app instance is started.
 *
 * @since 2.0.0
 */
public class AppStarted extends AppEvent
{
    public AppStarted(IImmutableAppInstance instance)
    {
        super(instance);
    }
}
