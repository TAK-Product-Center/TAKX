package gov.tak.api.location;

import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.event.location.LocationChanged;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for location providers.
 * <p>
 * Location providers obtain/generate location information and make it available to the rest of the system.
 * In general, this is used to determine the current location of the device running TAKX for use in communications,
 * such as Situational Awareness message broadcasts.
 * <p>
 * The location will be polled by system services using {@link #getCurrentLocation()}.
 * <p>
 * Reactive services expect to receive a {@link LocationChanged} event in order to
 * respond to location changes.  It is therefore <b>required</b> that implementations fire said event when the location
 * they report changes. This will be done automatically during any configuration change in
 * {@link ALocationProvider#configure}, so a subclass that overrides the {@link #configure} method must either
 * call down to the base implementation, or call {@link ALocationProvider#fireLocationChanged} directly.
 * <p>
 * If the location changes at any other time, implementing classes should call
 * {@link ALocationProvider#fireLocationChanged}.
 * <p>
 * Implementing classes should set the {@link gov.tak.api.util.StandardAttributeNames#HOW} metadata value on the
 * {@link GeoPointMetaData location} instance prior to firing the location changed event.  This value will indicate
 * to the receivers how the data was generated, be it human entered or machine generated.  Expected value is one of
 * {@link gov.tak.api.cot.CotConstants#HOW_HUMAN_ENTERED},
 * {@link gov.tak.api.cot.CotConstants#HOW_MACHINE_GENERATED_PREFIX}, or
 * {@link gov.tak.api.cot.CotConstants#HOW_GPS_LOST_PREFIX}.
 * <p>
 * Instances must provide a globally unique key to identify the provider.  Typically, for plugins, this will be
 * the FQN of the provider class.  This key will be used to record the user's selection of location provider in
 * the program preferences.
 *
 * @see ILocationProviderManager
 * @see LocationChanged
 * @see GeoPointMetaData
 * @since 3.0
 */
public interface ILocationProvider
{
    /**
     * @return A unique key representing this provider.  This key will be used to record and
     * later restore a selected location provider.  It must be unique across all registered location providers.
     * This is typically the FQN of the provider's class.
     */
    @Nonnull
    String getUniqueKey();

    /**
     * @return A human-readable name to represent this provider
     */
    @Nonnull
    String getDisplayName();

    /**
     * @return The current location, may be {@code null} if the location is unknown/unavailable
     * @throws IllegalStateException If called prior to this provider being started via a call to {@link #start()}
     */
    @Nullable
    GeoPointMetaData getCurrentLocation() throws IllegalStateException;

    /**
     * @return {@code true} if this provider has been installed as the active location provider.
     * @see ILocationProviderManager#setActiveLocationProvider(String)
     */
    boolean isActive();

    /**
     * @return {@code true} if this provider can be configured by the user.  If {@code true} is returned,
     * then the provider must provide a configuration UI, and show it when {@link #showConfiguration} is called.
     */
    boolean isConfigurable();

    /**
     * @return {@code true} if the provider has been configured, either by the user or by restoring saved state
     */
    boolean isConfigured();

    /**
     * Present a configuration UI to the user and return a new configuration from their selection,
     * or {@code null} if the user cancels.
     *
     * @param configuration The configuration to display/edit.  This allows the preference panel to provide
     *                      a configuration which has not yet been installed into the location provider for the
     *                      user to start from.
     * @return New configuration (for use with {@link #configure}), or {@code null} if none
     * @since 3.3 (signature change)
     */
    @Nullable
    ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration);

    /**
     * Start acquiring location information.  The provider should avoid acquiring any resources prior to this call.
     */
    void start();

    /**
     * Stop acquiring location information.  Tear down and release any resources being used.
     */
    void stop();

    /**
     * Set the restored state, as pulled from program preferences.  This method will be called when the provider
     * is {@link ILocationProviderManager#registerLocationProvider(ILocationProvider) registered}, IFF there
     * is a non-empty state recorded in the preferences.
     *
     * @param restoredState State from preferences
     */
    void setRestoredState(@Nonnull String restoredState);

    /**
     * @return String to be placed in program preferences for future restoration
     * @see #setRestoredState(String)
     */
    @Nonnull
    String getRestorableState();

    /**
     * Install the given configuration.
     *
     * @param configuration Configuration to install
     * @see #getConfiguration()
     */
    void configure(@Nonnull ILocationProviderConfiguration configuration);

    /**
     * Get the current Configuration (i.e., what would be used if {@link #start()} were to be called).  Typically,
     * this is created from restored state or directly installed via {@link #configure}.
     *
     * @return Configuration state
     * @see #setRestoredState(String)
     */
    @Nonnull
    ILocationProviderConfiguration getConfiguration();

    /**
     * Reconfigure according to the given string.  The format must conform to the expected restoration state string.
     * <p/>
     * Note that the configuration string format is unique to each specific location provider, so this method is
     * <em>NOT</em> generically usable on any location provider.
     * <p/>
     * Not all location providers support this operation, and an {@link UnsupportedOperationException} will be thrown in
     * that case.
     *
     * @param configurationString New configuration
     * @see #setRestoredState(String)
     * @throws UnsupportedOperationException by default
     * @throws IllegalArgumentException If the given configuration string is not properly formed
     * @since 3.4.1
     */
    default void configureFrom(@Nonnull String configurationString) throws IllegalArgumentException
    {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " cannot be configured using 'configureFrom'");
    }
}
