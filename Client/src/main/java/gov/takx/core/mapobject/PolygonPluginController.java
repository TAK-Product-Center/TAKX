package gov.takx.core.mapobject;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapePolygonMapObject;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.ShapeType;
import gov.takx.core.plugin.DefaultDevicePluginController;
import gov.takx.core.view.gui.maptools.PolygonHolesConfigurationPanel;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.takx.api.shapes.ShapeProperties.HOLE_ADDED;
import static gov.takx.api.shapes.ShapeProperties.HOLE_REMOVED;
import static gov.takx.api.shapes.ShapeProperties.LOCATIONS;

/**
 * A controller for polygons is required so they can provide a configuration panel to support holes.
 *
 * @since 1.47
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PolygonPluginController extends DefaultDevicePluginController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IShapePolygonMapObject thisPolygonMapObject;
    private PolygonHolesConfigurationPanel polygonHolesPreferencesPanel;
    private HolePropertyChangeListener holePropertyChangeListener;

    private static final List<String> PROPERTIES_TO_WATCH = List.of(HOLE_ADDED, HOLE_REMOVED, LOCATIONS);

    @Inject
    private IMapObjectPersist mapObjectPersist;

    public PolygonPluginController(String unitId)
    {
        super(KeyUtils.TOOL_FAMILY, ShapeType.POLYGON.getShapeType(), unitId);
    }

    @Override
    protected void onDelegateInjected()
    {
        thisPolygonMapObject = (IShapePolygonMapObject) devicePluginDelegate.getMapObject();

        // We need to listen to location changes to determine if the changed polygon is an eligible hole
        holePropertyChangeListener = new HolePropertyChangeListener();

        // Listen to all polygons for location changes which could change their eligibility for use as a hole
        mapObjectPersist.findMapObjectsOfType(IShapePolygonMapObject.class)
                .forEach(polygon -> polygon.addPropertyChangeListener(PROPERTIES_TO_WATCH, holePropertyChangeListener));

        // Restore all the holes in the globe map objects once the globe is loaded
        devicePluginDelegate.getGlobeManager().getPrimaryGlobeDelegate().doWhenReady(() -> thisPolygonMapObject.restoreHoles());
    }

    @RemoveObserver
    @Override
    public void shutdown()
    {
        // Stop listening for eligible holes
        mapObjectPersist.findMapObjectsOfType(IShapePolygonMapObject.class)
                .forEach(polygon -> polygon.removePropertyChangeListener(PROPERTIES_TO_WATCH, holePropertyChangeListener));
    }

    @Override
    public List<IPluginConfigurationPanel> getConfigurationPanels()
    {
        polygonHolesPreferencesPanel = new PolygonHolesConfigurationPanel(this, thisPolygonMapObject);

        // This allows us to clean up our panel reference
        polygonHolesPreferencesPanel.addAncestorListener(new PreferencesAncestorListener());

        // Add any contained polygons as potential holes
        mapObjectPersist.findMapObjectsOfType(IShapePolygonMapObject.class)
                .forEach(this::testAndAddPolygonToHolesPreferencePanel);

        List<IPluginConfigurationPanel> panels = super.getConfigurationPanels();
        panels.add(polygonHolesPreferencesPanel);
        return panels;
    }

    /**
     * Adds the polygon to the holes configuration panel if it is contained with this polygon and doesn't have holes of
     * its own. If the polygon is already marked as a hole the configuration panel will receive a true flag.
     *
     * @param polygonMapObject The map object of the contained polygon
     */
    private void testAndAddPolygonToHolesPreferencePanel(IShapePolygonMapObject polygonMapObject)
    {
        // Don't want to add this polygon as its own hole
        if (polygonMapObject != thisPolygonMapObject && polygonHolesPreferencesPanel != null)
        {
            final IMutablePolygon polygon = polygonMapObject.getAssociatedShape();
            if (thisPolygonMapObject.getHoles().contains(polygon))
            {
                // It's already a hole so just add it to the panel as a selected polygon
                polygonHolesPreferencesPanel.addOrUpdateContainedPolygon(true, polygonMapObject);
            } else
            {
                // Don't want to add a polygon with holes as a hole and make sure that it fits geometric hole requirements
                final List<ILocation> thisPolygonsVertices = thisPolygonMapObject.getVertices();
                if (polygonMapObject.getHoles().isEmpty()
                        && ShapeUtils.isEligibleHole(thisPolygonsVertices, polygonMapObject.getVertices()))
                {
                    polygonHolesPreferencesPanel.addOrUpdateContainedPolygon(false, polygonMapObject);
                }
            }
        }
    }

    /**
     * Check to see if the polygon is an eligible hole. If it isn't we remove it from this polygon if it's a hole and we
     * remove it from the holes panels if it is or isn't a hole
     *
     * @param polygonToTest The polygon to be tested and removed
     * @param deleted       {@code true} if the polygon to test has been deleted
     * @return True if the polygon is no longer eligible as a hole
     */
    private boolean testAndRemoveHole(IShapePolygonMapObject polygonToTest, boolean deleted)
    {
        // Check to see if the polygon is contained or if it has been deleted
        if (deleted || !ShapeUtils.polygonContainsShape(thisPolygonMapObject.getVertices(), polygonToTest.getVertices()))
        {
            thisPolygonMapObject.removeHole(polygonToTest);

            // Always remove the polygon if it's no longer eligible
            if (polygonHolesPreferencesPanel != null)
            {
                polygonHolesPreferencesPanel.removeEligiblePolygon(polygonToTest);
            }
            return true;
        }
        return false;
    }

    private void testAndAddOrRemovePolygon(IShapePolygonMapObject polygonToTest)
    {
        // Remove a polygon if it is no longer a hole.
        if (!testAndRemoveHole(polygonToTest, false))
        // The polygon was not a removed hole so lets see if it's an eligible hole and add it to the panel
        {
            testAndAddPolygonToHolesPreferencePanel(polygonToTest);
        }
    }

    /**
     * Triggered by a form submittal in the view
     *
     * @param polygonMapObject Map Object affected
     * @param hole {@code true} if the polygon has been marked as a hole
     * @since 3.3
     */
    public void updateHoleFromView(IShapePolygonMapObject polygonMapObject, boolean hole)
    {
        final IMutablePolygon actualPolygon = polygonMapObject.getAssociatedShape();
        final boolean currentHole = thisPolygonMapObject.getHoles().contains(actualPolygon);

        if (hole && !currentHole)
        {
            thisPolygonMapObject.addHole(polygonMapObject);
        } else if (!hole && currentHole)
        {
            thisPolygonMapObject.removeHole(polygonMapObject);
        }
    }

    @SuppressWarnings("unused") // event observer
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject instanceof IShapePolygonMapObject polygonMO)
        {
            polygonMO.addPropertyChangeListener(PROPERTIES_TO_WATCH, holePropertyChangeListener);
            testAndAddPolygonToHolesPreferencePanel(polygonMO);
        }
    }

    @SuppressWarnings("unused") // event observer
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject instanceof IShapePolygonMapObject polygonMO)
        {
            polygonMO.removePropertyChangeListener(PROPERTIES_TO_WATCH, holePropertyChangeListener);
            testAndRemoveHole(polygonMO, true);
        }
    }

    /**
     * Used to listen to events on polygons that would make it an eligible hole or make it no longer eligible
     */
    private class HolePropertyChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            switch (evt.getPropertyName())
            {
                case LOCATIONS ->
                {
                    final var sourcePolygon = (IShapePolygonMapObject) evt.getSource();

                    // We need to retest all holes if our polygon changed
                    if (sourcePolygon == thisPolygonMapObject)
                    {
                        mapObjectPersist.findMapObjectsOfType(IShapePolygonMapObject.class)
                                .forEach(PolygonPluginController.this::testAndAddOrRemovePolygon);
                    } else
                    {
                        testAndAddOrRemovePolygon(sourcePolygon);
                    }
                }
                case HOLE_ADDED ->
                {
                    if (polygonHolesPreferencesPanel != null)
                    {
                        final IPolygon addedPolygon = (IPolygon) evt.getNewValue();
                        polygonHolesPreferencesPanel.addOrUpdateContainedPolygon(true, addedPolygon.getShapeMapObject());
                    }
                }
                case HOLE_REMOVED ->
                {
                    if (polygonHolesPreferencesPanel != null)
                    {
                        final IPolygon removedPolygon = (IPolygon) evt.getOldValue();
                        polygonHolesPreferencesPanel.addOrUpdateContainedPolygon(false, removedPolygon.getShapeMapObject());
                    }
                }
                default -> logger.warn("Unexpected property change: {}", evt.getPropertyName());
            }
        }
    }

    private class PreferencesAncestorListener implements AncestorListener
    {
        @Override
        public void ancestorAdded(AncestorEvent event)
        {
        }

        @Override
        public void ancestorRemoved(AncestorEvent event)
        {
            // Cleanup when the panel is hidden
            polygonHolesPreferencesPanel.removeAncestorListener(this);
            polygonHolesPreferencesPanel = null;
        }

        @Override
        public void ancestorMoved(AncestorEvent event)
        {
        }
    }
}
