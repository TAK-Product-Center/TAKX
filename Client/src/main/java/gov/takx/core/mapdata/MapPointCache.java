package gov.takx.core.mapdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides an LRU cache of {@link MapPoint} objects.  It behaves as a standard {@link Map} with the keys being
 * the ID of the RDM that the MapPoint represents.
 *
 * @since 1.13
 */
public class MapPointCache extends LinkedHashMap<Integer, MapPoint>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int MAX_POINTS_CEILING = 100000;

    private int maxEntries;

    /**
     * Construct a cache with the given size limit.
     *
     * @param maxEntries Maximum number of MapPoints to allow in the cache.
     */
    public MapPointCache(int maxEntries)
    {
        // Pass 3rd arg as true to order entries by last access
        super((int) (Math.min(maxEntries, MAX_POINTS_CEILING) * 1.4), 0.75f, true);

        this.maxEntries = maxEntries;
    }

    /**
     * Set a new max number of MapPoints to allow in the cache.
     *
     * @param maxEntries Maximum number of MapPoints to allow in the cache.
     */
    public void setMaxEntries(int maxEntries)
    {
        this.maxEntries = maxEntries;
    }

    /**
     * Overridden just to add synchronization.
     */
    @Override
    public synchronized MapPoint put(Integer key, MapPoint value)
    {
        return super.put(key, value);
    }

    /**
     * Overridden just to add synchronization.
     */
    @Override
    public synchronized MapPoint get(Object key)
    {
        return super.get(key);
    }

    /**
     * Delete all the MapPoints in the cache that belong to the given MapObject.
     *
     * @param moId ID of MapObject being deleted
     * @since 1.34
     */
    public synchronized void cleanMapObject(int moId)
    {
        final Iterator<MapPoint> iterator = values().iterator();

        while (iterator.hasNext())
        {
            final MapPoint mapPoint = iterator.next();

            if (mapPoint.getMoId() == moId)
            {
                if (logger.isTraceEnabled()) logger.trace("clean MP: {}", mapPoint);
                iterator.remove();
            }
        }
    }

    /**
     * Delete all the MapPoints in the cache that belong to the given MapObject and are older than the deletion
     * cutoff time.
     *
     * @param moId           ID of MapObject being deleted
     * @param timeOfDeletion Deletion cutoff time
     * @since 1.34
     */
    public synchronized void cleanMapObject(int moId, long timeOfDeletion)
    {
        final Iterator<MapPoint> iterator = values().iterator();

        while (iterator.hasNext())
        {
            final MapPoint mapPoint = iterator.next();

            if (mapPoint.getMoId() == moId && mapPoint.getTime() <= timeOfDeletion)
            {
                if (logger.isTraceEnabled()) logger.trace("clean MP: {}", mapPoint);
                iterator.remove();
            }
        }
    }

    /**
     * Determine if the eldet entry should be removed.
     *
     * @param eldest the least recently accessed entry
     * @return {@code tue} if the entry should be removed
     */
    protected boolean removeEldestEntry(Map.Entry<Integer, MapPoint> eldest)
    {
        final boolean remove = size() > maxEntries;

        if (remove && logger.isTraceEnabled()) logger.trace("Remove cache entry: {}", eldest.getValue());

        return remove;
    }
}
