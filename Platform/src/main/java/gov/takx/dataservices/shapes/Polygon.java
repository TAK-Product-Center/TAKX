package gov.takx.dataservices.shapes;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import javax.naming.OperationNotSupportedException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of a polygon object.
 *
 */
public class Polygon extends Polyline implements IMutablePolygon
{
    private final Set<IMutablePolygon> holes = ConcurrentHashMap.newKeySet();

    /**
     * Default constructor.
     * @since 3.3
     */
    public Polygon()
    {
    }

    /**
     * Create a new polygon instance with the given name and list of vertex locations.
     *
     * @param name             display name for shape
     * @param vertices         List of vertex locations
     * @throws IllegalArgumentException If the set of vertices does not contain at least 3 points
     */
    public Polygon(@Nonnull String name, @Nonnull List<? extends ILocation> vertices)
            throws IllegalArgumentException
    {
        super(name, vertices);
    }

    @Override
    protected int getMinVertexCount()
    {
        return 3;
    }

    @Override
    public boolean contains(ILocation location)
    {
        return ShapeUtils.polygonContainsLocation(getVertices(), location);
    }

    @Override
    public boolean contains(IShape shape) throws OperationNotSupportedException
    {
        return ShapeUtils.polygonContainsShape(getVertices(), shape);
    }

    @Override
    public boolean intersects(IShape shape) throws OperationNotSupportedException
    {
        return ShapeUtils.polygonIntersectsShape(getVertices(), shape);
    }

    @Nonnull
    @Override
    public Set<IPolygon> getHoles()
    {
        return Collections.unmodifiableSet(holes);
    }

    @Override
    public void addHole(@Nonnull IMutablePolygon hole) throws IllegalArgumentException
    {
        if (hole == this)
        {
            throw new IllegalArgumentException("Can not add a hole to itself: " + hole.getName());
        } else if (holes.contains(hole))
        {
            throw new IllegalArgumentException(hole.getName() + " is already a hole in " + getName());
        } else if (!hole.getHoles().isEmpty())
        {
            throw new IllegalArgumentException(hole.getName() + " already has holes. It can not be added as a hole to: " + getName());
        } else if (!ShapeUtils.isEligibleHole(getVertices(), hole.getVertices()))
        {
            throw new IllegalArgumentException(hole.getName() + " vertices are not within " + getName());
        }

        holes.add(hole);
        propertyChangeSupport.firePropertyChange(ShapeProperties.HOLE_ADDED, null, hole);
        hole.setVisible(false); // Hide the polygon because it will become a hole in the WW shape
    }

    @Override
    public void removeHole(@Nonnull IMutablePolygon hole)
    {
        if (holes.remove(hole))
        {
            propertyChangeSupport.firePropertyChange(ShapeProperties.HOLE_REMOVED, hole, null);
            hole.setVisible(true); // Make it visible again since it is no longer a hole
        }
    }

    @Override
    public void reattachHoles(Collection<IMutablePolygon> restoredHoles)
    {
        holes.clear();
        holes.addAll(restoredHoles);

        // And fire events so listeners are aware of the new holes
        restoredHoles.forEach(hole -> propertyChangeSupport.firePropertyChange(ShapeProperties.HOLE_ADDED, null, hole));
    }
}
