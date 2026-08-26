package gov.takx.dataservices.shapes;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableSquare;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

/**
 * Implementation of a square shape.
 *
 */
public class Square extends Rectangle implements IMutableSquare
{
    /**
     * Default constructor.
     * @since 3.3
     */
    public Square()
    {
    }

    /**
     * Create a new square instance with the given name, center point and side length.
     *
     * @param name        display name for shape
     * @param location    Location of center point
     * @param orientation Orientation angle
     * @param sideLength  The length of the sides of the square
     */
    public Square(@Nonnull String name, @Nonnull ILocation location, double orientation, double sideLength)
    {
        super(name, location, orientation, sideLength, sideLength);
    }

    /**
     * Create a new square instance with the given name, and minimum and maximum corners.
     *
     * @param name             display name for shape
     * @param minCorner        the minimum corner for the square
     * @param maxCorner        the maximum corner for the square
     * @since 5.7
     */
    public Square(@Nonnull String name, @Nonnull ILocation minCorner, @Nonnull ILocation maxCorner)
    {
        super(name, minCorner, maxCorner);
    }

    @Override
    public double getSideLength()
    {
        return getWidth();
    }

    @Override
    public void setSideLength(double sideLength)
    {
        setSize(sideLength, sideLength);
    }

    @Override
    public void setWidth(double width)
    {
        setSideLength(width);
    }

    @Override
    public void setHeight(double height)
    {
        setSideLength(height);
    }

    @Override
    public boolean contains(ILocation location)
    {
        return ShapeUtils.polygonContainsLocation(getVertices(), location);
    }
}
