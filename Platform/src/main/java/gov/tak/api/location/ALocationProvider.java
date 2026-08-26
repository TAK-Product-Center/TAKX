package gov.tak.api.location;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.location.ConnectionStateChanged;
import gov.takx.api.event.location.ConnectionStateChanged.ConnectionState;
import gov.takx.api.event.location.LocationChanged;
import gov.takx.bootstrap.ApplicationModuleFinder;
import gov.takx.platform.cdi.ContainerBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.events.WeldEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Abstract base for implementing a location provider. Subclasses <b>MUST</b> either be
 * {@link jakarta.enterprise.context.ApplicationScoped} beans, or an object annotated with
 * {@link PseudoBean}.
 * <p>
 * See {@link #onStart()} and {@link #onStop()} if custom start/stop management is required.
 * <p>
 * Note that a location provider is assumed to start in a "connected" state (meaning it either doesn't require a
 * connection, or is connected at the time it is activated).  If this is not the case for a given provider, then
 * it should fire an appropriate event, using {@link #fireConnectionStateChanged}, in the
 * {@link #onStart()} method.
 *
 * @param <C> Our configuration type
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public abstract class ALocationProvider<C extends ILocationProviderConfiguration> implements ILocationProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String uniqueKey;
    private final String displayName;
    private final boolean configurable;
    private final boolean manualUpdate;

    protected String restoredState = "";
    protected C currentConfiguration;
    protected boolean active = false; // Not until we're activated (started)

    @Inject
    @PlatformUnderTest
    private boolean underTest;

    private Event<LocationChanged> locationChangedEvent;
    private Event<ConnectionStateChanged> connectionStateChangedEvent;

    /**
     * @param uniqueKey            Our unique key
     * @param displayName          Our display name
     * @param configurable         {@code true} if this provider is configurable
     * @param currentConfiguration Current (initial) configuration to install
     */
    protected ALocationProvider(String uniqueKey, String displayName, boolean configurable, C currentConfiguration)
    {
        this(uniqueKey, displayName, configurable, currentConfiguration, false);
    }

    /**
     * @param uniqueKey            Our unique key
     * @param displayName          Our display name
     * @param configurable         {@code true} if this provider is configurable
     * @param currentConfiguration Current (initial) configuration to install
     * @param manualUpdate         Pass {@code true} if location changes come from a manual (user-entered) location update
     * @since 3.3
     */
    protected ALocationProvider(String uniqueKey, String displayName, boolean configurable, C currentConfiguration, boolean manualUpdate)
    {
        this.uniqueKey = uniqueKey;
        this.displayName = displayName;
        this.configurable = configurable;
        this.manualUpdate = manualUpdate;

        this.currentConfiguration = currentConfiguration;
    }

    @PostConstruct
    @SuppressWarnings("resource")
    private void postConstruct()
    {
        // We need to fire events in the "core" container, not in a potential plugin's container
        final String containerId = underTest
                ? ContainerBuilder.TEST_CONTAINER_NAME
                : ApplicationModuleFinder.CLIENT_MODULE_ID;

        final WeldEvent<Object> eventSelector = WeldContainer.instance(containerId).event();

        locationChangedEvent = eventSelector.select(LocationChanged.class);
        connectionStateChangedEvent = eventSelector.select(ConnectionStateChanged.class);
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public boolean isConfigurable()
    {
        return configurable;
    }

    @Override
    public boolean isConfigured()
    {
        return !getConfiguration().isEmpty();
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Nonnull
    @Override
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public void setRestoredState(@Nonnull String restoredState)
    {
        this.restoredState = restoredState;
    }

    @Nonnull
    @Override
    public String getRestorableState()
    {
        return getConfiguration().getRestorableState();
    }

    @Override
    public void configure(@Nonnull ILocationProviderConfiguration configuration)
    {
        // Check our location before and after the configuration change, and fire a LocationChanged event if needed
        final GeoPointMetaData oldLocation = getCurrentLocation();

        //noinspection unchecked
        currentConfiguration = (C) configuration;

        final GeoPointMetaData newLocation = getCurrentLocation();

        // If the location has changed, fire the event
        // Note: GeoPointMetaData doesn't define an equals() method, so we need to compare the underlying points

        final GeoPoint oldPoint = oldLocation == null ? null : oldLocation.get();
        final GeoPoint newPoint = newLocation == null ? null : newLocation.get();

        if (!Objects.equals(oldPoint, newPoint))
        {
            fireLocationChanged(oldLocation, newLocation);
        }
    }

    /**
     * Fire a location changed event.
     *
     * @param oldLocation Previous location
     * @param newLocation Current location
     * @see LocationChanged
     */
    protected void fireLocationChanged(GeoPointMetaData oldLocation, GeoPointMetaData newLocation)
    {
        // Only fire the event if we are the active provider
        if (active)
        {
            locationChangedEvent.fire(new LocationChanged(oldLocation, newLocation, manualUpdate));
        } else
        {
            logger.debug("Suppressed LocationChange event for inactive provider: {}", uniqueKey);
        }
    }

    /**
     * Fire a {@link ConnectionStateChanged} event with this instance as the source.
     *
     * @param connectionState New connection state
     * @param message         Message for use in logging and to present to the user describing the new state
     */
    protected void fireConnectionStateChanged(@Nonnull ConnectionState connectionState, @Nullable String message)
    {
        final ConnectionStateChanged event = new ConnectionStateChanged(this, connectionState, message);
        logger.debug("{}", event);

        connectionStateChangedEvent.fire(event);
    }

    @Nonnull
    @Override
    public C getConfiguration()
    {
        // Some providers have a slight chicken and egg issue if they can't "internalize" the restored state
        // when it is set since the needed services are not spun up.  So, now, if asked for the configuration (which
        // will never happen before the services are started), we construct from restored state if needed.

        if (currentConfiguration.isEmpty() && !restoredState.isEmpty())
        {
            final C configuration = makeConfigurationFromRestoredState(restoredState);
            if (configuration != null) return configuration;
        }
        return currentConfiguration;
    }

    /**
     * Construct a Configuration from the given restored state string.
     *
     * @param restoredState State from which to build a Configuration
     * @return Configuration, if the state can be properly reified into a Configuration, {@code null} otherwise
     */
    @Nullable
    protected abstract C makeConfigurationFromRestoredState(String restoredState);

    /**
     * {@inheritDoc}
     * <p>
     * Installs the current configuration (which might come from restored state), and then calls {@link #onStart()}.
     * <p>
     * Note: Subclasses should not override this method.  Instead, they should override the {@code onStart()},
     * which is called by this method, for custom start behavior.
     */
    @Override
    public void start()
    {
        if (isConfigurable()) configure(getConfiguration());

        onStart();
        active = true;
    }

    /**
     * Called from {@link #start()}.  Subclasses should add custom "start" handling by overriding this method.
     */
    protected void onStart()
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: Subclasses should not override this method.  Instead, they should override the {@code onStop()},
     * which is called by this method, for custom stop behavior.
     */
    @Override
    public void stop()
    {
        onStop();
        active = false;
    }

    /**
     * Called from {@link #stop()}.  Subclasses should add custom "stop" handling by overriding this method.
     */
    protected void onStop()
    {
    }

    @Override
    public String toString()
    {
        return isConfigurable()
                ? getDisplayName() + " [" + currentConfiguration.getDisplayableString() + "]"
                : getDisplayName();
    }
}
