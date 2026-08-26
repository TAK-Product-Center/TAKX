package gov.takx.api.mapobject;

import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectPartialDelete;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import jakarta.annotation.Nonnull;

/**
 * Contract for receiving callbacks from the {@link IMapObjectManager} when Map Objects are added/removed.
 * <p>
 * Plugin developers are encouraged to use the newer CDI event notifications instead of this interface.
 *
 * @see MapObjectAdded
 * @see MapObjectRemoved
 * @see MapObjectPartialDelete
 */
public interface IMapObjectManagerListener
{
    /**
     * A Map Object has been added.
     *
     * @param mapObject Map Object in question
     */
    void clientMapObjectAdded(@Nonnull IMapObject mapObject);

    /**
     * A Map Object has been removed. A map object will be "removed" when it is suppressed or deleted.
     *
     * @param mapObject Map Object in question
     */
    void clientMapObjectRemoved(@Nonnull IMapObject mapObject);

    /**
     * Part of the data for a MapObject is being deleted.  The RDMs associated with the MapObject will still exist
     * at the time this method is called.  They may not exist (depending on threading execution) at any point
     * after this method returns.
     *
     * @param mapObject      MapObject being affected
     * @param timeOfDeletion The time of deletion, all RDMs earlier than this cutoff are being deleted
     * @since 1.34
     */
    void mapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion);
}
