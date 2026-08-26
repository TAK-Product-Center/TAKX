package gov.takx.api.event.mapobject;

import gov.takx.api.mapobject.IMapObject;

/**
 * This event is fired when a map object is removed. A map object will be removed when it is suppressed or deleted.
 *
 * @since 2.38
 */
public class MapObjectRemoved extends AMapObjectEvent
{
    /**
     * Removed event for a map object.
     *
     * @param mapObject The map object that was removed
     */
    public MapObjectRemoved(IMapObject mapObject)
    {
        super(mapObject);
    }
}
