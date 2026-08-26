package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapObjectOfflineMessage;

/**
 */
public class MapObjectOfflineMessage implements IMapObjectOfflineMessage
{
    private final IMapObject mapObject;
    private final long timeOfOperation;

    /**
     * Construct a message indicating that the given MapObject is going offline (being deleted/suppressed).
     *
     * @param mapObject The MapObject going offline
     */
    public MapObjectOfflineMessage(IMapObject mapObject)
    {
        this.mapObject = mapObject;
        timeOfOperation = System.currentTimeMillis();
    }

    @Override
    public String getMapObjectKey()
    {
        return mapObject.getKey();
    }

    @Override
    public String getFamily()
    {
        return mapObject.getFamily();
    }

    @Override
    public String getType()
    {
        return mapObject.getType();
    }

    @Override
    public String getUnitID()
    {
        return mapObject.getUnitID();
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public long getTimeOfOperation()
    {
        return timeOfOperation;
    }
}
