package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for shapes that have concentric rings.
 *
 * @since 3.3
 */
@NoPluginImplementations
public interface IRingedShape extends IRegularShape
{
    int MAX_RING_COUNT = 25;
    int MIN_RING_COUNT = 1;

    /**
     * @return The number of rings to render for this shape (default is 1)
     */
    int getRingCount();
}
