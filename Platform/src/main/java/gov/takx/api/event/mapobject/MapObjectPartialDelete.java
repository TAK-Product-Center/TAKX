package gov.takx.api.event.mapobject;

import gov.takx.api.mapobject.IMapObject;

/**
 * This event is fired when a map object is partially deleted.
 * <p>
 * The RDMs associated with the MapObject will still exist at the time this event is fired.
 * They may not exist (depending on threading execution) at any point afterwards.
 *
 * @since 2.38
 */
public class MapObjectPartialDelete extends AMapObjectEvent
{
    private final long timeOfDeletion;

    /**
     * Partial delete event for a map object.
     *
     * @param mapObject      The map object that was partially deleted
     * @param timeOfDeletion The time of deletion, all RDMs earlier than this cutoff are being deleted
     */
    public MapObjectPartialDelete(IMapObject mapObject, long timeOfDeletion)
    {
        super(mapObject);
        this.timeOfDeletion = timeOfDeletion;
    }

    /**
     * @return The time of deletion, all RDMs earlier than this cutoff are being deleted.
     */
    public long getTimeOfDeletion()
    {
        return timeOfDeletion;
    }
}
