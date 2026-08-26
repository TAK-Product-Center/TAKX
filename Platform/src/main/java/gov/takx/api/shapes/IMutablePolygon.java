package gov.takx.api.shapes;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Interface to a mutable polygon shape. See {@link IPolygon} for more information.
 */
@NoPluginImplementations
public interface IMutablePolygon extends IMutableIrregularShape, IPolygon
{
    /**
     * Tries to add the polygon as a hole to this polygon. If the polygon is not a valid hole then an exceptions is
     * thrown. To be valid the all vertices of the polygon must be within the containing polygon and no points of the
     * containing polygon can be within the potential hole. Another requirement is that the hole can not be used by
     * any other shapes.
     *
     * @param polygon The polygon to add as a hole.
     * @since 1.48
     */
    void addHole(@Nonnull IMutablePolygon polygon) throws IllegalArgumentException;

    /**
     * Removes the hole from the polygon
     *
     * @param hole The hole to remove
     */
    void removeHole(@Nonnull IMutablePolygon hole);

    /**
     * Reattach (replace) the existing holes (model instances) with the given collection.  This is an internal method
     * for use when restoring from persistence.  It provides the mechanism to ensure that the model objects
     * referenced here are the same instances as the model objects from each Map Object that is a hole.
     *
     * @param restoredHoles Hole model objects to restore from
     * @since 3.3
     */
    @Internal
    void reattachHoles(Collection<IMutablePolygon> restoredHoles);
}
