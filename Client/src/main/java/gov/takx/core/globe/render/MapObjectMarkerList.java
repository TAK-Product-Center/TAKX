package gov.takx.core.globe.render;

import gov.takx.api.globe.IMapObjectMarkerList;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.mapdata.MapPoint;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Implementation of marker list used in the rendering engine.
 *
 * @since 1.13
 */
public class MapObjectMarkerList implements IMapObjectMarkerList
{
    private final Set<MapPoint> mapPoints = new ConcurrentSkipListSet<>();
    private final IMapObject mapObject;

    /**
     * Construct a new list for the given map object.
     *
     * @param mapObject map object owning the data in this list
     */
    public MapObjectMarkerList(IMapObject mapObject)
    {
        this.mapObject = mapObject;
    }

    /**
     * Add a map point to the list.
     *
     * @param mapPoint map point to add
     */
    public void addMapPoint(MapPoint mapPoint)
    {
        mapPoints.add(mapPoint);
    }

    /**
     * Clear all the points out of the list
     */
    public void clear()
    {
        mapPoints.clear();
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public int size()
    {
        return mapPoints.size();
    }

    @Override
    public Iterator<MapPoint> iterator()
    {
        return mapPoints.iterator();
    }
}
