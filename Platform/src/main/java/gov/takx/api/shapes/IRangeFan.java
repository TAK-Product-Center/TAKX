package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.util.List;

/**
 * Interface for controlling a range fan. A range fan draws rings to show ranges around a center point.
 *
 * @since 1.16
 */
@NoPluginImplementations
public interface IRangeFan extends IRegularShape
{
    /**
     * Indicates that the range distance is interpreted as a great circle distance.
     *
     * @see #getRangeType()
     */
    int GREAT_CIRCLE = 0;

    /**
     * Indicates that the range distance is interpreted as a terrain following distance.
     *
     * @see #getRangeType()
     */
    int FOLLOW_TERRAIN = 1;

    /**
     * @return The orientation of the range fan
     */
    @Override
    double getOrientation();

    /**
     * @return {@code true} if the edge lines are to be rendered
     */
    boolean isShowEdges();

    /**
     * @return {@code true} if the orientation line of bearing should be rendered
     */
    boolean isShowOrientation();

    /**
     * @return {@code true} if range value annotations are to be rendered
     */
    boolean isShowRangeValues();

    /**
     * @return Interval between range rings
     */
    double getInterval();

    /**
     * @return Range ring distances, if set, {@code null} otherwise
     */
    @Nullable
    List<Double> getRangeDistances();

    /**
     * @return Range ring labels, if set, {@code null} otherwise
     */
    @Nullable
    List<String> getRangeLabels();

    /**
     * @return The colors to be used, cyclically, for each ring
     * @since 3.4
     */
    @Nonnull
    List<Color> getRangeColors();

    /**
     * @return Maximum distance at which to draw range rings
     */
    double getMaxDistance();

    /**
     * @return Distance of first range ring
     */
    double getMinDistance();

    /**
     * @return The sweep angle covered by the range rings
     */
    double getSweep();

    /**
     * Indicates how the range distance is interpreted.
     *
     * @return Either {@link #FOLLOW_TERRAIN} or {@link #GREAT_CIRCLE}.
     */
    int getRangeType();

    /**
     * Indicates the sampling distance (in meters) used to compute terrain following range fans. This value is only used
     * for terrain following range fans.
     *
     * @return The sampling distance used to compute the position of a terrain following range fan.
     */
    double getSampleDistance();

    /**
     * Returns a list of calculated locations for each ring when {@link #FOLLOW_TERRAIN} is used. This may return null
     * if the calculations are not complete or may return stale results if the calculations are in-flight.
     * @return a list of calculated locations for each ring, or {@code null}
     * @since 5.5
     */
    List<List<ILocation>> getFollowTerrainRingLocations();
}
