package gov.takx.api.structures;

import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.terrain.Terrain;
import gov.tak.api.util.AttributeSet;

import java.util.List;

/**
 * Contract for surface structures that can determine intersection of a {@link Line},
 * through a structure to a given {@link Terrain}.
 *
 * @since 3.0
 */
public interface IIntersectable
{
    /**
     * Returns a boolean specifying whether this shape can be used in 3D line intersection calculations.  In general,
     * 3D shapes are intersectable, while 2D shapes are not.
     *
     * @return {@code true} if this shape can be used in line intersection calculations, or {@code false}
     * otherwise.
     */
    boolean isIntersectable();

    /**
     * Calculate the intersections between a line and this shape.
     *
     * @param line    the line to intersect with this shape.
     * @param terrain the to use when computing the shape's geometry.
     * @return a list of intersections identifying where the line intersects the shape, or {@code null} if the line does
     * not intersect the shape.
     * @throws InterruptedException if the intersection calculations were interrupted.
     */
    List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException;

    /**
     * @return The metadata associated with this intersectable, often used to hold values that affect calculations
     * associated with intersections.
     */
    AttributeSet getMetadata();
}
