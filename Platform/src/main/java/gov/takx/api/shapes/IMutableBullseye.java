package gov.takx.api.shapes;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Interface to a mutable bullseye shape. See {@link IBullseye} for more information.
 * @since 5.5
 */
@NoPluginImplementations
public interface IMutableBullseye extends IBullseye, IMutableRingedShape
{
    /**
     * @param radius the new radius of the bullseye
     * @param units the {@link Span} units of the radius
     * @throws IllegalArgumentException if the radius is &lt;{@link #MIN_RADIUS} or &gt;{@link #MAX_RADIUS}
     */
    void setRadius(double radius, @Nonnull Span units) throws IllegalArgumentException;

    /**
     * @param ringRadius the new ring interval radius of the bullseye
     * @param units the {@link Span} units of the ring radius
     * @throws IllegalArgumentException if the ringRadius is &lt;{@link #MIN_RING_RADIUS} or the {@code getRingCount()*ringRadius} &gt;{@link #MAX_RING_RADIUS}
     */
    void setRingRadius(double ringRadius, @Nonnull Span units) throws IllegalArgumentException;

    /**
     * @param pointingEdgeToCenter {@code true} if the bullseye should point from the edge to the center, {@code false} if not
     */
    void setPointingEdgeToCenter(boolean pointingEdgeToCenter);

    /**
     * @param northReference the new {@link NorthReference} to use for the bullseye
     */
    void setNorthReference(@Nonnull NorthReference northReference);

    /**
     * @param bearingUnits the new bearing {@link Angle} to use for the bullseye
     * @throws IllegalArgumentException if the passed in {@link Angle} is invalid (not one of {@link Angle#DEGREE} or {@link Angle#MIL})
     */
    void setBearingUnits(@Nonnull Angle bearingUnits) throws IllegalArgumentException;

    /**
     * Sets the units for use in the rendering of the annotations, does not change the underlying units for {@link #getRadius()} or {@link #getRingRadius()}
     * @param units the new {@link Span} units to use for rendering the bullseye's annotations
     */
    void setSpanUnits(@Nonnull Span units);

    /**
     * @param showRangeRings {@code true} if the range rings should be shown
     */
    void setShowRangeRings(boolean showRangeRings);

}
