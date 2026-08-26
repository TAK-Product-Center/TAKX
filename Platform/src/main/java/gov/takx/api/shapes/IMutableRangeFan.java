package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.util.List;

/**
 * @since 1.16
 */
@NoPluginImplementations
public interface IMutableRangeFan extends IRangeFan, IMutableRegularShape
{
    /**
     * Set the orientation of the range fan.  This is the center line of bearing around which the fan is drawn.
     *
     * @param orientation Fan orientation (center LOB), in degrees, North is 0.0.
     */
    @Override
    void setOrientation(double orientation);

    /**
     * Specify whether the edge lines on the range rings should be rendered.  The default is {@code true}.
     *
     * @param showEdges {@code true} to render the edge lines, {@code false} to omit them
     */
    void setShowEdges(boolean showEdges);

    /**
     * Specify whether the orientation line of bearing (LOB) should be rendered.  The default is {@code false}.
     *
     * @param showOrientation {@code true} if the orientation line of bearing should be rendered
     */
    void setShowOrientation(boolean showOrientation);

    /**
     * Specify whether the range value annotations should be drawn.  The default is {@code true}.
     *
     * @param showRangeValues {@code true} to render range annotations, {@code false} to omit them
     */
    void setShowRangeValues(boolean showRangeValues);

    /**
     * Set the interval (in meters) at which to draw range rings.  Range rings will be drawn starting at minDistance
     * and at integer multiples of {@code interval} out to maxDistance.  The default value is 100.
     *
     * @param interval distance between range rings
     */
    void setInterval(double interval);

    /**
     * Set the range ring distances.  The range rings will be rendered at the specified distances.
     * The min/max distances (as set by {@link #setMinDistance(double)} and {@link #setMaxDistance(double)}) will
     * no longer apply.  Any existing range labels will be cleared, resulting in the labels being the
     * distance at the ring.
     *
     * @param rangeDistances List of range ring distances, must be in strictly increasing order
     *                       (meaning {@code rangeDistances[i] &gt; rangeDistances[i-1]} for all {@code i &gt; 0}).
     * @throws IllegalArgumentException If any of the range distances is &lt;= 0
     *                                  or if the range distances aren't in strictly increasing order
     * @see #clearRangeDistances()
     * @since 3.4 (parameter changed from array to list)
     */
    default void setRangeDistances(@Nonnull List<Double> rangeDistances) throws IllegalArgumentException
    {
        setRangeDistances(rangeDistances, null);
    }

    /**
     * Set the range ring distances and an associated set of labels to use on the ring (in lieu of the distance).
     * The lists must have an equal number of entries.  The range rings will be rendered at the specified distances.
     * The min/max distances (as set by {@link #setMinDistance(double)} and {@link #setMaxDistance(double)}) will
     * no longer apply.  The specified labels will only be rendered if {@link #isShowRangeValues()} is
     * {@code true}.
     *
     * @param rangeDistances List of range ring distances, must be in strictly increasing order
     *                       (meaning {@code rangeDistances[i] &gt; rangeDistances[i-1]} for all {@code i &gt; 0})
     * @param labels         Labels to use on range rings, may be {@code null} to use distances instead
     * @throws IllegalArgumentException If any of the range distances is &lt;= 0,
     *                                  if the range distances aren't in strictly increasing order,
     *                                  or if the range distances and labels don't have the same number of
     *                                  entries (unless labels is {@code null})
     * @see #clearRangeDistances()
     * @since 3.4 (parameters changed from arrays to lists)
     */
    void setRangeDistances(@Nonnull List<Double> rangeDistances, @Nullable List<String> labels) throws IllegalArgumentException;

    /**
     * If any discrete ring distances have been configured, clear them and draw range rings according to min/max/interval.
     *
     * @since 3.4
     * @see #setRangeDistances(List)
     * @see #setRangeDistances(List, List)
     */
    void clearRangeDistances();

    /**
     * Set the colors to be used, in sequence, on the range rings.  If there are more range rings to be drawn than colors,
     * then the colors are reused in sequence.  The {@code colors} list must not be empty, and all entries in the list
     * must be non-null.
     * <p>
     * If you want to revert to the default colors, use {@link gov.takx.dataservices.shapes.RangeFan#DEFAULT_COLORS}.
     * @since 3.4
     */
    void setRangeColors(@Nonnull List<Color> colors);

    /**
     * Set the maximum distance (in meters) at which to draw range rings.  It is possible for no range ring may be
     * drawn at this exact distance if minDistance plus some integer multiple of the interval doesn't equal maxDistance.
     * The default value is 500.
     *
     * @param maxDistance Maximum distance at which to draw range rings
     */
    void setMaxDistance(double maxDistance);

    /**
     * Set the minimum distance (in meters) for the range rings.  This is the minimum distance from the center points
     * at which a range ring will be drawn.  The default is 100.
     *
     * @param minDistance Minimum distance for first range ring
     */
    void setMinDistance(double minDistance);

    /**
     * Set the sweep angle (in degrees) to be covered by the range rings.  The default sweep is 360 (a full circle).
     *
     * @param sweep Sweep angle
     */
    void setSweep(double sweep);

    /**
     * Specifies how the range distance is interpreted.
     *
     * @param type The type of range. Either {@link #FOLLOW_TERRAIN} or {@link #GREAT_CIRCLE}.
     */
    void setRangeType(int type);

    /**
     * Specifies the sampling distance (in meters) used to compute terrain following range fans. This value is only used
     * for terrain following range fans.
     *
     * @param distance The sampling distance used to compute the position of a terrain following range fan.
     */
    void setSampleDistance(double distance);

    /**
     * Specifies a list of calculated locations for each ring when {@link #FOLLOW_TERRAIN} is used.
     * @param ringLocations a list of calculated locations for each of the range fan's rings
     * @since 5.5
     */
    void setFollowTerrainRingLocations(List<List<ILocation>> ringLocations);
}
