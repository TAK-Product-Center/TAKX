package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWindow;

/**
 * Receives notifications when the user changes the viewing altitude on the globe.
 *
 * @since 2.26
 */
public interface IZoomListener
{
    /**
     * Invoked when the user changes the viewing altitude on the globe by at least 1%.
     *
     * @param worldWindow The globe model where the zoom event occurred.
     */
    void zoomChange(WorldWindow worldWindow);
}
