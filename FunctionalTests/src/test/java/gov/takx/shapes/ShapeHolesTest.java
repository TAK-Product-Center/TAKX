package gov.takx.shapes;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapePolygonMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IShapeService.ShapeOptions;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.maptools.PolygonHolesConfigurationPanel;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.swing.table.DefaultTableModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests shapes with holes. Currently we only support polygons within polygons.
 *
 * @since 1.48
 */
@TestMethodOrder(DependsOnOrdering.class)
public class ShapeHolesTest extends AIntegrationTest
{
    private IShapePolygonMapObject holeContainingPolygonMO;
    private IPluginConfigurationPanel holesPanel;
    private DefaultTableModel eligibleHolesTableModel;

    private IMapObjectController holeContainingPolygon2Controller;
    private IPluginConfigurationPanel holesPanel2;
    private DefaultTableModel eligibleHolesTableModel2;
    private IShapePolygonMapObject holeContainingPolygon2MO;

    private IShapePolygonMapObject potentialHole1MO;

    private IMapObjectController potentialHole2Controller;
    private IShapePolygonMapObject potentialHole2MO;

    private final List<ILocation> validHoleLocations;
    private final List<ILocation> adjacentValidHoleLocations;
    private final List<ILocation> invalidHoleLocations;
    private final List<ILocation> holeContainingShapeLocations;
    private final List<ILocation> holeContainingShapeLocations2;
    private final List<ILocation> invalidHoleContainingShapeLocations;

    // Listen for polygon events that affect holes
    private IMutablePolygon holeContainingPolygon2Shape;
    private IMapObjectManager mapObjectManager;

    public ShapeHolesTest()
    {
        validHoleLocations = new ArrayList<>();
        validHoleLocations.add(new RaptorLocation(1, 1));
        validHoleLocations.add(new RaptorLocation(1, 5));
        validHoleLocations.add(new RaptorLocation(5, 5));
        validHoleLocations.add(new RaptorLocation(5, 1));

        adjacentValidHoleLocations = new ArrayList<>();
        adjacentValidHoleLocations.add(new RaptorLocation(6, 6));
        adjacentValidHoleLocations.add(new RaptorLocation(9, 6));
        adjacentValidHoleLocations.add(new RaptorLocation(9, 1));
        adjacentValidHoleLocations.add(new RaptorLocation(6, 1));

        invalidHoleLocations = new ArrayList<>();
        invalidHoleLocations.add(new RaptorLocation(1, 1));
        invalidHoleLocations.add(new RaptorLocation(1, 11)); // Invalid point outside of the containing polygon
        invalidHoleLocations.add(new RaptorLocation(9, 9));
        invalidHoleLocations.add(new RaptorLocation(9, 1));

        holeContainingShapeLocations = new ArrayList<>();
        holeContainingShapeLocations.add(new RaptorLocation(0, 0));
        holeContainingShapeLocations.add(new RaptorLocation(0, 10));
        holeContainingShapeLocations.add(new RaptorLocation(10, 10));
        holeContainingShapeLocations.add(new RaptorLocation(10, 0));

        holeContainingShapeLocations2 = new ArrayList<>();
        holeContainingShapeLocations2.add(new RaptorLocation(0, 0));
        holeContainingShapeLocations2.add(new RaptorLocation(0, 11));
        holeContainingShapeLocations2.add(new RaptorLocation(11, 11));
        holeContainingShapeLocations2.add(new RaptorLocation(11, 0));

        invalidHoleContainingShapeLocations = new ArrayList<>();
        invalidHoleContainingShapeLocations.add(new RaptorLocation(0, 0));
        invalidHoleContainingShapeLocations.add(new RaptorLocation(0, 10));
        invalidHoleContainingShapeLocations.add(new RaptorLocation(4, 4));
        invalidHoleContainingShapeLocations.add(new RaptorLocation(10, 0));
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
    }

    @AfterEach
    public void cleanup()
    {
        deleteAllMapObjects();
    }

    @Test
    public void init() throws ShapeAlreadyExistsException
    {
        final SwingUIBinder swingUIBinder = getBean(SwingUIBinder.class);

        IMapObjectController holeContainingPolygonController = createPolygon();
        holeContainingPolygonMO = (IShapePolygonMapObject) holeContainingPolygonController.getMapObject();
        holeContainingPolygonMO.setVertices(holeContainingShapeLocations);
        // Open panel
        final IDeviceSwingUIProvider uiProvider = swingUIBinder.forDevice(holeContainingPolygonController);
        holesPanel = uiProvider.getConfigurationPanels().get(0);

        // Create the second containing polygon using the shape service
        holeContainingPolygon2Shape = holeContainingPolygonController.getShapeService().createPolygonMapObject(
                new Polygon("Shape Service Polygon", holeContainingShapeLocations2), ShapeOptions.PUBLISHED);
        holeContainingPolygon2Controller = holeContainingPolygon2Shape.getShapeMapObject().getController();
        holeContainingPolygon2Shape.setVertices(holeContainingShapeLocations2);
        holeContainingPolygon2MO = (IShapePolygonMapObject) holeContainingPolygon2Controller.getMapObject();
        // Open panel
        final IDeviceSwingUIProvider uiProvider2 = swingUIBinder.forDevice(holeContainingPolygon2Controller);
        holesPanel2 = uiProvider2.getConfigurationPanels().get(0);

        IMapObjectController potentialHole1Controller = createPolygon();
        potentialHole1MO = (IShapePolygonMapObject) potentialHole1Controller.getMapObject();

        potentialHole2Controller = createPolygon();
        potentialHole2MO = (IShapePolygonMapObject) potentialHole2Controller.getMapObject();
    }

    @Test
    @DependsOn("init")
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void holePanelTest()
    {
        // Add an invalid polygon
        potentialHole1MO.setVertices(invalidHoleLocations);
        // Check the panel for the invalid polygon. It should not be there
        eligibleHolesTableModel = CdiTestUtils.getField(holesPanel, "polygonsTableModel");
        eligibleHolesTableModel2 = CdiTestUtils.getField(holesPanel2, "polygonsTableModel");
        assertThat(eligibleHolesTableModel.getRowCount()).isEqualTo(0);
        assertThat(eligibleHolesTableModel2.getRowCount()).isEqualTo(0);

        // Change the invalid polygon to valid polygon
        potentialHole1MO.setVertices(validHoleLocations);
        // Check the panel for the valid polygon
        assertThat(eligibleHolesTableModel.getRowCount()).isEqualTo(1);
        assertThat(eligibleHolesTableModel2.getRowCount()).isEqualTo(1);

        // Add a new valid polygon
        potentialHole2MO.setVertices(adjacentValidHoleLocations);
        // Check the panel for new valid polygon
        assertThat(eligibleHolesTableModel.getRowCount()).isEqualTo(2);
        assertThat(eligibleHolesTableModel2.getRowCount()).isEqualTo(2);

        // Change the valid polygon to an invalid polygon
        potentialHole2MO.setVertices(invalidHoleLocations);
        // Check panel to make sure the invalid polygon was removed
        assertThat(eligibleHolesTableModel.getRowCount()).isEqualTo(1);
        assertThat(eligibleHolesTableModel2.getRowCount()).isEqualTo(1);

        // Make the second hole eligible
        potentialHole2MO.setVertices(adjacentValidHoleLocations);
        // Make sure the second eligible hole is in the table
        assertThat(eligibleHolesTableModel.getRowCount()).isEqualTo(2);
        // Delete the second eligible hole
        getBean(IMapObjectManager.class).deleteMapObject(potentialHole2MO);
        // Make sure the second eligible hole is no longer in the table because it was deleted
        assertThat(eligibleHolesTableModel.getRowCount()).isEqualTo(1);

        // Re-add the deleted hole for later tests
        potentialHole2Controller = createPolygon();
        potentialHole2MO = (IShapePolygonMapObject) potentialHole2Controller.getMapObject();

        // Select the one valid hole in the table
        eligibleHolesTableModel.setValueAt(true, 0, 0);

        // Apply the hole
        holesPanel.applyAction();
    }

    @Test
    @DependsOn("holePanelTest")
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void globeShapeTest()
    {
        // There should now be one valid hole in the polygon
        assertThat(holeContainingPolygonMO.getHoles()).contains(potentialHole1MO.getAssociatedShape());
        assertThat(holeContainingPolygonMO.getHoles()).doesNotContain(potentialHole2MO.getAssociatedShape());

        // Make sure that the contained hole map object is not visible
        assertThat(potentialHole1MO.isVisible()).isFalse();

        // Make sure that the GlobeShapeMapObject Polygon has the proper hole
        final IApplicationGlobe globe = getBean(IExtendedGlobeManager.class).getPrimaryGlobeDelegate();
        final IPolygon wwPolygonContainingHoles = ((IGlobeShapeMapObject) globe.getGlobeMapObject(holeContainingPolygonMO.getKey())).requireShapeModel();
        Set<IPolygon> containingShapeBoundaries = wwPolygonContainingHoles.getHoles();
        final IPolygon hole1WWPolygon = ((IGlobeShapeMapObject) globe.getGlobeMapObject(potentialHole1MO.getKey())).requireShapeModel();
        List<ILocation> holeBoundary1 = hole1WWPolygon.getVertices();
        List<ILocation> holeBoundary = new ArrayList<>(containingShapeBoundaries).get(1).getVertices();
        assertThat(holeBoundary).isEqualTo(holeBoundary1);

        List<ILocation> holeBoundary2 = ((IPolygon) ((IGlobeShapeMapObject) globe.getGlobeMapObject(potentialHole2MO.getKey())).getShapeModel()).getVertices();
        // Make sure that the GlobeShapeMapObject Polygon does not have the invalid polygon
        assertThat(holeBoundary).isNotEqualTo(holeBoundary2);

        // Make one of the holes vertices outside of the polygon making it invalid
        potentialHole1MO.setVertices(invalidHoleLocations);
        // Make sure that the GlobeShapeMapObject no longer has the removed hole
        assertThat(containingShapeBoundaries).hasSize(1);

        // Make the hold valid again
        potentialHole1MO.setVertices(validHoleLocations);
        eligibleHolesTableModel.setValueAt(true, 0, PolygonHolesConfigurationPanel.IS_HOLE_COLUMN);
        holesPanel.applyAction();

        // Make sure that the GSMO has the newly valid polygon
        assertThat(holeContainingPolygonMO.getHoles()).contains(potentialHole1MO.getAssociatedShape());
        holeBoundary1 = hole1WWPolygon.getVertices();
        holeBoundary = new ArrayList<>(wwPolygonContainingHoles.getHoles()).get(1).getVertices();
        assertThat(holeBoundary).isEqualTo(holeBoundary1);

        // Make the hole invalid
        holeContainingPolygonMO.setVertices(invalidHoleContainingShapeLocations);
        containingShapeBoundaries = wwPolygonContainingHoles.getHoles();
        // Make sure that the changed containing polygon no longer has holes since they will be invalid
        assertThat(containingShapeBoundaries).hasSize(1);
    }

    @Test
    @DependsOn("globeShapeTest")
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void shapeAPITest() throws ShapeAlreadyExistsException
    {
        // Create a valid hole using the shape service
        IMutablePolygon hole = holeContainingPolygon2Controller.getShapeService().createPolygonMapObject(
                new Polygon("Hole polygon", validHoleLocations), ShapeOptions.PUBLISHED);
        final IShapeMapObject<?> holeMapObject = hole.getShapeMapObject();

        // Can't add a hole to itself
        assertThatIllegalArgumentException().isThrownBy(() -> hole.addHole(hole))
                .withMessageContaining("Can not add a hole to itself: ");

        // Make sure the hole is in the containing polygon's table
        assertThat(eligibleHolesTableModel2.getValueAt(1, 1)).isEqualTo(holeMapObject);

        // Make sure that the hole is not checked since it's not a hole yet but it is eligible
        assertThat((Boolean) eligibleHolesTableModel2.getValueAt(1, 0)).isFalse();

        // Add the hole to the containing polygon
        holeContainingPolygon2Shape.addHole(hole);

        // Make sure that the isHole checkbox for the hole is checked
        assertThat((Boolean) eligibleHolesTableModel2.getValueAt(1, 0)).isTrue();

        // Make sure that the polygon contains the hole
        assertThat(holeContainingPolygon2MO.getHoles()).contains(hole);

        // Can't add a hole to the same polygon twice
        assertThatIllegalArgumentException().isThrownBy(() -> holeContainingPolygon2Shape.addHole(hole))
                .withMessageContaining("is already a hole in");

        // Can't add a polygon with holes to another polygon
        IMutablePolygon hole2 = holeContainingPolygon2Controller.getShapeService().createPolygonMapObject(
                new Polygon("Hole polygon2", invalidHoleLocations), ShapeOptions.PUBLISHED);
        assertThatIllegalArgumentException().isThrownBy(() -> hole2.addHole(holeContainingPolygon2Shape))
                .withMessageContaining(" already has holes. It can not be added as a hole to: ");

        // A hole must have all of its vertices within a containing polygon and the containing vertices must not be within the hole
        assertThatIllegalArgumentException().isThrownBy(() -> holeContainingPolygon2Shape.addHole(hole2))
                .withMessageContaining("vertices are not within");

        // Remove the hole
        holeContainingPolygon2Shape.removeHole(hole);

        // Make sure that the isHole checkbox is no longer selected
        assertThat((Boolean) eligibleHolesTableModel2.getValueAt(1, 0)).isFalse();
    }

    @Test
    @DependsOn("shapeAPITest")
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void deleteTest()
    {
        // Make sure that we can delete without any exceptions
        final ICoreMapObjectManager mom = getBean(ICoreMapObjectManager.class);
        mom.deleteMapObject(holeContainingPolygonMO);
        mom.deleteMapObject(potentialHole1MO);
    }

    private IMapObjectController createPolygon()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, ShapeType.POLYGON.getShapeType())
                .location(new RaptorLocation(0, 0))
                .build();
        return mapObjectManager.getOrCreateMapObject(spec).getController();
    }
}
