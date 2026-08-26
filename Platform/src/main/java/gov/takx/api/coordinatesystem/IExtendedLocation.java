package gov.takx.api.coordinatesystem;

/**
 * Provides an additional flag used to determine if the location presented to the recipient is actually a user-generated
 * location, in lieu of a system-generated location.  The location can also be marked as coming from an undo
 * operation so that, when it is applied, the current location is not added to the undo stack again.
 *
 * @since 1.20
 */
public interface IExtendedLocation extends ILocation
{
    /**
     * Identifies if this location is user generated.
     *
     * @return true, if the location is user generated (instead of system or programmatically generated)
     */
    boolean isUserGenerated();

    /**
     * @return {@code true} if this is an undo, {@code false} otherwise
     */
    boolean isUndoOperation();
}
