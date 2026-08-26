package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface to a mutable rectangle shape. See {@link IRectangle} for more information.
 */
@NoPluginImplementations
public interface IMutableRectangle extends IRectangle, IMutableRegularShape
{
    /**
     * Sets the four vertices of this rectangle. The vertices must be cardinally ordered: SW, SE, NE, NW.
     *
     * @param vertices the vertices of this rectangle
     * @since 5.7
     */
    void setVertices(@Nonnull List<ILocation> vertices);

    /**
     * Set the Y-axis dimension (assuming 0 rotation) of a 2D rectangle.
     * If you are looking for the vertical (Z-axis) height, please use {@link #setVerticalHeight(double)}.
     * Set the height (Y-axis dimension) of the rectangle.
     *
     * @param height Y-axis dimension
     * @see #setVerticalHeight(double)
     */
    void setHeight(double height);

    /**
     * Set the width (X-axis dimension) of the rectangle
     *
     * @param width X-axis dimension
     */
    void setWidth(double width);

    /**
     * Set the length of both axes of the rectangle.
     *
     * @param width  X-axis dimension
     * @param height Y-axis dimension
     */
    void setSize(double width, double height);
}
