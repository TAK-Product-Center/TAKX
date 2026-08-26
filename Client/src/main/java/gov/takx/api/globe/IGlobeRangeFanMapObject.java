package gov.takx.api.globe;

import gov.takx.api.platform.IProgressCallback;
import gov.takx.api.shapes.IRangeFan;
import jakarta.annotation.Nonnull;

/**
 * Map object that draws rings to show ranges around a center point. This map object can draw full rings, or arcs
 * to show range fans in a particular direction.
 * <p>
 * The range distance can follow terrain or a great circle arc, depending on the range type.
 *
 * @see IRangeFan#getRangeType()
 * @since 1.16
 */
public interface IGlobeRangeFanMapObject extends IGlobeShapeMapObject
{
    /**
     * Registers a callback to monitor long running range fan calculations. The callback is only used for terrain
     * following range fans.
     *
     * @param progressCallback progress callback to add
     * @since 3.4
     */
    void addProgressCallback(@Nonnull IProgressCallback progressCallback);

    /**
     * Remove a previously registered callback.
     *
     * @param progressCallback callback to remove
     * @since 3.4
     */
    void removeProgressCallback(@Nonnull IProgressCallback progressCallback);
}
