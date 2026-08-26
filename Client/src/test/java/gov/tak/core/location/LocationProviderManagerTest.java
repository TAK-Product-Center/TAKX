package gov.tak.core.location;

import com.google.errorprone.annotations.Keep;
import gov.tak.api.location.ILocationProvider;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.platform.location.providers.FixedLocationProvider;
import gov.tak.platform.location.providers.MapObjectTrackingLocationProvider;
import gov.tak.platform.location.providers.NetworkLocationProvider;
import gov.tak.platform.location.providers.NoLocationProvider;
import gov.tak.test.mock.EmptyAppPlugin;
import gov.tak.test.mock.TestPluginLocationProvider;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.event.location.ConnectionStateChanged;
import gov.takx.api.event.location.ConnectionStateChanged.ConnectionState;
import gov.takx.api.event.location.LocationChanged;
import gov.takx.core.view.gui.proxy.HeadlessUserProxy;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.TestLocationProvider;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static gov.takx.test.mock.TestLocationProvider.PROVIDER_KEY;
import static gov.takx.test.mock.TestLocationProvider.TEST_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests covering the location provider manager service.
 *
 * @since 3.0
 */
public class LocationProviderManagerTest extends AIntegrationTest
{
    private static final String CONFIG_STRING = "22, 42";
    private static final GeoPointMetaData NEW_LOCATION = new GeoPointMetaData(new GeoPoint(45, 90));
    private static final double LATITUDE = 22;
    private static final double LONGITUDE = 42;

    private final ILocationProviderManager locationProviderManager;
    private final Collection<ILocationProvider> builtInLocationProviders;

    public LocationProviderManagerTest()
    {
        // A little test oddity here.  Our "plugin" provided test location provider isn't a plugin, so we can
        // not directly register it.  However, we need at least 1 plugin registered so the PluginModuleManager
        // will correctly scan and find the test provider.  So, use EmptyAppPlugin to force this processing.

        setPluginsToRegister(EmptyAppPlugin.class);

        locationProviderManager = getBean(ILocationProviderManager.class);
        builtInLocationProviders = locationProviderManager.getLocationProviders();
    }

    @BeforeEach
    @SuppressLogging(HeadlessUserProxy.class)
    public void cleanupBeforeMethod()
    {
        // Unregister ALL non-builtin providers
        locationProviderManager.getLocationProviders().forEach(provider -> {
            if (provider instanceof TestLocationProvider)
            {
                locationProviderManager.unregisterLocationProvider(provider);
            }
        });

        // Ensure that no prior saved states remain
        PreferenceEntry.LOCATION_PROVIDER_CONFIGURATIONS_NODE.node().clear();

        // Restore the NO Location provider as the default
        PreferenceEntry.SELECTED_LOCATION_PROVIDER.put(NoLocationProvider.KEY);
    }

    @AfterAll
    void cleanupAfterAll()
    {
        // Restore the NO Location provider as the default
        PreferenceEntry.SELECTED_LOCATION_PROVIDER.put(NoLocationProvider.KEY);
    }

    @Test
    public void test_built_ins_are_registered()
    {
        assertThat(builtInLocationProviders).containsOnly(
                getBean(NoLocationProvider.class),
                getBean(FixedLocationProvider.class),
                getBean(MapObjectTrackingLocationProvider.class),
                getBean(NetworkLocationProvider.class));
    }

    @Test
    public void test_plugin_providers_are_auto_registered()
    {
        assertThat(locationProviderManager.getLocationProviders()).contains(getBean(TestPluginLocationProvider.class));
    }

    @Test
    public void test_activating_unknown_provider_fails()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> locationProviderManager.setActiveLocationProvider("SlartyBartFast"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test_registering_null_provider_fails()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> locationProviderManager.registerLocationProvider(null));
    }

    @Test
    public void test_double_registering_provider_fails()
    {
        final TestLocationProvider provider = new TestLocationProvider();

        locationProviderManager.registerLocationProvider(provider);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> locationProviderManager.registerLocationProvider(provider));
    }

    @Test
    public void test_activating_unconfigured_provider_fails()
    {
        final TestLocationProvider provider = new TestLocationProvider();

        locationProviderManager.registerLocationProvider(provider);
        assertThatIllegalStateException()
                .isThrownBy(() -> locationProviderManager.setActiveLocationProvider(PROVIDER_KEY));
    }

    @Test
    public void test_activating_registered_and_configured_provider_succeeds()
    {
        final TestLocationProvider provider = createAndActivateConfiguredProvider();

        final GeoPointMetaData currentLocation = locationProviderManager.getCurrentLocation();
        assertThat(currentLocation).isNotNull();
        assertThat(currentLocation.get().getLatitude()).isEqualTo(LATITUDE);
        assertThat(currentLocation.get().getLongitude()).isEqualTo(LONGITUDE);
        assertThat(currentLocation).isEqualTo(provider.getCurrentLocation());
    }

    @Test
    public void test_basic_lifecycle()
    {
        // Install some saved state to ensure it is restored
        PreferenceEntry.LOCATION_PROVIDER_CONFIGURATIONS_NODE.node().put(PROVIDER_KEY, CONFIG_STRING);

        final TestLocationProvider provider = new TestLocationProvider();
        locationProviderManager.registerLocationProvider(provider);

        assertThat(locationProviderManager.getLocationProviders()).contains(provider);
        assertThat(provider.getRestoredState()).isEqualToIgnoringCase(CONFIG_STRING);

        // Activate the provider and ensure start is called
        locationProviderManager.setActiveLocationProvider(PROVIDER_KEY);
        assertThat(locationProviderManager.getActiveLocationProvider()).isSameAs(provider);
        assertThat(provider.isStartCalled()).isTrue();
        assertThat(provider.isStopCalled()).isFalse();
        assertThat(provider.isActive()).isTrue();

        // Reconfigure the provider so we can validate that its "restorable state" is properly saved
        final String newConfigString = "-22, -42";
        provider.configure(new TestLocationProvider.TestConfiguration(newConfigString));

        provider.clear();

        // Activate another provider and ensure that stop gets called
        locationProviderManager.setActiveLocationProvider(NoLocationProvider.KEY);
        assertThat(locationProviderManager.getActiveLocationProvider()).isNotSameAs(provider);
        assertThat(provider.isStartCalled()).isFalse();
        assertThat(provider.isStopCalled()).isTrue();
        assertThat(provider.isActive()).isFalse();

        assertThat(PreferenceEntry.LOCATION_PROVIDER_CONFIGURATIONS_NODE.node().get(PROVIDER_KEY, ""))
                .isEqualTo(newConfigString);
    }

    @Test
    @SuppressLogging(HeadlessUserProxy.class)
    public void test_unregistering_deactivates_active_provider()
    {
        final TestLocationProvider provider = createAndActivateConfiguredProvider();

        locationProviderManager.unregisterLocationProvider(provider);
        assertThat(locationProviderManager.getActiveLocationProvider()).isNotSameAs(provider);
        assertThat(provider.isStopCalled()).isTrue();
        assertThat(provider.isActive()).isFalse();
    }

    @Test
    public void test_stopping_service_stops_active_provider()
    {
        final TestLocationProvider provider = createAndActivateConfiguredProvider();

        provider.clear();
        stopService(ILocationProviderManager.class);

        assertThat(provider.isStopCalled()).isTrue();
        assertThat(locationProviderManager.getActiveLocationProvider()).isNotSameAs(provider);
        assertThat(locationProviderManager.getLocationProviders()).doesNotContain(provider);

        // And re-start the service so the next test is in a known state
        PreferenceEntry.SELECTED_LOCATION_PROVIDER.put(NoLocationProvider.KEY); // reinstall the NoLocationProvider
        startServiceUsingStartPlugins(LocationProviderManager.class);
    }

    @Test
    @SuppressLogging(LocationProviderManager.class)
    public void test_exception_during_configure_is_caught()
    {
        final TestLocationProvider provider = new TestLocationProvider();
        locationProviderManager.registerLocationProvider(provider);

        provider.setThrowExceptionInGetConfiguration(true);
        assertThatIllegalStateException()
                .isThrownBy(() -> locationProviderManager.setActiveLocationProvider(PROVIDER_KEY));
    }

    @Test
    @SuppressLogging(LocationProviderManager.class)
    public void test_exception_during_start_is_caught()
    {
        final TestLocationProvider provider = new TestLocationProvider();
        locationProviderManager.registerLocationProvider(provider);
        provider.configure(new TestLocationProvider.TestConfiguration(CONFIG_STRING));

        provider.setThrowExceptionInStart(true);

        // This won't result in an exception (since it is handled by LPM), but it should result in the
        // NoLocationProvider being installed instead
        locationProviderManager.setActiveLocationProvider(PROVIDER_KEY);

        assertThat(locationProviderManager.getActiveLocationProvider().getUniqueKey()).isEqualTo(NoLocationProvider.KEY);
    }

    @Test
    public void test_location_changed_event_is_fired()
    {
        final TestLocationProvider provider = createAndActivateConfiguredProvider();
        final ProviderEventListener listener = new ProviderEventListener();

        final GeoPointMetaData currentLocation = provider.getCurrentLocation();
        assertThat(currentLocation).isNotNull();

        provider.fireLocationChanged(NEW_LOCATION);

        final LocationChanged locationChanged = listener.locationChanged;
        assertThat(locationChanged).isNotNull();

        final GeoPointMetaData oldLocation = locationChanged.oldLocation();
        assertThat(oldLocation).isNotNull();
        assertThat(oldLocation.get()).isEqualTo(currentLocation.get());

        final GeoPointMetaData newLocation = locationChanged.newLocation();
        assertThat(newLocation).isNotNull();
        assertThat(newLocation.get()).isEqualTo(NEW_LOCATION.get());

        listener.dispose();
    }

    @Test
    public void test_connection_state_change_event_is_fired()
    {
        final TestLocationProvider provider = createAndActivateConfiguredProvider();
        final ProviderEventListener listener = new ProviderEventListener();

        provider.fireConnectionStateChanged();

        final ConnectionStateChanged connectionStateChanged = listener.connectionStateChanged;
        assertThat(connectionStateChanged).isNotNull();
        assertThat(connectionStateChanged.getSource()).isSameAs(provider);
        assertThat(connectionStateChanged.getMessage()).isEqualTo(TEST_EVENT);
        assertThat(connectionStateChanged.getConnectionState()).isEqualTo(ConnectionState.Disconnected);

        listener.dispose();
    }

    @Test
    public void test_location_changed_is_not_fired_if_not_active()
    {
        final TestLocationProvider provider = new TestLocationProvider();
        final ProviderEventListener listener = new ProviderEventListener();

        provider.fireLocationChanged(NEW_LOCATION);

        assertThat(listener.locationChanged).isNull();
    }

    /**
     * Create a TestLocationProvider, configure it, and set it as the active provider.
     *
     * @return the installed provider
     */
    private TestLocationProvider createAndActivateConfiguredProvider()
    {
        final TestLocationProvider provider = new TestLocationProvider();
        locationProviderManager.registerLocationProvider(provider);
        provider.configure(new TestLocationProvider.TestConfiguration(CONFIG_STRING));

        locationProviderManager.setActiveLocationProvider(PROVIDER_KEY);
        assertThat(provider.isActive()).isTrue();
        assertThat(locationProviderManager.getActiveLocationProvider()).isSameAs(provider);

        return provider;
    }

    /**
     * Simple observer to test LocationProvider events.
     */
    @PseudoBean
    private static final class ProviderEventListener
    {
        ConnectionStateChanged connectionStateChanged = null;
        LocationChanged locationChanged = null;

        @Keep
        private void onConnectionStateChanged(@Observes ConnectionStateChanged connectionStateChanged)
        {
            this.connectionStateChanged = connectionStateChanged;
        }

        @Keep
        private void onLocationChanged(@Observes LocationChanged locationChanged)
        {
            this.locationChanged = locationChanged;
        }

        @RemoveObserver
        public void dispose()
        {
        }
    }
}
