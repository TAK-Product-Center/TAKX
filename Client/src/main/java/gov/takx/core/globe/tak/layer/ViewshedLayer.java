
package gov.takx.core.globe.tak.layer;

import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.math.MathUtils;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.core.globe.tak.layer.opengl.GLViewshed;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adds viewshed features to {@link HeatMapLayer}.
 *
 * @since 5.6 (from ATAK; 5.7 renamed)
 */
public class ViewshedLayer extends HeatMapLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static
    {
        GLLayerFactory.register(GLViewshed.SPI);
    }

    public static final double DEFAULT_VIEWSHED_RADIUS = 3000;// meters
    public static final double DEFAULT_VIEWSHED_HEIGHT = 2; // meters
    public static final int DEFAULT_VIEWSHED_INTENSITY = 50; // percent

    private static final int MAX_SAMPLE_RATE = 501;
    private static final int MIN_SAMPLE_RATE = 15;
    private static final int MAX_SAMPLE_DENSITY = 5;

    private final List<OnPointOfInterestChangedListener> pointOfInterestChangedListeners = new CopyOnWriteArrayList<>();
    private final List<OnRadiusChangedListener> radiusChangedListeners = new CopyOnWriteArrayList<>();
    private final List<OnOpacityChangedListener> opacityChangedListeners = new CopyOnWriteArrayList<>();
    private final boolean forRoute;

    private ILocation markerLocation;
    private IGeoPoint pointOfInterest;
    private double radius = DEFAULT_VIEWSHED_RADIUS;
    private boolean circle = true;
    private int opacityUnseen = DEFAULT_VIEWSHED_INTENSITY;
    private int opacitySeen = DEFAULT_VIEWSHED_INTENSITY;
    private double viewHeightAboveMarker = DEFAULT_VIEWSHED_HEIGHT;

    public ViewshedLayer(@Nonnull String name, boolean forRoute)
    {
        super(name);

        this.forRoute = forRoute;
        setVisible(false);
    }

    /**
     * @return The viewshed point of interest
     */
    public IGeoPoint getPointOfInterest()
    {
        return pointOfInterest;
    }

    /**
     * Sets the Viewshed's marker location.  This location will be used to calculate the POI by adding in the
     * "height above marker" value, and converting it to an AGL reference. If {@code updating} is true then the
     * viewshed will produce a quicker low-resolution result suitable for viewing while dragging.
     *
     * @param location The location to set
     * @param updating {@code true} if the viewshed is being actively updated/dragged, {@code false} otherwise.
     */
    public void setMarkerLocation(@Nonnull ILocation location, boolean updating)
    {
        if (!Objects.equals(markerLocation, location))
        {
            markerLocation = location;
            setPointOfInterest(new GeoPoint(location), updating);
        }
    }

    /**
     * @return The reference marker's location
     */
    public ILocation getMarkerLocation()
    {
        return markerLocation;
    }

    /**
     * Set the POI of this viewshed by taking a given location, and adding in the "height above marker" value, and
     * converting it to an AGL reference.
     *
     * @param pointOfInterest The viewshed point of interest/location
     * @param updating Pass {@code true} if the location is actively being updated by the user
     */
    public void setPointOfInterest(@Nonnull IGeoPoint pointOfInterest, boolean updating)
    {
        setResolution(updating ? 101 : 501); // lower res while updating

        final var atakGeoPoint = MarshalManager.marshal(pointOfInterest, IGeoPoint.class, com.atakmap.coremap.maps.coords.GeoPoint.class);

        // Get ground elevation at the point's lat/lon
        final double groundElevation = ElevationManager.getElevation(atakGeoPoint, null);

        if (!atakGeoPoint.isValid() || !atakGeoPoint.isAltitudeValid() || !GeoPoint.isAltitudeValid(groundElevation))
        {
            markerLocation = null;
            this.pointOfInterest = null;
            return;
        }

        // Try to determine the point's AGL value
        final double metersAGL = EGM96.getAGL(atakGeoPoint, groundElevation);
        final double pointAltitudeAgl = GeoPoint.isAltitudeValid(metersAGL) ? metersAGL : 0; // mimics ATAK
        final double totalAltitudeAgl = Math.max(DEFAULT_VIEWSHED_HEIGHT, pointAltitudeAgl > 0 ? getViewHeightAboveMarker() + pointAltitudeAgl : getViewHeightAboveMarker());
        this.pointOfInterest = new GeoPoint(pointOfInterest.getLatitude(), pointOfInterest.getLongitude(), totalAltitudeAgl, IGeoPoint.AltitudeReference.AGL);

        if (logger.isDebugEnabled())
        {
            logger.debug("Point: {} Is point AGL: {} Ground: {} Height: {} Result: {}",
                    atakGeoPoint.getAltitude(), atakGeoPoint.getAltitudeReference() == com.atakmap.coremap.maps.coords.GeoPoint.AltitudeReference.AGL,
                    pointAltitudeAgl, getViewHeightAboveMarker(), totalAltitudeAgl);
        }

        pointOfInterestChangedListeners.forEach(listener -> listener.onPointOfInterestChanged(this));
    }

    public double getViewHeightAboveMarker()
    {
        return viewHeightAboveMarker;
    }

    public void setViewHeightAboveMarker(double viewHeightAboveMarker)
    {
        final var heightAbovePath = Math.max(viewHeightAboveMarker, DEFAULT_VIEWSHED_HEIGHT);

        if (heightAbovePath != this.viewHeightAboveMarker)
        {
            this.viewHeightAboveMarker = viewHeightAboveMarker;
            if (pointOfInterest != null) setPointOfInterest(pointOfInterest, false); // forces recalculations with new height
        }
    }

    /**
     * @return {@code true} if the viewshed is set to be rendered as a circle
     */
    public boolean isCircle()
    {
        return circle;
    }

    /**
     * @param circle Pass {@code true} if the viewshed should be rendered as a circle
     */
    public void setCircle(boolean circle)
    {
        this.circle = circle;
    }

    /**
     * @return The viewshed radius
     */
    public double getRadius()
    {
        return radius;
    }

    /**
     * @param radius The viewshed radius; note: if {@code Double.NaN}, no heatmap will be displayed
     */
    public void setRadius(double radius)
    {
        this.radius = radius;
        final var diameter = radius * 2;

        // Make sure that the area isn't being sampled in a density higher than needed
        if (diameter / MAX_SAMPLE_RATE < MAX_SAMPLE_DENSITY)
        {
            var sampleRate = (int) Math.ceil(diameter / MAX_SAMPLE_DENSITY);

            // Make sure the resolution is odd so the center falls on the center vertices
            if (sampleRate % 2 == 0) sampleRate++;
            setSampleResolution(sampleRate, sampleRate);
        } else
        {
            setSampleResolution(MAX_SAMPLE_RATE, MAX_SAMPLE_RATE);
        }

        radiusChangedListeners.forEach(listener -> listener.onRadiusChanged(this));
    }

    /**
     * @param resolution The viewshed sample resolution
     */
    public void setResolution(int resolution)
    {
        final var clampedResolution = MathUtils.clamp(resolution, MIN_SAMPLE_RATE, MAX_SAMPLE_RATE);
        if (getSampleResolutionX() != clampedResolution) setSampleResolution(clampedResolution, clampedResolution);
    }

    /**
     * @param opacity The viewshed opacity, as a percent value, applied to both the seen and unseen opacities
     * @since 5.7
     */
    public void setOpacity(int opacity)
    {
        setOpacityUnseen(opacity);
        setOpacitySeen(opacity);
    }

    /**
     * @return The opacity of the viewshed areas that can be seen from the point of interest
     */
    public int getOpacitySeen()
    {
        return opacitySeen;
    }

    /**
     * @param opacity The opacity of the viewshed areas that can be seen from the point of interest, from 1 to 99
     */
    public void setOpacitySeen(int opacity)
    {
        final var clampedOpacity = MathUtils.clamp(opacity, 1, 99);

        if (clampedOpacity != opacitySeen)
        {
            opacitySeen = clampedOpacity;
            notifyOpacityChangeListeners();
        }
    }

    /**
     * @return The opacity of the viewshed areas that cannot be seen from the point of interest
     */
    public int getOpacityUnseen()
    {
        return opacityUnseen;
    }

    /**
     * @param opacity The opacity of the viewshed areas that cannot be seen from the point of interest, from 1 to 99
     */
    public void setOpacityUnseen(int opacity)
    {
        final var clampedOpacity = MathUtils.clamp(opacity, 1, 99);

        if (clampedOpacity != opacityUnseen)
        {
            opacityUnseen = clampedOpacity;
            notifyOpacityChangeListeners();
        }
    }

    /**
     * @return {@code true} if this viewshed is being used for a route
     * @since 5.7
     */
    public boolean isForRoute()
    {
        return forRoute;
    }

    /**
     * @param listener The point of interest change listener to add
     */
    public void addOnPointOfInterestChangedListener(@Nonnull OnPointOfInterestChangedListener listener)
    {
        pointOfInterestChangedListeners.add(listener);
    }

    /**
     * @param listener @param listener The point of interest change listener to remove
     */
    public void removeOnPointOfInterestChangedListener(@Nonnull OnPointOfInterestChangedListener listener)
    {
        pointOfInterestChangedListeners.remove(listener);
    }

    /**
     * @param listener The range change listener to add
     */
    public void addOnRadiusChangedListener(@Nonnull OnRadiusChangedListener listener)
    {
        radiusChangedListeners.add(listener);
    }

    /**
     * @param listener The range change listener to remove
     */
    public void removeOnRadiusChangedListener(@Nonnull OnRadiusChangedListener listener)
    {
        radiusChangedListeners.remove(listener);
    }

    /**
     * @param listener The opacity change listener to add
     */
    public void addOnOpacityChangedListener(@Nonnull OnOpacityChangedListener listener)
    {
        opacityChangedListeners.add(listener);
    }

    /**
     * @param listener The opacity change listener to remove
     */
    public void removeOnOpacityChangedListener(@Nonnull OnOpacityChangedListener listener)
    {
        opacityChangedListeners.remove(listener);
    }

    private void notifyOpacityChangeListeners()
    {
        opacityChangedListeners.forEach(listener -> listener.onOpacityChanged(this));
    }

    /**
     * Contract for point of interest change listeners
     */
    public interface OnPointOfInterestChangedListener
    {
        void onPointOfInterestChanged(@Nonnull ViewshedLayer layer);
    }

    /**
     * Contract for radius change listeners
     */
    public interface OnRadiusChangedListener
    {
        void onRadiusChanged(@Nonnull ViewshedLayer layer);
    }

    /**
     * Contract for opacity change listeners
     */
    public interface OnOpacityChangedListener
    {
        void onOpacityChanged(@Nonnull ViewshedLayer layer);
    }
}
