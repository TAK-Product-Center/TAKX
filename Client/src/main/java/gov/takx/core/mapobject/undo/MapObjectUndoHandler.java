package gov.takx.core.mapobject.undo;

import gov.takx.api.coordinatesystem.IExtendedLocation;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.core.undo.UndoHandler;

/**
 * Each MapObject has an undo handler. Currently, only 1 thing can be undone:
 * <ol>
 * <li>A user-driven (via the UI) location change</li>
 * </ol>
 * <p/>
 * These undoable operations are maintained in a LIFO manner with the unique caveat that all undoable
 * operations are removed from the undo list whenever a non-user driven location change occurs.
 *
 * @since 1.26
 */
public class MapObjectUndoHandler extends UndoHandler
{
    private final IMapObjectController moc;

    /**
     * Construct the undo handler for the given MO controller.  Only events generated on this MO will be handled
     * here.
     *
     * @param moc MO Controller
     */
    public MapObjectUndoHandler(IMapObjectController moc)
    {
        this.moc = moc;
    }

    /**
     * Process a location change on the MO.  If the change is user-initiated, then create an undo operation for it.
     * Otherwise, clear the undo list because any non-user location change is defined to clear the undo stack for that
     * MO.
     *
     * @param location The location being applied to the MO
     */
    public void processLocationChange(ILocation location)
    {
        if (location instanceof final IExtendedLocation extendedLocation)
        {
            // If the location is user generated and is not an undo operation, then add an undo operation for it

            if (extendedLocation.isUserGenerated() && !extendedLocation.isUndoOperation())
            {
                addUndoOperation(new UndoLocationChange(moc));
            }
        } else
        {
            // No user-initiated, clear the undo list for this MO
            undoList.clear();
        }
    }
}
