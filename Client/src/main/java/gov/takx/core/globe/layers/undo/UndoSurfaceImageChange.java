package gov.takx.core.globe.layers.undo;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.core.globe.layers.SurfaceImageController;
import gov.takx.core.undo.AUndoOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * Undo operation to restore the previous corners on a surface image.
 *
 * @see SurfaceImageUndoHandler
 * @since 1.26
 */
public class UndoSurfaceImageChange extends AUndoOperation
{
    private final SurfaceImageController surfaceImageController;
    private final List<LatLon> corners;

    /**
     * Construct the undo action, save the given corner locations.
     *
     * @param surfaceImageController Controller of surface image we are working on
     * @param corners                The original corners to reinstate upon undo
     */
    public UndoSurfaceImageChange(SurfaceImageController surfaceImageController, List<LatLon> corners)
    {
        this.surfaceImageController = surfaceImageController;
        this.corners = new ArrayList<>(corners);
    }

    @Override
    public void apply()
    {
        surfaceImageController.setCorners(corners);
        globeManager.requestRedraw("Surface Image Undo");
    }
}
