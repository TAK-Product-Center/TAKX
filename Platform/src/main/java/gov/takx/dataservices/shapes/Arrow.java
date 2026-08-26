package gov.takx.dataservices.shapes;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.elevation.ElevationManager;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IMutableArrow;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpanConversionUtils;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Implementation of an arrow shape.
 * <p>
 * Note: arrow actually could be represented as a regular shape, since it is defined by a starting point and the range
 * and bearing characteristics. However, we represent it on the globe using a
 * {@link gov.nasa.worldwindx.examples.util.DirectedPath} because it already supports rendering the arrow heads, so it's
 * easier to treat it as an irregular shape and use the vertices to define it.
 *
 * @since 3.0
 */
public class Arrow extends AIrregularShape implements IMutableArrow
{
    private static final Monitors MONITORS = new Monitors();

    private double bearing;
    private double range = 1; // If 0 is default, possible for bearing to be lost if set first
    private boolean dynamic;
    private boolean rangeLocked;

    /**
     * Default constructor.
     * @since 3.3
     */
    public Arrow()
    {
    }

    /**
     * Create a new arrow instance with the given name, starting point, range, and bearing.
     *
     * @param name Name for shape
     * @param location Location of starting point
     * @param range The length of the arrow (in meters)
     * @param bearing The arrow angle (in degrees from true north)
     */
    public Arrow(@Nonnull String name, @Nonnull ILocation location, double range, double bearing)
    {
        setName(name);

        this.bearing = bearing;
        this.range = range;

        super.setVertices(List.of(location, getEndPoint(location)));
    }

    @Override
    protected int getMinVertexCount()
    {
        return 2;
    }

    @Override
    public double getBearing()
    {
        return bearing;
    }

    @Override
    public void setBearing(double bearing)
    {
        final double normalizedBearing = normalizeBearing(bearing);

        if (Double.compare(normalizedBearing, this.bearing) != 0)
        {
            propertyChangeSupport.updateAndFire(ShapeProperties.BEARING, this.bearing, normalizedBearing,
                    () -> this.bearing = normalizedBearing);

            updateVerticesForRangeAndBearing();
        }
    }

    /**
     * Normalizes the bearing value to be 0-359.
     *
     * @param bearing The bearing, in degrees from true north
     * @return The bearing as a value from 0-359.
     */
    private double normalizeBearing(double bearing)
    {
        while (bearing < 0) bearing += 360;

        return bearing % 360;
    }

    /**
     * Calculates the correct end point based on the range and bearing values and updates the vertices accordingly.
     */
    private void updateVerticesForRangeAndBearing()
    {
        final List<ILocation> vertices = getVertices();
        if (!vertices.isEmpty())
        {
            setEndpointFromRangeAndBearing(vertices.get(0));
        }
    }

    /**
     * Calculates the correct end point based on provided starting point and the range and bearing values, and updates
     * the vertices accordingly.
     *
     * @param startingPoint The starting point for the range and bearing calculations
     */
    private void setEndpointFromRangeAndBearing(ILocation startingPoint)
    {
        super.setVertices(List.of(startingPoint, getEndPoint(startingPoint)));
    }

    private GeoPoint getEndPoint(ILocation location)
    {
        final IGeoPoint endingPoint = GeoCalculations.pointAtDistance(location, bearing, range);

        double latitude = endingPoint.getLatitude();
        double longitude = endingPoint.getLongitude();
        return new GeoPoint(latitude, longitude, ElevationManager.getElevation(latitude, longitude, null));
    }

    @Override
    public double getRange()
    {
        return range;
    }

    @Override
    public void setRange(double range)
    {
        if (Double.isNaN(range) || range <= 0)
        {
            throw new IllegalArgumentException("Range value must be greater than 0; value provided: " + range);
        }

        if (Double.compare(range, this.range) != 0)
        {
            propertyChangeSupport.updateAndFire(ShapeProperties.RANGE, this.range, range,
                    () -> this.range = range);

            updateVerticesForRangeAndBearing();
        }
    }

    @Override
    public boolean isDynamic()
    {
        return dynamic;
    }

    @Override
    public void setDynamic(boolean dynamic)
    {
        this.dynamic = dynamic;
    }

    @Override
    public void setStartingPoint(@Nonnull ILocation startingPoint)
    {
        final List<ILocation> vertices = getVertices();
        if (vertices.size() > 1)
        {
            // If we already have an end point set, don't move it; update the range/bearing for the new start only
            setVertices(List.of(startingPoint, vertices.get(1)));
        } else
        {
            // If we don't have an end point set, calculate it from the current range and bearing values
            setEndpointFromRangeAndBearing(startingPoint);
        }
    }

    @Override
    public void setVertices(@Nonnull List<? extends ILocation> points) throws IllegalArgumentException
    {
        if (points.size() != 2) throw new IllegalArgumentException("An arrow must have exactly 2 vertices; got " + points.size());

        final ILocation startingPoint = points.get(0);
        final ILocation endingPoint = points.get(1);

        final double newRange = GeoCalculations.distance(startingPoint, endingPoint);
        final double newBearing = GeoCalculations.bearing(startingPoint, endingPoint);

        propertyChangeSupport.updateAndFire(ShapeProperties.BEARING, bearing, newBearing, () -> bearing = newBearing);
        propertyChangeSupport.updateAndFire(ShapeProperties.RANGE, range, newRange, () -> range = newRange);

        super.setVertices(points);
    }

    @Override
    public boolean isRangeLocked()
    {
        return rangeLocked;
    }

    @Override
    public void setRangeLocked(boolean rangeLocked)
    {
        this.rangeLocked = rangeLocked;
    }

    /**
     * @return The display text for the bearing of this arrow according to its current bearing
     * @see UserPreferenceEntry#DISPLAY_BEARING_UNIT
     * @since 5.0
     */
    @Override
    public String getBearingLabelText()
    {
        return MONITORS.bearingUnitsMonitor.getCurrentValue().format(getLocation(), range, bearing);
    }

    @Override
    public String getRangeLabelText()
    {
        return MONITORS.rangeUnitsMonitor.getCurrentValue().format(range, 1);
    }

    /**
     * @param location Location (origin) of the arrow
     * @param range Range to the endpoint, in meters
     * @param bearing Bearing from location to endpoint, degrees true
     * @param northReference Target North Reference
     * @param bearingUnits Target Bearing Units
     * @return a string describing the bearing of the arrow based on the given parameters, e.g.
     * '{@code 1,234 milsT}' and '{@code 2.34 radM}'
     * @since 5.0
     * @deprecated Use {@link BearingUnits#format(IGeoPoint, double, double)} instead
     */
    @SuppressWarnings("unused")
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public static String getBearingLabelText(ILocation location, double range, double bearing, NorthReference northReference, @Nonnull Angle bearingUnits)
    {
        return MONITORS.bearingUnitsMonitor.getCurrentValue().format(location, range, bearing);
    }

    /**
     * Creates a string describing the length of the arrow and the units of measurement.
     *
     * @param range Range to format, in meters
     * @param unitsSystem Target units system, one of {@link Span#ENGLISH}, {@link Span#METRIC}, or {@link Span#NM}
     * @return A string representation of the range, e.g., '{@code 123.4 ft}' and '{@code 1,234 km}' and '{@code 12.3 NM}'
     * @since 5.0
     * @deprecated Use {@link gov.takx.platform.utils.units.RangeUnits#format(double, int)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public static String getRangeLabelText(double range, int unitsSystem)
    {
        return SpanConversionUtils.rangeUnitsFromSpanSystem(unitsSystem).format(range, 1);
    }

    /**
     * Holds preference monitors to prevent repeated calls to {@link gov.takx.api.preferences.ITypedPreferenceValue#getValueAs(Class)}
     * and the subsequent String -> Object conversions.
     *
     * @since 5.5
     */
    private static class Monitors
    {
        private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, u -> {
        });
        private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, u -> {
        });
    }
}
