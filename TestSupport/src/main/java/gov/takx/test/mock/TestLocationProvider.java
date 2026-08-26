package gov.takx.test.mock;

import gov.tak.api.location.ALocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.location.ConnectionStateChanged;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to be used to set locations through the {@link gov.tak.core.location.LocationProviderManager}.
 *
 * @since 3.4
 */
@PseudoBean
public class TestLocationProvider extends ALocationProvider<TestLocationProvider.TestConfiguration>
{
    public static final String PROVIDER_KEY = "TestLocationProvider";
    public static final String TEST_EVENT = "Test event";

    private boolean startCalled = false;
    private boolean stopCalled = false;
    private boolean throwExceptionInGetConfiguration = false;
    private boolean throwExceptionInStart = false;

    public TestLocationProvider()
    {
        this(PROVIDER_KEY);
    }

    TestLocationProvider(String key)
    {
        super(key, key, true, new TestConfiguration());
    }

    /**
     * Sets stop and start called both to false, as if neither onStart nor onStop have been called.
     */
    public void clear()
    {
        startCalled = stopCalled = false;
    }

    /**
     * @return A string that can beu sed to restore the current state of this Location provider
     */
    public String getRestoredState()
    {
        return restoredState;
    }

    @Nonnull
    @Override
    public TestConfiguration getConfiguration()
    {
        if (throwExceptionInGetConfiguration) throw new RuntimeException("you got it");

        return super.getConfiguration();
    }

    @Nullable
    @Override
    protected TestConfiguration makeConfigurationFromRestoredState(String restoredState)
    {
        return !restoredState.isEmpty() ? new TestConfiguration(restoredState) : null;
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentLocation()
    {
        return isActive() ? currentConfiguration.location : null;
    }

    @Nullable
    @Override
    public ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration)
    {
        return null;
    }

    @Override
    protected void onStart()
    {
        startCalled = true;
        if (throwExceptionInStart) throw new RuntimeException("I refuse to start");
    }

    @Override
    protected void onStop()
    {
        stopCalled = true;
    }

    /**
     * Fires a {@link gov.takx.api.event.location.ConnectionStateChanged.ConnectionState#Disconnected} event
     */
    public void fireConnectionStateChanged()
    {
        fireConnectionStateChanged(ConnectionStateChanged.ConnectionState.Disconnected, TEST_EVENT);
    }

    /**
     * Fires a {@link gov.takx.api.event.location.LocationChanged} message with the provided location without actually changing
     * the configuration of this location provider.
     *
     * @param newLocation The new location for the event.
     */
    public void fireLocationChanged(GeoPointMetaData newLocation)
    {
        fireLocationChanged(currentConfiguration.location, newLocation);
    }

    /**
     * @return {@code true} if onStart has been called
     */
    public boolean isStartCalled()
    {
        return startCalled;
    }

    /**
     * @return {@code true} if onStop has been called
     */
    public boolean isStopCalled()
    {
        return stopCalled;
    }

    /**
     * @param throwExceptionInGetConfiguration if {@code true} getConfiguration will throw a {@link RuntimeException}
     */
    public void setThrowExceptionInGetConfiguration(boolean throwExceptionInGetConfiguration)
    {
        this.throwExceptionInGetConfiguration = throwExceptionInGetConfiguration;
    }

    /**
     * @param throwExceptionInStart if {@code true} onStart will throw a {@link RuntimeException}
     */
    public void setThrowExceptionInStart(boolean throwExceptionInStart)
    {
        this.throwExceptionInStart = throwExceptionInStart;
    }

    /**
     * Basic configuration used to configure this location provider from a string value.
     */
    public static final class TestConfiguration implements ILocationProviderConfiguration
    {
        private final String config;
        private final GeoPointMetaData location;

        private TestConfiguration()
        {
            this("");
        }

        /**
         * @param config A string of latitude and longitude value locations.  See {@link CoordinateSystem#computeLatLonFromString(String)}
         */
        public TestConfiguration(String config)
        {
            this.config = config;

            if (config.isEmpty())
            {
                location = null;
            } else
            {
                final ILocation configuredLocation = CoordinateSystem.computeLatLonFromString(config);
                assertThat(configuredLocation).isNotNull();

                location = new GeoPointMetaData(new GeoPoint(configuredLocation));
            }
        }

        @Override
        public boolean isEmpty()
        {
            return location == null;
        }

        @Nonnull
        @Override
        public String getRestorableState()
        {
            return config;
        }

        @Nonnull
        @Override
        public String getDisplayableString()
        {
            return config;
        }
    }
}
