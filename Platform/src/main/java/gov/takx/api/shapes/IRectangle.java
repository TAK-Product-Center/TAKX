package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;

import java.util.List;

/**
 * Interface to an immutable rectangle shape.
 *
 */
@NoPluginImplementations
public interface IRectangle extends IRegularShape
{
    /**
     * Returns the Y-axis dimension of a 2D rectangle, assuming 0 rotation.
     * If you are looking for the vertical (Z-axis) height, please use {@link #getVerticalHeight()}.
     *
     * @see #getVerticalHeight()
     */
    double getHeight();

    /**
     * @return width (X-axis dimension) of the rectangle
     */
    double getWidth();

    /**
     * @return the minimum corner of the IRectangle
     */
    ILocation getMinCorner();

    /**
     * @return the maximum corner of the IRectangle
     */
    ILocation getMaxCorner();

    /**
     * @return The 4 vertices of this rectangle in cardinal order: SW, SE, NE, NW.
     */
    List<ILocation> getVertices();
}
