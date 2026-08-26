package gov.tak.core.map.layer;

import jakarta.annotation.Nonnull;

/**
 * Interface for classes that can provide deconfliction of objects collected during a pick.
 *
 * @since 5.5
 */
public interface IPickedObjectDeconflictor
{
    /**
     * Manipulate the provided {@link PickedObjectList} to deconflict pick results.
     *
     * @param pickedObjectList the picked object list
     * @param context the pick context
     */
    void deconflictPick(@Nonnull PickedObjectList pickedObjectList, @Nonnull IPickContext context);
}
