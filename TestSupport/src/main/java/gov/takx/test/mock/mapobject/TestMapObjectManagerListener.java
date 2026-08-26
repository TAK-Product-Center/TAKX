package gov.takx.test.mock.mapobject;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManagerListener;
import jakarta.annotation.Nonnull;

/**
 * Listener for notifications from the map object manager.
 *
 * @since 3.4
 */
@SuppressWarnings("unused") // full implementation provided for test support
public class TestMapObjectManagerListener implements IMapObjectManagerListener
{
    private int mapObjectsAddedCount = 0;
    private int mapObjectsRemovedCount = 0;
    private int mapObjectsPartiallyDeletedCount = 0;
    private IMapObject lastMapObjectAdded;
    private IMapObject lastMapObjectRemoved;
    private IMapObject lastMapObjectPartiallyDeleted;
    private long lastTimeOfDeletion;

    @Override
    public void clientMapObjectAdded(@Nonnull IMapObject mapObject)
    {
        mapObjectsAddedCount++;
        lastMapObjectAdded = mapObject;
    }

    @Override
    public void clientMapObjectRemoved(@Nonnull IMapObject mapObject)
    {
        mapObjectsRemovedCount++;
        lastMapObjectRemoved = mapObject;
    }

    @Override
    public void mapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
    {
        mapObjectsPartiallyDeletedCount++;
        lastMapObjectPartiallyDeleted = mapObject;
        lastTimeOfDeletion = timeOfDeletion;
    }

    /**
     * Resets state of listener.
     */
    public void reset()
    {
        mapObjectsAddedCount = 0;
        mapObjectsRemovedCount = 0;
        mapObjectsPartiallyDeletedCount = 0;
        lastMapObjectAdded = null;
        lastMapObjectRemoved = null;
        lastMapObjectPartiallyDeleted = null;
        lastTimeOfDeletion = 0L;
    }

    /**
     * @return The number of map objects added since this listener was created or reset.
     */
    public int getMapObjectsAddedCount()
    {
        return mapObjectsAddedCount;
    }

    /**
     * @return The number of map objects removed since this listener was created or reset.
     */
    public int getMapObjectsRemovedCount()
    {
        return mapObjectsRemovedCount;
    }

    /**
     * @return The number of map objects partially deleted since this listener was created or reset.
     */
    public int getMapObjectsPartiallyDeletedCount()
    {
        return mapObjectsPartiallyDeletedCount;
    }

    /**
     * @return The last map object added to this listener.
     */
    public IMapObject getLastMapObjectAdded()
    {
        return lastMapObjectAdded;
    }

    /**
     * @return The last map object removed from this listener.
     */
    public IMapObject getLastMapObjectRemoved()
    {
        return lastMapObjectRemoved;
    }

    /**
     * @return The last map object partially deleted from this listener.
     */
    public IMapObject getLastMapObjectPartiallyDeleted()
    {
        return lastMapObjectPartiallyDeleted;
    }

    /**
     * @return The last time a map object was partially deleted from this listener.
     */
    public long getLastTimeOfDeletion()
    {
        return lastTimeOfDeletion;
    }
}
