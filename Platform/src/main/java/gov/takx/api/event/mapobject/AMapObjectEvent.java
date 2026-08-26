package gov.takx.api.event.mapobject;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMapObjectManagerListener;

/**
 * Abstract base class for CDI events that are fired by {@link IMapObjectManager}. Observing one
 * of these events is roughly equivalent to registering an {@link IMapObjectManagerListener}
 * via {@link IMapObjectManager#addMapObjectManagerListener(IMapObjectManagerListener)}. Note:
 * the listener interface includes three methods, so a class should observe the corresponding event for each method it
 * needs to handle:
 * <ul>
 *     <li>{@link IMapObjectManagerListener#clientMapObjectAdded(IMapObject)} -&gt; {@link MapObjectAdded}</li>
 *     <li>{@link IMapObjectManagerListener#clientMapObjectRemoved(IMapObject)} -&gt; {@link MapObjectRemoved}</li>
 *     <li>{@link IMapObjectManagerListener#mapObjectPartialDelete(IMapObject, long)} -&gt; {@link MapObjectPartialDelete}</li>
 * </ul>
 *
 * @since 2.38
 */
public abstract class AMapObjectEvent implements PlatformEvent
{
    private final IMapObject mapObject;

    /**
     * Constructor.
     *
     * @param mapObject The map object of interest
     */
    protected AMapObjectEvent(IMapObject mapObject)
    {
        this.mapObject = mapObject;
    }

    /**
     * @return The map object of interest
     */
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{mapObject=" + mapObject.getKey() + '}';
    }
}
