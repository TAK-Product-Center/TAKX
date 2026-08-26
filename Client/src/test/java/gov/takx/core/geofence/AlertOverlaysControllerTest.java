package gov.takx.core.geofence;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.maps.PointMapItem;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.GeoFenceAlert;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.geofence.event.GeoFenceAdded;
import gov.takx.api.geofence.event.GeoFenceAlertAdded;
import gov.takx.api.geofence.event.GeoFenceAlertRemoved;
import gov.takx.api.geofence.event.GeoFenceRemoved;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IShape;
import gov.takx.core.explorer.ExplorerManager;
import gov.takx.core.explorer.IExplorer;
import gov.takx.test.AIntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * @since 5.3
 */
@PseudoBean
public class AlertOverlaysControllerTest extends AIntegrationTest
{
    private static final String SHAPE_UID = "SHAPE_UID";
    private static final String SHAPE_NAME = "SHAPE_NAME";
    private static final String MAP_ITEM_NAME = "POINT";

    @Mocked
    private IShape shape;
    @Mocked
    private IShapeMapObject<?> shapeMapObject;
    @Mocked
    private PointMapItem pointMapItem;

    @Inject
    private ExplorerManager explorerManager;

    private IExplorer explorer;

    @Override
    protected void postProjectOpen()
    {
        explorer = explorerManager.getDefaultExplorerDelegate();
    }

    @BeforeEach
    void beforeEach()
    {
        doReturn(SHAPE_UID).when(shapeMapObject).getUid();
        doReturn(SHAPE_NAME).when(shapeMapObject).getName();

        doReturn(SHAPE_NAME).when(shape).getName();
        doReturn(shapeMapObject).when(shape).getShapeMapObject();

        doReturn(MAP_ITEM_NAME).when(pointMapItem).getName();
    }

    @AfterEach
    void afterEch()
    {
        // Remove all nodes under the GeoFence Alerts node
        (getBean(AlertOverlaysController.class).getGeoFencesGroup()).setChildren(List.of());
    }

    @Test
    void geoFenceAdded_createsGeoFenceNode()
    {
        final var geoFence = addGeoFence();

        final var fenceNode = explorer.getExplorable(AlertOverlaysController.makeGeoFenceUniqueKey(geoFence));
        assertThat(fenceNode).isNotNull();
        assertThat(fenceNode.getDisplayName()).isEqualTo(AlertOverlaysController.makeGeoFenceDisplayName(shapeMapObject));
        assertThat(fenceNode.createQuickActions()).hasSize(2);
    }

    @Test
    void geoFenceRemoved_removesGeoFenceNode()
    {
        final var geoFence = addGeoFence();
        fireEventToServiceInstance(AlertOverlaysController.class, new GeoFenceRemoved(geoFence, shapeMapObject));

        assertThat(explorer.getExplorable(AlertOverlaysController.makeGeoFenceUniqueKey(geoFence))).isNull();
    }

    @Test
    void onAlertAdded_createsAlertNode()
    {
        final var alert = addGeoFenceAndAlert();
        final var fenceNode = explorer.getExplorable(AlertOverlaysController.makeGeoFenceUniqueKey(alert.geoFence()));
        assertThat(fenceNode.getChildrenCount()).isEqualTo(1);

        final var alertNode = fenceNode.getChild(0);
        assertThat(alertNode.getUniqueKey()).isEqualTo(AlertOverlaysController.makeAlertUniqueKey(alert));
        assertThat(alertNode.getDisplayName()).contains(MAP_ITEM_NAME);
        assertThat(alertNode.createQuickActions()).hasSize(1);
    }

    @Test
    void onAlertRemoved_removesAlertNode()
    {
        final var alert = addGeoFenceAndAlert();
        fireEventToServiceInstance(AlertOverlaysController.class, new GeoFenceAlertRemoved(alert));

        final var fenceNode = explorer.getExplorable(AlertOverlaysController.makeGeoFenceUniqueKey(alert.geoFence()));
        assertThat(fenceNode.getChildrenCount()).isEqualTo(0);
        assertThat(explorer.getExplorable(AlertOverlaysController.makeAlertUniqueKey(alert))).isNull();
    }

    /**
     * Create a new GeoFence and add it to the controller.
     * @return new GeoFence
     */
    private GeoFence addGeoFence()
    {
        final var geoFence = GeoFence.builder().shapeUid(SHAPE_UID).build();
        doReturn(geoFence).when(shapeMapObject).requireGeoFence();
        fireEventToServiceInstance(AlertOverlaysController.class, new GeoFenceAdded(geoFence, shapeMapObject));
        return geoFence;
    }

    /**
     * Create GeoFence and an associated alert, and add them to the controller.
     * @return new alert
     */
    private GeoFenceAlert addGeoFenceAndAlert()
    {
        final var geoFence = addGeoFence();
        final var alert = new GeoFenceAlert(shape, geoFence, pointMapItem, Trigger.Exit, new CoordinatedTime());
        fireEventToServiceInstance(AlertOverlaysController.class, new GeoFenceAlertAdded(alert));
        return alert;
    }
}
