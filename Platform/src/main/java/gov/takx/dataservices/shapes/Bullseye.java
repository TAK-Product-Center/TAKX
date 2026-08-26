package gov.takx.dataservices.shapes;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IBullseye;
import gov.takx.api.shapes.IMutableBullseye;
import gov.takx.api.shapes.ShapeProperties;
import jakarta.annotation.Nonnull;

/**
 * Implementation of a Bullseye shape.
 * @since 5.5
 */
public class Bullseye extends ARingedShape implements IMutableBullseye, ExtentHolder
{
    private NorthReference northReference = NorthReference.TRUE;
    private Angle bearingUnits = Angle.DEGREE;
    private Span spanUnits = Span.FOOT;
    private boolean showRangeRings;
    private boolean pointingEdgeToCenter;

    /** Stored in {@link Span#METER} */
    private double radius;
    /** Stored in {@link Span#METER} */
    private double ringRadius;

    /**
     * Default constructor. Sets specific defaults:
     * <ul>
     *     <li>Ring Radius: 100 {@link Span#METER}</li>
     *     <li>Ring Count: 1</li>
     * </ul>
     */
    public Bullseye()
    {
        // default settings
        setRingRadius(100, Span.METER);
        setRingCount(1);
    }

    /**
     * Create a new bullseye instance with the given name, center point and radius of provided units.
     *
     * @param name     Shape name
     * @param location {@link ILocation} of center point
     * @param radius   radius of the bullseye
     * @param units    {@link Span} units of the radius
     */
    public Bullseye(@Nonnull String name, @Nonnull ILocation location, double radius, @Nonnull Span units)
    {
        this();
        setName(name);
        setLocation(location);
        setRadius(radius, units);
    }

    @Override
    public void setRadius(double radius, @Nonnull Span units) throws IllegalArgumentException
    {
        final double newRadius = SpanUtilities.convert(radius, units, Span.METER);
        if (!IBullseye.isValidRadius(newRadius, Span.METER)) throw new IllegalArgumentException("Bullseye radius must be between " + MIN_RADIUS + "m and " + MAX_RADIUS + "m; value provided: " + newRadius + "m");
        propertyChangeSupport.updateAndFire(ShapeProperties.RADIUS, this.radius, newRadius, () -> this.radius = newRadius);
    }

    @Override
    public double getRadius()
    {
        return radius;
    }

    @Override
    public void setRingCount(int ringCount) throws IllegalArgumentException
    {
        if (ringCount >= MIN_RING_COUNT && ringCount <= MAX_RING_COUNT && !IBullseye.isValidRingRadius(ringRadius, Span.METER, ringCount))
        {
            throw new IllegalArgumentException("Reduce ring count or ring radius (ring count * ring radius <= "
                    + MAX_RING_RADIUS + "m; value provided: " + getRingRadius() + "m * " + ringCount + "rings = "
                    + (getRingRadius() * ringCount) + "m");
        }
        super.setRingCount(ringCount);
    }

    @Override
    public void setRingRadius(double ringRadius, @Nonnull Span units) throws IllegalArgumentException
    {
        final double newRingRadius = SpanUtilities.convert(ringRadius, units, Span.METER);
        if (!IBullseye.isValidRingRadius(newRingRadius, Span.METER, getRingCount()))
        {
            String msg = "Radius is " + (newRingRadius < MIN_RING_RADIUS
                    ? "too small, must be >= " + MIN_RING_RADIUS + "m; value provided: " + newRingRadius + "m"
                    : "too big, ring count * ring radius <= " + MAX_RING_RADIUS + "m; value provided: " + newRingRadius + "m * " + getRingCount()
                            + "rings = " + (newRingRadius * getRingCount()) + "m");
            throw new IllegalArgumentException(msg);
        }
        propertyChangeSupport.updateAndFire(ShapeProperties.INTERVAL, this.ringRadius, newRingRadius, () -> this.ringRadius = newRingRadius);
    }

    @Override
    public double getRingRadius()
    {
        return ringRadius;
    }

    @Override
    public void setPointingEdgeToCenter(boolean pointingEdgeToCenter)
    {
        propertyChangeSupport.updateAndFire(DIRECTION, this.pointingEdgeToCenter, pointingEdgeToCenter, () -> this.pointingEdgeToCenter = pointingEdgeToCenter);
    }

    @Override
    public boolean isPointingEdgeToCenter()
    {
        return pointingEdgeToCenter;
    }

    @Override
    public void setNorthReference(@Nonnull NorthReference northReference)
    {
        propertyChangeSupport.updateAndFire(NORTH_REFERENCE, this.northReference, northReference, () -> this.northReference = northReference);
    }

    @Nonnull
    @Override
    public NorthReference getNorthReference()
    {
        return northReference;
    }

    @Override
    public void setBearingUnits(@Nonnull Angle bearingUnits) throws IllegalArgumentException
    {
        if (!BEARING_OPTIONS.contains(bearingUnits)) throw new IllegalArgumentException("Bearing option " + bearingUnits.getName() + " is not a valid unit for Bullseye");
        propertyChangeSupport.updateAndFire(BEARING_UNITS, this.bearingUnits, bearingUnits, () -> this.bearingUnits = bearingUnits);
    }

    @Nonnull
    @Override
    public Angle getBearingUnits()
    {
        return bearingUnits;
    }

    @Override
    public void setSpanUnits(@Nonnull Span units)
    {
        propertyChangeSupport.updateAndFire(SPAN_UNITS, spanUnits, units, () -> spanUnits = units);
    }

    @Nonnull
    @Override
    public Span getSpanUnits()
    {
        return spanUnits;
    }

    @Override
    public void setShowRangeRings(boolean showRangeRings)
    {
        propertyChangeSupport.updateAndFire(SHOW_RANGE_RINGS, this.showRangeRings, showRangeRings, () -> this.showRangeRings = showRangeRings);
    }

    @Override
    public boolean isShowRangeRings()
    {
        return showRangeRings;
    }

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        return ExtentFactory.createExtent(globe, getLocation(), getRadius() * getRingCount());
    }
}
