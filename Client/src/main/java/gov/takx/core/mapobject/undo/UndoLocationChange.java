package gov.takx.core.mapobject.undo;

import gov.takx.api.coordinatesystem.ExtendedLocation;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.core.undo.UndoOperation;

/**
 * Undo operation to manage user-initiated location changes.
 *
 * @see MapObjectUndoHandler
 * @since 1.26
 */
public class UndoLocationChange implements UndoOperation
{
    private final IMapObjectController moc;
    private final ILocation location;

    /**
     * Construct on the given MO controller, save the location from the MO.
     *
     * @param moc MO Controller
     */
    public UndoLocationChange(IMapObjectController moc)
    {
        this.moc = moc;
        location = moc.getMapObject().getLocation();
    }

    /**
     * Restore the configured location.
     */
    @Override
    public void apply()
    {
        // Construct a location marked as an undo operation so we can avoid this operation clearing our undo stack.
        moc.sendLocationChangeMessage(new ExtendedLocation(location, true, true));
    }

    /**
     * Equal if the locations are the same.
     *
     * @param o Object to compare against
     * @return {@code true} if locations are the same
     */
    @Override
    public boolean equals(Object o)
    {
        return o instanceof UndoLocationChange && location.equals(((UndoLocationChange) o).location);
    }

    @Override
    public int hashCode()
    {
        return location.hashCode();
    }
}
