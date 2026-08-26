package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface to a mutable arrow shape. See {@link IArrow} for more information.
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface IMutableArrow extends IArrow, IMutableIrregularShape
{
    /**
     * Set the bearing of the arrow.
     *
     * @param bearing New bearing, in degrees from true north
     */
    void setBearing(double bearing);

    /**
     * Set the range of the arrow.
     *
     * @param range New range, in meters
     */
    void setRange(double range);

    /**
     * Set the first vertex of the arrow, using the range and bearing parameters to calculate the ending point.
     *
     * @param startingPoint The arrow starting point
     */
    void setStartingPoint(@Nonnull ILocation startingPoint);

    /**
     * Sets the start and end points for the arrow, with the side effect of updating the range and bearing values to
     * match. This method requires exactly 2 points. Use {@link #setStartingPoint(ILocation)} to set only the starting
     * point of the arrow if the ending point is unknown.
     *
     * @param points The start and end points for the arrow
     * @see #setStartingPoint(ILocation)
     * @throws IllegalArgumentException if there are not exactly 2 points provided
     */
    @Override
    void setVertices(@Nonnull List<? extends ILocation> points) throws IllegalArgumentException;
}
