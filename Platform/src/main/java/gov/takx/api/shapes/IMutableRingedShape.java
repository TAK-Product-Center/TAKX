package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for shapes that have concentric rings.
 *
 * @since 3.3
 */
@NoPluginImplementations
public interface IMutableRingedShape extends IRingedShape, IMutableRegularShape
{
    /**
     * Sets the number of concentric rings to render (default is 1).
     *
     * @throws IllegalArgumentException if ring count is less than {@value MIN_RING_COUNT} or greater than
     *                                  {@value MAX_RING_COUNT}.
     */
    void setRingCount(int ringCount);
}
