package gov.tak.core.location;

import gov.tak.api.location.ILocationProvider;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.platform.location.providers.FixedLocationProvider;
import gov.tak.platform.location.providers.NoLocationProvider;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.location.LocationChanged;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.module.IPluginModuleManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.platform.preferences.PreferenceEntry.LOCATION_PROVIDER_CONFIGURATIONS_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.SELECTED_LOCATION_PROVIDER;

/**
 * Core location provider management service.  This service manages a list of registered location providers,
 * one of which is always 'active'.  This service also manages the lifecycle of the location provider
 * instances as they are activated/deactivated.
 *
 * @since 3.0
 */
@ApplicationScoped
public class LocationProviderManager implements ILocationProviderManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String KEY_MUST_NOT_BE_NULL = "'locationProvider.getUniqueKey()' must not be null";
    private static final String DISPLAY_NAME_MUST_NOT_BE_NULL = "'locationProvider.getDisplayName()' must not be null";

    private static final String NO_LOCATION_PROVIDER_KEY = NoLocationProvider.KEY;
    private static final Package BUILTIN_PROVIDER_PACKAGE = NoLocationProvider.class.getPackage();

    private final IUserProxy userProxy;
    private final Event<LocationChanged> locationChangedEvent;
    private final IPluginModuleManager pluginModuleManager;

    // All registered location providers
    private final Map<String, ILocationProvider> locationProviders = new ConcurrentHashMap<>();

    // Currently active provider, note this is compared using identity not equality
    private ILocationProvider activeLocationProvider;

    @Inject
    LocationProviderManager(IUserProxy userProxy, Event<LocationChanged> locationChangedEvent,
                            @Any Instance<ILocationProvider> coreProviders, IPluginModuleManager pluginModuleManager)
    {
        this.userProxy = userProxy;
        this.locationChangedEvent = locationChangedEvent;
        this.pluginModuleManager = pluginModuleManager;

        resetProviders(coreProviders);
    }

    @Override
    public void registerLocationProvider(@Nonnull ILocationProvider locationProvider)
    {
        final String key = Objects.requireNonNull(locationProvider.getUniqueKey(), KEY_MUST_NOT_BE_NULL);

        if (logger.isDebugEnabled()) logger.debug(providerAndKey("register", locationProvider));

        if (locationProviders.containsKey(key))
        {
            throw new IllegalArgumentException("A location provider is already registered with key '" + key + "'");
        }

        locationProviders.put(key, locationProvider);

        // Install any saved configuration we have
        final String configuration = LOCATION_PROVIDER_CONFIGURATIONS_NODE.node().get(key, null);
        if (!StringUtils.isEmpty(configuration, true))
        {
            locationProvider.setRestoredState(configuration);
        }
    }

    @Override
    public void unregisterLocationProvider(@Nonnull ILocationProvider locationProvider)
    {
        final String key = Objects.requireNonNull(locationProvider.getUniqueKey(), KEY_MUST_NOT_BE_NULL);
        final String displayName = Objects.requireNonNull(locationProvider.getDisplayName(), DISPLAY_NAME_MUST_NOT_BE_NULL);

        if (logger.isDebugEnabled()) logger.debug(providerAndKey("unregister", locationProvider));

        // Ensure nothing tries to remove the NoLocationProvider
        if (NO_LOCATION_PROVIDER_KEY.equals(key))
        {
            throw new IllegalArgumentException("location provider '" + key + "' cannot be unregistered");
        }

        locationProviders.remove(key);

        if (activeLocationProvider == locationProvider)
        {
            final String message = "Active location provider '" + displayName + "' unregistered; installing 'No Location' provider";
            userProxy.reportWarning("Location Provider Removed", message);

            logger.info(message);

            setActiveLocationProvider(NO_LOCATION_PROVIDER_KEY);
        }
    }

    @Nonnull
    @Override
    public Collection<ILocationProvider> getLocationProviders()
    {
        return new ArrayList<>(locationProviders.values());
    }

    @Nullable
    @Override
    public ILocationProvider getRegisteredLocationProvider(@Nonnull String locationProviderKey)
    {
        return locationProviders.get(locationProviderKey);
    }

    @Nonnull
    @Override
    public ILocationProvider getActiveLocationProvider()
    {
        return activeLocationProvider;
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentLocation()
    {
        return activeLocationProvider.getCurrentLocation();
    }

    @Override
    public void setFixedLocation(@Nonnull GeoPoint location)
    {
        final ILocationProvider provider = getRegisteredLocationProvider(FixedLocationProvider.KEY);
        if (provider instanceof FixedLocationProvider fixedLocationProvider)
        {
            fixedLocationProvider.configureFrom(location);

            // If the fixed location provider is not already the active provider, then make it so
            if (activeLocationProvider != fixedLocationProvider) setActiveLocationProvider(FixedLocationProvider.KEY);
        } else
        {
            logger.error("FixedLocationProvider is of wrong type: {}", provider != null ? provider.getClass() : "null");
        }
    }

    @Override
    public void setActiveLocationProvider(@Nonnull String locationProviderKey)
    {
        final ILocationProvider locationProvider = locationProviders.get(locationProviderKey);

        // Ensure this provider has been registered
        if (locationProvider == null)
        {
            throw new IllegalArgumentException("Unknown location provider '" + locationProviderKey + "'");
        }

        // Ensure that the provider is configured for use
        if (locationProvider.isConfigurable() && !isProviderConfigured(locationProvider))
        {
            throw new IllegalStateException(providerAndKey("location provider has not been configured", locationProvider));
        }

        if (logger.isDebugEnabled()) logger.debug(providerAndKey("INSTALL", locationProvider));

        // We may need to fire a LocationChanged event when changing active providers, so get the current location
        final GeoPointMetaData oldLocation = getCurrentLocation();

        if (activeLocationProvider != locationProvider)
        {
            deactivateActiveLocationProvider();
            activateLocationProvider(locationProvider);
        }

        saveConfigurationToPreferences(); // In case only the configuration changed

        final GeoPointMetaData newLocation = getCurrentLocation();

        // If the location has changed, fire the event
        if (!Objects.equals(oldLocation, newLocation))
        {
            locationChangedEvent.fire(new LocationChanged(oldLocation, newLocation, true));
        }
    }

    /**
     * Determine if the given provider is configured.  Protect against exceptions coming from the provider
     * as it tries to intern its configuration.
     *
     * @param locationProvider Provider to test
     * @return {@code true} if the provider is configured
     */
    private boolean isProviderConfigured(ILocationProvider locationProvider)
    {
        try
        {
            return !locationProvider.getConfiguration().isEmpty();
        } catch (Exception e)
        {
            logger.warn(providerAndKey("Failure testing configuration of", locationProvider), e);
            return false;
        }
    }

    /**
     * Deactivate the active provider, saving its configuration before calling stop on it.
     */
    private void deactivateActiveLocationProvider()
    {
        if (logger.isDebugEnabled()) logger.debug(providerAndKey("STOP", activeLocationProvider));

        saveConfigurationToPreferences();

        try
        {
            activeLocationProvider.stop();
        } catch (Exception e)
        {
            logger.warn(providerAndKey("Failed to stop provider", activeLocationProvider), e);
        }
    }

    /**
     * Save the current settings (provider and configuration) to the preferences.
     * @since 5.4
     */
    private void saveConfigurationToPreferences()
    {
        final String restorableState = activeLocationProvider.getRestorableState();
        if (!StringUtils.isEmpty(restorableState))
        {
            LOCATION_PROVIDER_CONFIGURATIONS_NODE.node().put(activeLocationProvider.getUniqueKey(), restorableState);
        }

        LOCATION_PROVIDER_CONFIGURATIONS_NODE.getPreferenceManager().savePreferences();
    }

    /**
     * Set the given location provider as active, install any saved configuration, and start it.
     *
     * @param locationProvider Provider to make our active provider
     */
    private void activateLocationProvider(ILocationProvider locationProvider)
    {
        if (logger.isDebugEnabled()) logger.debug(providerAndKey("START", locationProvider));

        activeLocationProvider = locationProvider;

        try
        {
            activeLocationProvider.start();

            // Record selection in preferences
            SELECTED_LOCATION_PROVIDER.put(locationProvider.getUniqueKey());

            saveConfigurationToPreferences();
        } catch (Exception e)
        {
            logger.warn(providerAndKey("Failed to start provider", locationProvider), e);

            // Install the NoLocationProvider since the selected one failed
            setActiveLocationProvider(NO_LOCATION_PROVIDER_KEY);
        }
    }

    /**
     * Start our service.  Install and activate the previously selected provider.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_PLUGINS_LocationProviderManager) StartPlugins event)
    {
        // Locate and load all plugin-provided Location Providers
        pluginModuleManager.processDeploymentServices(ILocationProvider.class, (deployment, provider) -> {
            // Ensure we don't register any builtin providers.  Only necessary when under test, but not harmful
            // at normal runtime.

            if (provider.getClass().getPackage() != BUILTIN_PROVIDER_PACKAGE)
            {
                registerLocationProvider(provider);
            }
        });

        final String selectedProviderKey = SELECTED_LOCATION_PROVIDER.getStringValue();

        if (!locationProviders.containsKey(selectedProviderKey))
        {
            logger.error("Previously selected location provider '{}' is not registered; using 'No Location' provider instead", selectedProviderKey);

            setActiveLocationProvider(NO_LOCATION_PROVIDER_KEY);
        } else
        {
            setActiveLocationProvider(selectedProviderKey);
        }
    }

    /**
     * Stop our service.  Deactivate the current provider.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event,
                             @Any Instance<ILocationProvider> coreProviders)
    {
        deactivateActiveLocationProvider();

        resetProviders(coreProviders);
    }

    /**
     * Reset our provider list to the known built-in instances and install the NoLocationProvider as active.
     *
     * @param coreProviders Core built-in provider instances
     */
    private void resetProviders(Instance<ILocationProvider> coreProviders)
    {
        locationProviders.clear(); // Clear out all registered providers

        // Locate and register the builtin location providers.  The check for the containing package is not
        // necessary at normal runtime, but without it, when running tests, non-builtin providers are included
        // in the coreProviders instance list, and we don't want to register them.

        coreProviders.stream()
                .filter(p -> p.getClass().getPackage() == BUILTIN_PROVIDER_PACKAGE)
                .forEach(this::registerLocationProvider);

        // Install 'No Location' as the default until we're started
        activeLocationProvider = locationProviders.get(NO_LOCATION_PROVIDER_KEY);
    }

    /**
     * @return A string with the given prefix and the provider key and string form
     */
    private String providerAndKey(String pfx, ILocationProvider provider)
    {
        return pfx + " '" + provider.getUniqueKey() + "': " + provider;
    }
}
