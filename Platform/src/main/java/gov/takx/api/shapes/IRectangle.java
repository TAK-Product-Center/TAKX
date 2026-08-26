package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nullable;

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
    @Nullable
    ILocation getMinCorner();

    /**
     * @return the maximum corner of the IRectangle
     */
    @Nullable
    ILocation getMaxCorner();

    /**
     * @return The 4 vertices of this rectangle in cardinal order: SW, SE, NE, NW.
     */
    @Nullable
    default List<ILocation> getVertices()
    {
        return getVertices(false);
    }

    /**
     * Get the vertices of this rectangle in one of the following orders:
     * <ul>
     *     <li>CoT Order: {@code UL, LL, LR, UR (CCW from UL)}</li>
     *     <li>Legacy Order: {@code LL, LR, UR, UL (CCW from LL)}, also noted as {@code SW, SE, NE, NW}</li>
     * </ul>
     * @param takOrder {@code true} to return the vertices in the order required in CoT for other TAK products, {@code false} to return them in legacy order
     * @return The 4 vertices of this rectangle in either CoT or legacy order.
     * @since 5.8
     */
    @Nullable
    List<ILocation> getVertices(boolean takOrder);
}
