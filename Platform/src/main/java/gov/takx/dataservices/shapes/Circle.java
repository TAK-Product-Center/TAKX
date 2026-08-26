package gov.takx.dataservices.shapes;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

/**
 * Implementation of a circle shape.
 *
 */
public class Circle extends ARingedShape implements IMutableCircle, ExtentHolder
{
    private double radius;

    /**
     * Default constructor.
     * @since 3.3
     */
    public Circle()
    {
    }

    /**
     * Create a new circle instance with the given name, center point and radius.
     *
     * @param name Shape name
     * @param location Location of center point
     * @param radius radius of the circle
     */
    public Circle(@Nonnull String name, @Nonnull ILocation location, double radius)
    {
        setName(name);
        setLocation(location);
        setRadius(radius);
    }

    @Override
    public double getRadius()
    {
        return radius;
    }

    @Override
    public void setRadius(double radius)
    {
        if (Double.isNaN(radius) || radius <= 0) throw new IllegalArgumentException("'radius' must be > 0");

        propertyChangeSupport.updateAndFire(ShapeProperties.RADIUS, this.radius, radius,
                () -> this.radius = radius);
    }

    @Override
    public boolean contains(ILocation location)
    {
        return ShapeUtils.circleContainsPoint(getLocation(), getRadius(), location);
    }

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        double radius = getRadius() * getRingCount();
        return ExtentFactory.createExtent(globe, getLocation(), radius);
    }
}
