package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to an immutable arrow shape.
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface IArrow extends IIrregularShape
{
    /**
     * @return The bearing (or direction) of the arrow, in degrees from true north
     */
    double getBearing();

    /**
     * @return The range (or length) of the arrow, in meters
     */
    double getRange();

    /**
     * @return {@code true} if the arrow is a dynamic measure, {@code false} if not.
     * @since 5.0
     */
    boolean isDynamic();

    /**
     * Sets whether the arrow is a dynamic measure arrow.
     *
     * @param dynamic {@code true} if dynamic measure, {@code false} if not.
     * @since 5.0
     */
    void setDynamic(boolean dynamic);

    /**
     * @return The display text for the bearing of this arrow according to its current configuration
     * @since 5.0
     */
    String getBearingLabelText();

    /**
     * @return The display text for the range of this arrow according to its current configuration
     * @since 5.0
     */
    String getRangeLabelText();

    /**
     * @return {@code true} if the arrow range is locked, {@code false} if not.
     * @since 5.0
     */
    boolean isRangeLocked();

    /**
     * When range locked, the arrow's length cannot be changed.
     *
     * @param rangeLocked {@code true} if the arrow range is locked, {@code false} if not.
     * @since 5.0
     */
    void setRangeLocked(boolean rangeLocked);
}
