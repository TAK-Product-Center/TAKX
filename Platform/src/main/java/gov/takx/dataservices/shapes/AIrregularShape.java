package gov.takx.dataservices.shapes;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableIrregularShape;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all irregular shape instances, that is, those defined by a set of vertex locations rather
 * than a center point and a set of defining characteristics (like radius, width, etc.).
 *
 */
public abstract class AIrregularShape extends AShape implements IMutableIrregularShape, ExtentHolder
{
    private List<? extends ILocation> vertices;

    /**
     * Default constructor.
     */
    protected AIrregularShape()
    {
    }

    /**
     * Overridden to move all the points of this shape according to the new starting location.
     * <p>
     * {@inheritDoc}
     * @since 5.5
     */
    @Override
    public void setLocation(ILocation location)
    {
        final var currentVertices = getVertices();
        if (currentVertices.isEmpty() || currentVertices.get(0).equals(location)) return;

        setVertices(ShapeUtils.movePoints(location, currentVertices, getAltitudeMode()));
    }

    @Nonnull
    @Override
    public List<ILocation> getVertices()
    {
        return vertices != null ? new ArrayList<>(vertices) : new ArrayList<>();
    }

    @Override
    public void setVertices(@Nonnull List<? extends ILocation> vertices)
    {
        setVertices(vertices, true);
    }

    /**
     * Set the list of vertex locations that define the shape, optionally skipping minimum vertex count validation.
     *
     * @param vertices list of vertex locations
     * @param validate {@code true} to validate for minimum vertex size, {@code false} to skip validation
     * @since 5.5
     * @throws IllegalArgumentException if validation fails
     */
    public void setVertices(@Nonnull List<? extends ILocation> vertices, boolean validate) throws IllegalArgumentException
    {
        final int minVertexCount = getMinVertexCount();
        if (validate && vertices.size() < minVertexCount)
        {
            throw new IllegalArgumentException(getClass().getSimpleName() + "s must have at least " + minVertexCount + " vertices, got " + vertices.size());
        }

        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATIONS, this.vertices, vertices,
                () -> this.vertices = new ArrayList<ILocation>(vertices));

        if (!vertices.isEmpty()) super.setLocation(vertices.get(0));
    }

    protected abstract int getMinVertexCount();

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        return ExtentFactory.createExtent(globe, getVertices());
    }
}
