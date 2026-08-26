package gov.tak.api.location;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Contract for the management of {@link ILocationProvider location providers}.  Via this interface location providers
 * can be registered, unregistered, and queried.  A single location provider can be specified as the <em>active</em>
 * provider, which will be used by other core services to obtain the current location.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ILocationProviderManager
{
    /**
     * Register a location provider for use.  Note that the registered provider may or may not actually be
     * put into service.  The determination of which provider to use is generally under user control.
     * <p>
     * If/When the provider is put into service, its {@link ILocationProvider#start()} method will be called.
     * <p>
     * The provider will be registered using its unique key (obtained from {@link ILocationProvider#getUniqueKey()}.
     * If another provider has already been registered under that same key, an IllegalArgumentException will be thrown.
     *
     * @param locationProvider Provider to register
     * @throws IllegalArgumentException If a provider is already registered with the same unique key
     */
    void registerLocationProvider(@Nonnull ILocationProvider locationProvider);

    /**
     * Unregister a location provider, removing it from potential use.  If the given location provider is
     * currently in service, it will be stopped, via a call to {@link ILocationProvider#stop()}, and the
     * "no location" provider will be installed as a default.
     * <p>
     * If the given location provider was never registered, then this call has no affect.
     *
     * @param locationProvider Provider to unregister
     */
    void unregisterLocationProvider(@Nonnull ILocationProvider locationProvider);

    /**
     * @return Collection of all registered location providers
     */
    @Nonnull
    Collection<ILocationProvider> getLocationProviders();

    /**
     * Get a previously registered location provider with the given key.
     *
     * @param locationProviderKey Key of provider to obtain
     * @return location provider, or {@code null} if no provider is registered with the given key
     * @since 3.4.1
     */
    @Nullable
    ILocationProvider getRegisteredLocationProvider(@Nonnull String locationProviderKey);

    /**
     * Install the named location provider as the <em>active</em> provider.  The previously active provider
     * will be shutdown (via a call to {@link ILocationProvider#stop()}).  The named location provider will be
     * activated via a call to {@link ILocationProvider#start()}.
     * <p>
     * The named provider must have previously been registered via {@link #registerLocationProvider}, which
     * may happen automatically as part of plugin scanning and deployment.
     *
     * @param locationProviderKey Location provider to install and activate
     * @throws IllegalArgumentException If the given provider was not previously registered
     * @throws IllegalStateException    If the given provider is not properly configured
     */
    void setActiveLocationProvider(@Nonnull String locationProviderKey) throws IllegalArgumentException, IllegalStateException;

    /**
     * @return The current <em>active</em> location provider
     */
    @Nonnull
    ILocationProvider getActiveLocationProvider();

    /**
     * @return The current location as reported by the active location provider
     * @see ILocationProvider#getCurrentLocation()
     */
    @Nullable
    GeoPointMetaData getCurrentLocation();

    /**
     * Install the {@link gov.tak.platform.location.providers.FixedLocationProvider Fixed Location Provider} and
     * configure it with the given location.
     *
     * @param location New location to report
     * @since 5.2
     */
    void setFixedLocation(@Nonnull GeoPoint location);
}
