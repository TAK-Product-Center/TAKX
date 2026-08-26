package gov.takx.core.view.gui.maps.exporters.dataset;

/**
 * Listener interface to get notifications of boundaries being drawn (or not)
 *
 * @since 1.49
 */
public interface IBoundaryDrawingListener
{
    /**
     * Callback notification that a boundary is being drawn (or not)
     *
     * @param drawing boolean actively drawing a boundary.
     */
    void onDrawingBoundary(boolean drawing);
}
