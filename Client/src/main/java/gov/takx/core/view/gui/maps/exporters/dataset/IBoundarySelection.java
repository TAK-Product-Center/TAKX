package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.nasa.worldwind.geom.Sector;

import java.util.Optional;

/**
 * Interface for the map cache boundary selection panels
 *
 * @since 2.7
 */
public interface IBoundarySelection
{
    /**
     * Get the bounding Sector for the currently selected boundary shape
     *
     * @return the active normalized sector as Optional.
     */
    Optional<Sector> getSector();

    /**
     * Set the BoundaryDrawingListener from the Dialog
     */
    void setBoundaryDrawingListener(IBoundaryDrawingListener listener);

    /**
     * @return the currently selected boundary shape to be used for map cache creation
     */
    Boundary getBoundary();

    /**
     * Stop the sector selector and remove listeners.
     */
    void cleanUp();
}
