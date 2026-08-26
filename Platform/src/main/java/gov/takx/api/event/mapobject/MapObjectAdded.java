package gov.takx.api.event.mapobject;

import gov.takx.api.mapobject.IMapObject;

/**
 * This event is fired when a new map object is added.
 *
 * @since 2.38
 */
public class MapObjectAdded extends AMapObjectEvent
{
    /**
     * Added event for a map object.
     *
     * @param mapObject The map object that was added
     */
    public MapObjectAdded(IMapObject mapObject)
    {
        super(mapObject);
    }
}
