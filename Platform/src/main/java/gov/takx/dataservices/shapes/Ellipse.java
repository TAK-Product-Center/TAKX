package gov.takx.dataservices.shapes;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableEllipse;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 */
public class Ellipse extends ARingedShape implements IMutableEllipse, ExtentHolder
{
    private double semiMajorAxis;
    private double semiMinorAxis;

    /**
     * Default constructor.
     * @since 3.3
     */
    public Ellipse()
    {
    }

    /**
     * Create a new ellipse instance with the given center point and major/minor axes.
     *
     * @param name             display name for shape
     * @param location         Location of center point
     * @param orientation      Orientation angle
     * @param semiMajorAxis    Semi-Major axis length of the ellipse
     * @param semiMinorAxis    Semi-Minor axis length of the ellipse
     */
    public Ellipse(@Nonnull String name, @Nonnull ILocation location, double orientation,
                   double semiMajorAxis, double semiMinorAxis)
    {
        setName(name);
        setLocation(location);
        setOrientation(orientation);
        setSemiMajorAxis(semiMajorAxis);
        setSemiMinorAxis(semiMinorAxis);
    }

    @Override
    public double getSemiMajorAxis()
    {
        return semiMajorAxis;
    }

    @Override
    public void setSemiMajorAxis(double semiMajorAxis)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.SEMI_MAJOR_AXIS, this.semiMajorAxis, semiMajorAxis,
                () -> this.semiMajorAxis = semiMajorAxis);
    }

    @Override
    public double getSemiMinorAxis()
    {
        return semiMinorAxis;
    }

    @Override
    public void setSemiMinorAxis(double semiMinorAxis)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.SEMI_MINOR_AXIS, this.semiMinorAxis, semiMinorAxis,
                () -> this.semiMinorAxis = semiMinorAxis);
    }

    @Override
    public void setAxes(double semiMajorAxis, double semiMinorAxis)
    {
        setSemiMajorAxis(semiMajorAxis);
        setSemiMinorAxis(semiMinorAxis);
    }

    @Override
    public boolean contains(ILocation location)
    {
        // RX lies; the "minor" axis can be longer than the "major" axis. The reported angle is always relative to the
        // nominally "minor" axis. The ShapeUtils contains method requires that it be relative to the major axis.
        // Account for that here.
        double minorAxisLength = getSemiMinorAxis() * 2;
        double majorAxisLength = getSemiMajorAxis() * 2;
        double angle = getOrientation();

        final ILocation myLocation = getLocation();

        return minorAxisLength > majorAxisLength
                // Minor is really major, so the rotation is correct. Just reverse "major" and "minor".
                ? ShapeUtils.ellipseContainsPoint(myLocation, majorAxisLength, minorAxisLength, angle, myLocation)
                // Minor is really minor, so adjust rotation to be relative to major axis. It's okay to overshoot as
                // long as it aligns with one end of the major axis (i.e. 90 and 270 are equivalent).
                : ShapeUtils.ellipseContainsPoint(myLocation, minorAxisLength, majorAxisLength, (angle + 90) % 360, myLocation);
    }

    @Override
    public List<ILocation> getVertices()
    {
        return ShapeUtils.getLocations(this);
    }

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        double radius = Math.max(getSemiMajorAxis(), getSemiMinorAxis()) * getRingCount();
        return ExtentFactory.createExtent(globe, getLocation(), radius);
    }
}
