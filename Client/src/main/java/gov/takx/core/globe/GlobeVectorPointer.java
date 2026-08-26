package gov.takx.core.globe;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Polyline;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeVectorPointer extends Polyline implements PropertyChangeListener
{
    @Inject
    private IRaptorDataPortal dataPortal;

    @Inject
    private ISmartTrackManager smartTrackManager;

    @Inject
    private IGlobeManager globeManager;

    /**
     * Used to calculate the length of the pointer.
     * <p/>
     * 1/2 of the Earth's circumference is divided into the number of units indicated by this constant.
     * Each unit of magnitude will extend the pointer by a length equivalent to one of these divisions.
     */
    private static final int UNITS_TO_DIVIDE_HALF_EARTH_CIRCUMFERENCE_INTO = 10000000;

    /**
     * Minimum length for the pointer as long as it has a vector.
     */
    private static final double MIN_POINTER_LENGTH = 10 * (Math.PI / UNITS_TO_DIVIDE_HALF_EARTH_CIRCUMFERENCE_INTO);

    /**
     * Used by renderer to determine if this Renderable should be displayed.
     */
    private boolean isVisible;

    /**
     * The corresponding map object's last location. Location and
     * vector information arrive separately, so it must be persisted.
     */
    private ILocation lastLocation;

    /**
     * The corresponding map object's last magnitude. Location and
     * vector information arrive separately, so it must be persisted.
     */
    private double lastMagnitude;

    /**
     * The corresponding map object's last heading. Location and
     * vector information arrive separately, so it must be persisted.
     */
    private double lastHeading;

    /**
     * The globe canvas. Used for resizing of the vector pointers as the zoom level changes.
     */
    private WorldWindow canvas;

    /**
     * Multiplier to lengthen the length of the pointer relative to the elevation of the current view.
     */
    private double elevationAdjustment;

    /**
     * {@link IMapObject} this pinter is associated with.
     */
    private final IMapObject mapObject;

    private final IRaptorDataListener dataListener = new DataListener();

    /**
     * All available vector pointers.
     */
    private static final Map<IMapObject, GlobeVectorPointer> allVectorPointers = new HashMap<>();

    /**
     * Constructor.
     *
     * @param mapObject Parent map object.
     * @param color     Color
     */
    public GlobeVectorPointer(IMapObject mapObject, Color color)
    {
        this.mapObject = mapObject;
        prepareVectorPointerManager(mapObject, color);
    }

    /**
     * Set the current magnitude and heading.
     *
     * @param magnitude Parent map object's current magnitude.
     * @param heading   Parent map object's current heading.
     */
    public void setMagnitudeAndHeading(double magnitude, double heading)
    {
        lastMagnitude = magnitude;
        lastHeading = heading;

        draw(lastLocation, lastHeading, lastMagnitude);
    }

    /**
     * Indicates if this pointer should be visible.
     *
     * @return Boolean that indicates if this pointer should be visible.
     */
    public boolean isVisible()
    {
        return isVisible;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // Update the location of the primary annotation (manager) to match
        // the map object.
        if (evt.getPropertyName().equals(IMapObject.LOCATION))
        {
            lastLocation = (ILocation) evt.getNewValue();
            draw(lastLocation, lastHeading, lastMagnitude);
        }
        // Keep the same visibility as the map object.
        else if (evt.getPropertyName().equals(IMapObject.VISIBILITY))
        {
            setVisible((Boolean) evt.getNewValue());
        }
        // Snap to surface if the tracks do.
        else if (evt.getPropertyName().equals(ITrackStyleDefinition.SNAP_TO_SURFACE))
        {
            setFollowTerrain((Boolean) evt.getNewValue());
        } else if (evt.getPropertyName().equals(AVKey.VIEW))
        {
            // Eye position can be NULL during a screenshot when we toggle to/from flat view
            Position eyePosition = canvas.getView().getEyePosition();
            if (eyePosition != null)
            {
                setElevationAdjustment(eyePosition.getElevation());
                draw(lastLocation, lastHeading, lastMagnitude);
            }
        }
    }

    /**
     * Set this pointer's visibility.
     *
     * @param visible {@code true} if the pointer should be visible, {@code false} otherwise.
     */
    private void setVisible(boolean visible)
    {
        isVisible = visible;
    }

    /**
     * Set up this object:
     * <p/>
     * Create references to the map object and the globe icon map object.
     * Add listeners to the map object.
     * Set the attributes on the annotation associated with the GlobeStateIconManager.
     *
     * @param mapObject The {@link IMapObject} associated with this manager.
     * @param color     The color that the pointer should be.
     */
    private void prepareVectorPointerManager(IMapObject mapObject, Color color)
    {
        dataPortal.addListener(dataListener);

        checkZoomListener();

        allVectorPointers.put(mapObject, this);

        lastLocation = mapObject.getLocation();
        isVisible = true;

        mapObject.addPropertyChangeListener(IMapObject.LOCATION, this);
        mapObject.addPropertyChangeListener(IMapObject.VISIBILITY, this);

        // Watch for changes in the snap to surface property
        final ITrackStylingContext tsc = smartTrackManager.getTrackStylingContext(mapObject);
        tsc.addPropertyChangeListener(this, ITrackStyleDefinition.SNAP_TO_SURFACE);

        if (color != null)
        {
            setColor(color);
        }
        setLineWidth(7);
        setFollowTerrain(true);

        draw(mapObject.getLocation(), 0, 0);
    }

    private void checkZoomListener()
    {
        // If we haven't acquired an instance of the globe canvas yet...
        if (canvas == null)
        {
            // Try to get an instance.
            canvas = globeManager.getPrimaryGlobeDelegate().getWorldWindow();

            // If successful, add a listener.
            if (canvas != null)
            {
                canvas.getSceneController().addPropertyChangeListener(AVKey.VIEW, this);
                setElevationAdjustment(canvas.getView().getEyePosition().getElevation());
            }
        }
    }

    /**
     * Set this pointer's location, direction and length based on the parameters provided.
     *
     * @param location  The current start location.
     * @param heading   The diretion that the pointer should proceed from the start position.
     * @param magnitude The number of units, relative to {@link this.UNITS_TO_DIVIDE_HALF_EARTH_CIRCUMFERENCE_INTO},
     *                  that the pointer should extend.
     */
    private void draw(ILocation location, double heading, double magnitude)
    {
        checkZoomListener();

        if (location != null)
        {
            double pointerLength = magnitude * (Math.PI / UNITS_TO_DIVIDE_HALF_EARTH_CIRCUMFERENCE_INTO) * elevationAdjustment;

            if (magnitude > 0)
            {
                pointerLength += MIN_POINTER_LENGTH;
            }

            // Add point 1
            final java.util.List<LatLon> points = new ArrayList<>();
            points.add(LatLon.fromDegrees(location.getLatitude(), location.getLongitude()));

            // Calculate point 2
            Angle beaconHeading = Angle.fromDegrees(heading);
            LatLon endpoint = LatLon.greatCircleEndPosition(LatLon.fromDegrees(location.getLatitude(),
                    location.getLongitude()),
                    beaconHeading.radians,
                    pointerLength);

            // Add point 2
            points.add(endpoint);

            // Calculate point 3
            Angle arrowHeading = Angle.fromDegrees(heading > 180 ? (heading - 180) - 20 : heading + 180 - 20);
            LatLon endpoint2 = LatLon.greatCircleEndPosition(endpoint,
                    arrowHeading.radians,
                    pointerLength / 4);

            // Add point 3
            points.add(endpoint2);

            setPositions(points, location.getElevation());
        }
    }

    /**
     * Multiplier to adjust the length of the pointer by to account for elevation.
     *
     * @param elevation The elevation used to calculate the multiplier.
     */
    private void setElevationAdjustment(double elevation)
    {
        elevationAdjustment = elevation / 1000.0;
    }

    /**
     * Clear the static list of pointers on project close.
     */
    private void projectClosing(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        // Remove all of our listener registrations
        for (IMapObject mapObject : allVectorPointers.keySet())
        {
            mapObject.removePropertyChangeListener(IMapObject.LOCATION, this);
            mapObject.removePropertyChangeListener(IMapObject.VISIBILITY, this);

            final ITrackStylingContext tsc = smartTrackManager.getTrackStylingContext(mapObject);
            tsc.removePropertyChangeListener(this, ITrackStyleDefinition.SNAP_TO_SURFACE);
        }
        allVectorPointers.clear();
    }

    /**
     * Data listener to know when our map object goes off line.
     */
    private class DataListener extends DefaultRaptorDataListener
    {
        @Override
        public boolean matchMapObjectForData(IMapObject mapObject)
        {
            return false; // Don't want data, just online/offline
        }

        @Override
        public boolean matchMapObject(IMapObject mapObject)
        {
            return mapObject.getFamily().equals(GlobeVectorPointer.this.mapObject.getFamily()) && mapObject.getUnitID().equals(GlobeVectorPointer.this.mapObject.getUnitID());
        }

        @Override
        public void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipeHistoricalData)
        {
            allVectorPointers.remove(mapObject);
        }
    }
}
