package gov.takx.core.apps;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.apps.IAppInstance;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.event.app.AppStarted;
import gov.takx.api.event.app.AppStopped;
import gov.takx.api.messages.IPluginState;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDefinition;
import gov.takx.api.plugin.app.IAppPluginManager.AutoStartMode;
import gov.takx.api.ui.IUserProxy;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Basically, the controller for an app.  Manages the AppPluginDelegate creation and disposal on start/stop.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class AppInstance implements IAppInstance, Comparable<AppInstance>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IAppPluginDefinition pluginDefinition;
    private final IAppPlugin app;
    private final AppPluginDelegate appDelegate;
    private final String key;

    private boolean running;
    private AutoStartMode autoStartMode;

    @Inject
    private ICoreAppManager appManager;

    @Inject
    private IUserProxy userProxy;

    @Inject
    private Event<AppStarted> appStartedEvent;

    @Inject
    private Event<AppStopped> appStoppedEvent;

    public AppInstance(@Nonnull IAppPluginDefinition pluginDefinition, @Nonnull IAppPlugin app, @Nonnull AutoStartMode autoStartMode)
    {
        this.pluginDefinition = pluginDefinition;
        this.app = app;
        this.autoStartMode = autoStartMode;

        key = KeyUtils.getUniqueKey(app.getFamily(), app.getType(), app.getVersion());

        appDelegate = new AppPluginDelegate(app);
        app.setAppDelegate(appDelegate);
    }

    @Override
    @Nonnull
    public AppPluginDelegate getAppDelegate()
    {
        return appDelegate;
    }

    @Override
    @Nonnull
    public String getUniqueKey()
    {
        return key;
    }

    @Override
    public String getName()
    {
        return app.getName();
    }

    @Override
    public String getVersion()
    {
        return app.getVersion();
    }

    @Override
    public String getDescription()
    {
        return app.getDescription();
    }

    @Override
    public String getFamily()
    {
        return app.getFamily();
    }

    @Override
    public String getType()
    {
        return app.getType();
    }

    @Override
    @Nonnull
    public IAppPlugin getApp()
    {
        return app;
    }

    @Override
    @Nonnull
    public IAppPluginDefinition getPluginDefinition()
    {
        return pluginDefinition;
    }

    @Override
    public boolean isRunning()
    {
        return running;
    }

    @Override
    public void start() throws Exception
    {
        try
        {
            app.start();
            running = true;
            appStartedEvent.fire(new AppStarted(this));
        } catch (Throwable e)
        {
            logger.error("Failed to start App {}", key, e);
            userProxy.reportError("App Failed to Start", key + " failed to start", e);
        }
    }

    @Override
    public void stop() throws Exception
    {
        try
        {
            app.stop();
        } catch (Throwable e)
        {
            logger.error("Failed to stop App {}", key, e);
            userProxy.reportError("App Failed to Stop", key + " failed to stop", e);
        }
        running = false;
        appStoppedEvent.fire(new AppStopped(this));
    }

    @SuppressWarnings("removal")
    @Override
    public void uninstall()
    {
        logger.warn("Apps cannot be uninstalled; ignoring for {}", key);
    }

    @Override
    public void shutdown() throws Exception
    {
        app.shutdown();
        appDelegate.dispose();
        running = false;
    }

    @Override
    public void restoreState(@Nonnull IPluginState state)
    {
        app.restore(state.getState());
    }

    @Override
    public void setAutoStartMode(@Nonnull AutoStartMode autoStartMode)
    {
        this.autoStartMode = autoStartMode;
        appManager.saveAutoStartMode(this);
    }

    @Override
    @Nonnull
    public AutoStartMode getAutoStartMode()
    {
        return autoStartMode;
    }

    @Override
    public boolean isAutoStart()
    {
        return autoStartMode == AutoStartMode.start || autoStartMode == AutoStartMode.background;
    }

    @Override
    public boolean isBackground()
    {
        return autoStartMode == AutoStartMode.background;
    }

    @Override
    public void setBackground(boolean background)
    {
        final var mode = background ? AutoStartMode.background : isAutoStart() ? AutoStartMode.start : AutoStartMode.none;
        setAutoStartMode(mode);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppInstance that = (AppInstance) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode()
    {
        return key.hashCode();
    }

    // This comparable is used to present an ordered list of missing installed apps in an enhanced package to the user.
    @Override
    public int compareTo(AppInstance o)
    {
        return key.compareTo(o.getUniqueKey());
    }

    public String toString()
    {
        return "appInstance=" + getUniqueKey();
    }
}
