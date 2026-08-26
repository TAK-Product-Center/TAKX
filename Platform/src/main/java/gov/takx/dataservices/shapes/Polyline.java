package gov.takx.dataservices.shapes;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutablePolyline;
import gov.takx.api.shapes.ShapeProperties;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Implementation of a polyline (path) object.
 *
 */
public class Polyline extends AIrregularShape implements IMutablePolyline
{
    private double verticalHeight = 10;
    private boolean verticalHeightEnabled = false;

    /**
     * Default constructor.
     * @since 3.3
     */
    public Polyline()
    {
    }

    /**
     * Create a new polyline instance with the given name and list of vertex locations.
     *
     * @param name      display name for shape
     * @param vertices  List of vertex locations
     * @throws IllegalArgumentException If the set of vertices does not contain at least 2 points
     */
    public Polyline(@Nonnull String name, @Nonnull List<? extends ILocation> vertices)
            throws IllegalArgumentException
    {
        setName(name);
        setVertices(vertices);
    }

    @Override
    protected int getMinVertexCount()
    {
        return 2;
    }

    @Override
    public void setVerticalHeight(double verticalHeight)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.VERTICAL_HEIGHT, this.verticalHeight, verticalHeight,
                () -> this.verticalHeight = verticalHeight);
    }

    @Override
    public void setVerticalHeightEnabled(boolean verticalHeightEnabled)
    {
        propertyChangeSupport.updateAndFire(ShapeProperties.VERTICAL_HEIGHT_ENABLED, this.verticalHeightEnabled,
                verticalHeightEnabled, () -> this.verticalHeightEnabled = verticalHeightEnabled);
    }

    @Override
    public double getVerticalHeight()
    {
        return verticalHeight;
    }

    @Override
    public boolean isVerticalHeightEnabled()
    {
        return verticalHeightEnabled;
    }

    @Override
    public boolean canUseVerticalHeight()
    {
        return true;
    }
}
