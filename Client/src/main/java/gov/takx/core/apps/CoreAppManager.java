package gov.takx.core.apps;

import gov.takx.api.apps.IAppInstance;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.app.AppEvent;
import gov.takx.api.event.app.AppInstalled;
import gov.takx.api.event.app.AppStarted;
import gov.takx.api.event.app.AppStopped;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.app.IAppListener;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDefinition;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Core app manager.  Responsible for the installation, un-installation, and lifecycle management of all apps.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class CoreAppManager implements ICoreAppManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IPreferenceManager preferenceManager;
    private final PluginRegistryManager pluginRegistryManager;
    private final IPluginStatePersist pluginStatePersist;
    private final Event<AppInstalled> appInstalledEvent;

    private final Map<String, AppInstance> apps = new ConcurrentHashMap<>();
    private final List<IAppListener> listeners = new CopyOnWriteArrayList<>();
    private final PreferencesNode appConfigurationNode = PreferenceEntry.APPS_CONFIGURATION_NODE.node();

    @Inject
    CoreAppManager(IPreferenceManager preferenceManager, PluginRegistryManager pluginRegistryManager,
                   IPluginStatePersist pluginStatePersist, Event<AppInstalled> appInstalledEvent)
    {
        this.preferenceManager = preferenceManager;
        this.pluginRegistryManager = pluginRegistryManager;
        this.pluginStatePersist = pluginStatePersist;
        this.appInstalledEvent = appInstalledEvent;
    }

    @Override
    @Nullable
    public IAppInstance getAppInstance(@Nonnull IAppPlugin app)
    {
        return apps.get(app.getUniqueKey());
    }

    @Override
    @Nonnull
    public Collection<IAppInstance> getApps()
    {
        return new ArrayList<>(apps.values());
    }

    @Nullable
    @Override
    public IImmutableAppInstance getApp(@Nonnull String family, @Nonnull String type)
    {
        return apps.get(KeyUtils.getUniqueKey(family, type));
    }

    @Override
    @Nullable
    public IImmutableAppInstance getInstalledAppByClass(@Nonnull Class<?> appClass)
    {
        return apps.values().stream()
                .filter(appInstance -> appInstance.getApp().getClass() == appClass)
                .findAny()
                .orElse(null);
    }

    /**
     * Auto-install all App plugins.
     * @since 5.7
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        // Use pluginRegistryManager so we are assured that all plugins are loaded before we obtain the collection
        pluginRegistryManager.getAppPluginRegistry().getPluginDefinitions().forEach(this::addAppInstance);
        preferenceManager.savePreferences();
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_CoreAppManager) StopProjectServices event)
    {
        event.getProgressMonitor().setText("Shutdown apps...");

        stopAllApps();
        shutdownAllApps();

        apps.clear();
    }

    /**
     * Handle any hot deployed plugins.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        if (pluginAdded.getPluginCategory() == PluginCategory.AppPlugin)
        {
            final var appInstance = addAppInstance((IAppPluginDefinition) pluginAdded.pluginDefinition());
            if (appInstance != null)
            {
                // Auto-start the App as configured
                processAutoStartPlugins(List.of(appInstance), null);

                final var appInstalled = new AppInstalled(appInstance);
                appInstalledEvent.fire(appInstalled);
                notifyAppListeners(appInstalled, IAppListener::onAppInstalled);
            }
        }
    }

    /**
     * Create a new {@link AppInstance} according to the given {@code pluginDefinition}.  Also cleans out any old
     * {@code "install"} entries form our configuration node.
     *
     * @return New AppInstance, {@code null} if the app didn't deploy properly
     * @since 5.7
     */
    @Nullable
    private AppInstance addAppInstance(@Nonnull IAppPluginDefinition pluginDefinition)
    {
        final var app = pluginDefinition.getInstance();
        if (app == null) // App can be null if plugin container failed to deploy
        {
            logger.debug("App instance was null: {}", pluginDefinition);
            return null;
        }

        final String appKey = app.getUniqueKey();

        // See if it has an auto-start configuration, if not, use the plugin definitions start mode
        var config = appConfigurationNode.get(appKey, null);
        if (config == null) config = pluginDefinition.getAutoStartMode().name();

        if ("install".equalsIgnoreCase(config)) // Remove old "install" nodes
        {
            appConfigurationNode.remove(appKey);
            config = AutoStartMode.none.name();
        }

        var autoStartMode = AutoStartMode.getByValue(config);
        if (autoStartMode == null)
        {
            autoStartMode = pluginDefinition.getAutoStartMode(); // Revert to the plugin definition setting
            logger.warn("Unknown AutoStartMode: '{}'; using '{}' for App {}", config, autoStartMode, appKey);
        }

        final var appInstance = new AppInstance(pluginDefinition, app, autoStartMode);
        apps.put(appKey, appInstance);
        return appInstance;
    }

    /**
     * Start any apps configured to auto-start.
     */
    private void startPlugins(@Observes @Priority(ServiceOrdering.START_PLUGINS_CoreAppManager) StartPlugins event)
    {
        final var progressMonitor = event.getProgressMonitor();
        progressMonitor.setText("Starting apps...");
        processAutoStartPlugins(apps.values(), progressMonitor);
    }

    /**
     * Process the given list of app instances, and auto-start as configured.
     */
    private void processAutoStartPlugins(@Nonnull Collection<AppInstance> appInstances, @Nullable IProgressMonitor progressMonitor)
    {
        appInstances.stream()
                .filter(appInstance -> appInstance.getAutoStartMode() != AutoStartMode.none)
                .forEach(appInstance -> startApp(appInstance, progressMonitor));
    }

    private void startApp(@Nonnull IAppInstance appInstance, @Nullable IProgressMonitor progressMonitor)
    {
        if (progressMonitor != null) progressMonitor.setSubstatus("Starting " + appInstance.getName());
        try
        {
            // Load project-specific state.
            final var state = pluginStatePersist.query(appInstance.getApp().getUniqueKey());
            if (state != null) appInstance.restoreState(state);

            appInstance.start();
        } catch (Exception e)
        {
            logger.error("Unable to auto start app {}", appInstance.getName(), e);

            displayRemoveAppOptionDialog(appInstance.getApp().getUniqueKey(),
                    "<html>Unable to auto start app: '" + appInstance.getName() + "'<br><br>" +
                            "Would you like to remove the app from the auto-load list?");
        }
    }

    private void displayRemoveAppOptionDialog(final String appKey, final String message)
    {
        final int response = AlwaysOnTopJOptionPane.showConfirmDialog(null, message, "Error starting app", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION)
        {
            // Remove the app from the autoload list
            appConfigurationNode.remove(appKey);
            preferenceManager.savePreferences();
        }
    }

    /**
     * Legacy support for deprecated app install mechanism.  All apps are auto-installed as of 5.7, but we still need
     * to apply the auto-start settings the caller specified.
     */
    @SuppressWarnings("removal")
    @Override
    public IImmutableAppInstance installApp(@Nonnull String family, @Nonnull String type, @Nonnull String version, boolean autoStart, boolean background) throws IllegalArgumentException
    {
        final var key = KeyUtils.getUniqueKey(family, type);
        final var appInstance = apps.get(key);
        if (appInstance != null)
        {
            final var mode = autoStart ? (background ? AutoStartMode.background : AutoStartMode.start) : AutoStartMode.none;
            appInstance.setAutoStartMode(mode);
        } else
        {
            logger.warn("App instance not found: '{}'", key);
        }
        return appInstance;
    }

    /**
     * Stop all app plugins that are currently in a running state.
     */
    private void stopAllApps()
    {
        runningAppInstances().forEach(app -> {
            try
            {
                app.stop();
            } catch (Throwable e)
            {
                logger.warn("Exception stopping app: '{}'", app.getName(), e);
            }
        });
    }

    /**
     * Notify all the app plugins that TAKX is shutting down.
     */
    private void shutdownAllApps()
    {
        apps.values().forEach(app -> {
            try
            {
                app.shutdown();
            } catch (final Throwable e)
            {
                logger.warn("Exception stopping app: '{}'", app.getName(), e);
            }
        });
    }

    private void onAppStarted(@Observes AppStarted event)
    {
        notifyAppListeners(event, IAppListener::onAppStarted);
    }

    private void onAppStopped(@Observes AppStopped event)
    {
        notifyAppListeners(event, IAppListener::onAppStopped);
    }

    @Override
    public void setAutoStartMode(@Nonnull IImmutableAppInstance appInstance, @Nonnull AutoStartMode autoStartMode)
    {
        ((IAppInstance) appInstance).setAutoStartMode(autoStartMode);
    }

    @Override
    public void saveAutoStartMode(@Nonnull IAppInstance appInstance)
    {
        final var autoStartMode = appInstance.getAutoStartMode();
        final var key = appInstance.getApp().getUniqueKey();

        if (autoStartMode != AutoStartMode.none)
        {
            appConfigurationNode.put(key, autoStartMode.name());
        } else
        {
            appConfigurationNode.remove(key);
        }
        preferenceManager.savePreferences();
    }

    @Override
    public void addListener(@Nonnull IAppListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull IAppListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Notify all registered app listeners of an event.
     *
     * @param event          The AppEvent, from which to get the app instance and event name
     * @param listenerMethod Method to invoke with event instance
     * @since 2.25
     */
    private void notifyAppListeners(AppEvent event, BiConsumer<IAppListener, IImmutableAppInstance> listenerMethod)
    {
        logger.debug("Notify: {}", event);

        final var appInstance = event.getInstance();
        ExceptionUtils.safeDoOperation(listeners, l -> listenerMethod.accept(l, appInstance));
    }

    /**
     * Handle the saving event, save our state.
     *
     * @since 2.37
     */
    private void handleSaving(@Observes ProjectSaving ignored)
    {
        runningAppInstances().forEach(appInstance -> {
            try
            {
                // Save the app state (if any)
                final var app = appInstance.getApp();
                pluginStatePersist.saveOrUpdateState(app.getUniqueKey(), app);
            } catch (Exception e)
            {
                logger.warn("Exception saving app state: {}", appInstance.getUniqueKey(), e);
            }
        });
    }

    /**
     * @return A stream of all running app instances
     * @since 2.25
     */
    private Stream<AppInstance> runningAppInstances()
    {
        return apps.values().stream().filter(AppInstance::isRunning);
    }
}
