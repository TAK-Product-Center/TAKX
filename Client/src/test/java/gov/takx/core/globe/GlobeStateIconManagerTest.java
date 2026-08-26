package gov.takx.core.globe;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.platform.globe.CardinalPoint;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.awt.image.BufferedImage;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * This unit test requires JOGL.
 */
@TestMethodOrder(DependsOnOrdering.class)
public class GlobeStateIconManagerTest extends AIntegrationTest
{
    private static final URL IMAGE_32X32 = GlobeStateIconManagerTest.class.getClassLoader().getResource("icons/32x32.png");
    private static final String UNIT_ID = "Unit 1";

    private IMapObjectController moc;

    public GlobeStateIconManagerTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    @Test
    public void bringPluginOnline()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID)
                .location(new RaptorLocation(0, 0))
                .build();
        moc = getBean(IMapObjectManager.class).getOrCreateMapObject(spec).getController();
    }

    @Test
    @DependsOn("bringPluginOnline")
    public void createStateIcons()
    {
        IStateIconManager stateIconManager = moc.getStateIconManager();

        // Add and remove a state icon at each location.
        for (CardinalPoint location : CardinalPoint.values())
        {
            stateIconManager.addStateIcon(location, IMAGE_32X32);

            assertThat(stateIconManager.hasStateIconAtPosition(location)).withFailMessage("State icon just added is not being reported when requested by position.").isTrue();
            assertThat(stateIconManager.getStateIcon(location)).withFailMessage("<null> was returned for the location where a state icon was just added.").isNotNull();

            stateIconManager.removeStateIcon(location);

            assertThat(stateIconManager.hasStateIconAtPosition(location)).withFailMessage("A state icon was reported for a position that should be empty.").isFalse();
            assertThat(stateIconManager.getStateIcon(location)).withFailMessage("A state icon was returned for a location that should be empty.").isNull();
        }

        // Test removeAll().
        for (CardinalPoint location : CardinalPoint.values())
        {
            stateIconManager.addStateIcon(location, IMAGE_32X32);
        }

        stateIconManager.removeAllStateIcons();

        for (CardinalPoint location : CardinalPoint.values())
        {
            assertThat(stateIconManager.hasStateIconAtPosition(location)).withFailMessage("A state icon was reported for a position that should be empty.").isFalse();
            assertThat(stateIconManager.getStateIcon(location)).withFailMessage("A state icon was returned for a location that should be empty.").isNull();
        }

        // Provide an invalid null BufferedImage state icon image value.
        assertThatNullPointerException().isThrownBy(() -> stateIconManager.addStateIcon(CardinalPoint.SOUTH, (BufferedImage) null));

        // Provide an invalid null URL state icon image value.
        assertThatNullPointerException().isThrownBy(() -> stateIconManager.addStateIcon(CardinalPoint.SOUTH, (URL) null));
    }

    @Test
    @DependsOn("bringPluginOnline")
    public void whenMoIsHiddenAndReshown_thenStateIconsAreStillVisible()
    {
        final ILocation location = new RaptorLocation(10, 10);
        final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
        final MapObjectSpec spec = MapObjectSpec.builder(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME).location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        IStateIconManager stateIconManager = mapObject.getController().getStateIconManager();
        stateIconManager.addStateIcon(CardinalPoint.NORTH_WEST, IMAGE_32X32);
        assertThat(stateIconManager.isVisible()).isTrue();

        // TODO: the following works if the GlobeMapObjectFeatureManager supports headless in its startService
//        mapObjectManager.setVisible(mapObject.getKey(), false);
//        assertThat(stateIconManager.isVisible()).isFalse();
//
//        mapObjectManager.setVisible(mapObject.getKey(), true);
//        assertThat(stateIconManager.isVisible()).isTrue();
    }
}
