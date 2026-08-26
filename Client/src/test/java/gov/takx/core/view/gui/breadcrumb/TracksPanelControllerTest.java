package gov.takx.core.view.gui.breadcrumb;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link TracksPanelController}.
 * 
 * @since 5.3
 */
public class TracksPanelControllerTest extends AIntegrationTest
{
    private static final String markerUid1 = "markerUid1";
    private static final String markerUid2 = "markerUid2";

    @Mocked(allowCaching = false)
    private IMapObjectFinder mapObjectFinder;
    @Mocked(allowCaching = false)
    private IMarkerMapObject markerMapObject1;
    @Mocked(allowCaching = false)
    private IMarkerMapObject markerMapObject2;

    private TracksPanelController controller;

    @BeforeEach
    public void beforeEach()
    {
        initMarker(markerMapObject1, markerUid1);
        initMarker(markerMapObject2, markerUid2);

        // headless, but we initialize properly for test
        controller = new TracksPanelController(mapObjectFinder, false);
        controller.startService(null);
    }

    @Test
    public void mapObjectEvents_updateModel()
    {
        // headless, but we initialize properly for test
        controller.mapObjectAdded(new MapObjectAdded(markerMapObject1));
        controller.mapObjectAdded(new MapObjectAdded(markerMapObject2));
        SwingTestUtils.waitForEdtEvents();
        controller.modelUpdater.processNow(true);
        SwingTestUtils.waitForEdtEvents();

        assertThat(controller.getModel().size()).isEqualTo(2);

        controller.deleteMarker(markerMapObject1);
        controller.deleteMarker(markerMapObject2);
        SwingTestUtils.waitForEdtEvents();
        controller.modelUpdater.processNow(true);
        SwingTestUtils.waitForEdtEvents();

        assertThat(controller.getModel().size()).isEqualTo(0);
    }

    @Test
    public void projectClosedEvent_clearsModel()
    {
        // headless, but we initialize properly for test
        controller.mapObjectAdded(new MapObjectAdded(markerMapObject1));
        SwingTestUtils.waitForEdtEvents();
        controller.modelUpdater.processNow(true);
        SwingTestUtils.waitForEdtEvents();

        assertThat(controller.getModel().size()).isEqualTo(1);

        controller.stopService(null);

        assertThat(controller.getModel().size()).isEqualTo(0);
    }

    @Test
    public void constructor_restoresMarkers(@Mocked IMapObjectFinder tempMapObjectFinder)
    {
        // Use temp finder and controller to test initialization without affecting other tests
        when(tempMapObjectFinder.findAllMapObjects()).thenReturn(List.of(markerMapObject1, markerMapObject2));

        // headless, but we initialize properly for test
        final var tempController = new TracksPanelController(tempMapObjectFinder, false);
        SwingTestUtils.waitForEdtEvents();
        tempController.modelUpdater.processNow(true);
        SwingTestUtils.waitForEdtEvents();

        assertThat(tempController.getModel().size()).isEqualTo(2);
    }

    private void initMarker(IMarkerMapObject markerMapObject, String uid)
    {
        when(markerMapObject.getUid()).thenReturn(uid);
        when(markerMapObject.getName()).thenReturn("name " + uid);

        when(mapObjectFinder.findMapObjectByUid(uid)).thenReturn(markerMapObject);
    }
}
