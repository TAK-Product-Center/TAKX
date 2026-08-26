package gov.tak.platform.location.providers;

import gov.tak.api.cot.CotConstants;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests covering the various built-in location providers.
 *
 * @since 3.0
 */
public class LocationProviderTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private PersistTestSetup testSetup;

    @BeforeAll
    public void setup()
    {
        if (logger.isDebugEnabled()) logger.debug("{} is starting", getClass().getCanonicalName());

        testSetup = new PersistTestSetup();
        testSetup.init();

        startService(IRaptorDataPortal.class);
        startService(IDeviceGateway.class);
        startService(IMapObjectManager.class);
    }

    @AfterAll
    public void teardown()
    {
        stopService(IRaptorDataPortal.class);
        stopService(IDeviceGateway.class);
        stopService(IMapObjectManager.class);

        testSetup.shutdown();
    }

    @Test
    public void testNullLocationProvider()
    {
        final NoLocationProvider provider = getBean(NoLocationProvider.class);

        assertThat(provider.isConfigurable()).isFalse();
        assertThat(provider.isConfigured()).isTrue();

        final ILocationProviderConfiguration configuration = provider.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration.getRestorableState()).isNotNull();
        assertThat(configuration.getDisplayableString()).isNotNull();

        assertThat(provider.getCurrentLocation()).isNull();
    }

    @Test
    public void testFixedLocationProvider()
    {
        final FixedLocationProvider provider = getBean(FixedLocationProvider.class);

        assertThat(provider.isConfigurable()).isTrue();
        assertThat(provider.isConfigured()).isFalse();

        ILocationProviderConfiguration configuration = provider.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration.getRestorableState()).isNotNull();
        assertThat(configuration.getDisplayableString()).isNotNull();

        assertThat(provider.getCurrentLocation()).isNull();

        provider.setRestoredState("22.0, -42.00");
        configuration = provider.getConfiguration();
        assertThat(configuration).isNotNull();

        provider.configure(configuration); // Now configure it
        assertThat(provider.isConfigured()).isTrue();

        GeoPointMetaData currentLocation = provider.getCurrentLocation();
        assertThat(currentLocation).isNotNull();
        assertThat(currentLocation.get().getLatitude()).isEqualTo(22.0);
        assertThat(currentLocation.get().getLongitude()).isEqualTo(-42.0);

        assertThat(currentLocation.getMetaData().getStringAttribute(StandardAttributeNames.HOW)).isEqualTo(CotConstants.HOW_HUMAN_ENTERED);

        assertThat(provider.getCurrentLocation()).isEqualTo(currentLocation); // Shouldn't change
    }

    @Test
    public void testMapObjectTrackingLocationProvider()
    {
        final MapObjectTrackingLocationProvider provider = getBean(MapObjectTrackingLocationProvider.class);

        assertThat(provider.isConfigurable()).isTrue();
        assertThat(provider.isConfigured()).isFalse();

        ILocationProviderConfiguration configuration = provider.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration.getRestorableState()).isNotNull();
        assertThat(configuration.getDisplayableString()).isNotNull();

        assertThat(provider.getCurrentLocation()).isNull();

        // Construct a MO to track
        final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);

        RaptorLocation location = new RaptorLocation(10, 20);
        final MapObjectSpec spec = MapObjectSpec.builder("foo", "bar", "0").location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        provider.setRestoredState(mapObject.getKey());
        configuration = provider.getConfiguration();
        assertThat(configuration).isNotNull();

        provider.configure(configuration); // Now configure it
        assertThat(provider.isConfigured()).isTrue();

        GeoPointMetaData currentLocation = provider.getCurrentLocation();
        assertThat(currentLocation).isNotNull();
        assertThat(currentLocation.get().getLatitude()).isEqualTo(mapObject.getLocation().getLatitude());
        assertThat(currentLocation.get().getLongitude()).isEqualTo(mapObject.getLocation().getLongitude());

        assertThat(currentLocation.getMetaData().getStringAttribute(StandardAttributeNames.HOW)).isEqualTo(CotConstants.HOW_MACHINE_GENERATED_PREFIX);

        location = new RaptorLocation(50, 60);
        mapObject.setLocation(location, true);
        currentLocation = provider.getCurrentLocation();
        assertThat(currentLocation).isNotNull();
        assertThat(currentLocation.get().getLatitude()).isEqualTo(mapObject.getLocation().getLatitude());
        assertThat(currentLocation.get().getLongitude()).isEqualTo(mapObject.getLocation().getLongitude());
    }
}
