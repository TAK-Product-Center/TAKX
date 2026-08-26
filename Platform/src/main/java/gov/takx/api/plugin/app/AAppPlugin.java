package gov.takx.api.plugin.app;

import gov.takx.api.plugin.ARaptorPlugin;
import gov.takx.api.plugin.annotations.AppPlugin;

/**
 * Abstract base implementation for App plugins.  Developers of plugins are encouraged to extend this
 * base class instead of implementing all the required methods directly.
 *
 * @since 2.0.0
 */
public abstract class AAppPlugin extends ARaptorPlugin<AppPlugin> implements IAppPlugin
{
    /**
     * Delegate that provides access to the TAKX platform.
     */
    protected IAppPluginDelegate delegate;

    /**
     * Default constructor when the sub-class needs to extract data to determine the family, type, and version.
     */
    protected AAppPlugin()
    {
        super(AppPlugin.class);
    }

    @Override
    public void setAppDelegate(IAppPluginDelegate delegate)
    {
        this.delegate = delegate;
        onDelegateInjected();
    }

    /**
     * Event triggered immediately after the IAppPluginDelegate has been injected into the controller.  This event
     * can be used by classes to perform any setup once the delegate has been injected.
     */
    protected void onDelegateInjected()
    {
        // default to empty
    }

    /**
     * Default implementation, returns {@code null}.
     *
     * @return {@code null}
     */
    @Override
    public String store()
    {
        return null;
    }

    /**
     * Default implementation, does nothing.
     */
    @Override
    public void restore(String s)
    {
        // default to empty
    }

    /**
     * Default implementation.  No warning will be displayed when requested to stop
     *
     * @return {@code null}
     */
    @Override
    public String displayWarningOnStop()
    {
        return null;
    }
}
