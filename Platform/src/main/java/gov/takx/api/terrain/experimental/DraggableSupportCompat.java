package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.drag.DragContext;
import gov.nasa.worldwind.drag.DraggableSupport;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;

/**
 * An implementation of {@link DraggableSupport} that avoids use of {@link gov.nasa.worldwind.terrain.SectorGeometry}
 * since we no longer support creation of WW SectorGeometry.
 *
 * @since 5.2
 */
public class DraggableSupportCompat extends DraggableSupport
{
    /**
     * Provides persistence of initial values of a drag operation to increase dragging precision and provide better
     * dragging behavior.
     *
     * @param dragObject   the object to be dragged.
     * @param altitudeMode the altitude mode.
     * @throws IllegalArgumentException if the object is null.
     */
    public DraggableSupportCompat(Object dragObject, int altitudeMode)
    {
        super(dragObject, altitudeMode);
    }

    @Override
    public void dragScreenSizeConstant(DragContext dragContext)
    {
        // delegate to dragGlobeSizeConstant
        dragGlobeSizeConstant(dragContext);
    }

    @Override
    public void dragGlobeSizeConstant(DragContext dragContext)
    {
        // Ideally we'd have access to the MapRenderer here so we can get the surface position for our screen coordinates
        // But since we're currently limited to what the DraggableSupport has access to, we settle for either:
        // 1. The "terrain pick" - this has been calculated with the MapRenderer and therefore gives us the highest quality results.
        //    We assume that dragContext.getPoint() is the same point as the pointer.
        // 2. View.computePositionFromScreenPoint() - since the SectorGeometry is empty, this will only return results on the ellipsoid.
        //    At low zoom levels this causes inaccurate dragging.
        PickedObject terrainObject = dragContext.getSceneController().getDrawContext().getPickedObjects().getTerrainObject();
        Position moveTo = terrainObject != null
                ? (Position) terrainObject.getObject()
                : dragContext.getSceneController().getView().computePositionFromScreenPoint(dragContext.getPoint().x, dragContext.getPoint().y);
        double referenceAltitude = getReferencePosition().getAltitude();
        doMove(new Position(moveTo, referenceAltitude), dragContext.getGlobe());
    }
}
