package gov.takx.core.view.gui.maps.exporters.dataset;

/**
 * Listener for changes in the user-selected Boundary
 *
 * @since 2.7
 */
public interface IBoundaryChangeListener
{
    /**
     * Notification that the user-selected Boundary has changed
     *
     * @param boundary
     */
    public void onBoundaryChanged(Boundary boundary);
}
