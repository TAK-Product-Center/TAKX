package gov.takx.core.mapdata;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.globe.render.IRendererSearchResult;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IRDMPersist;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.platform.globe.renderer.RendererSearchResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for determining if map objects and messages pass various criteria.
 */
@Singleton
public class MapDataUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MAX_SUMMARY_POINTS = 2000; // Max track points to show in the summary dialog

    private final IMapObjectManager mapObjectManager;
    private final ISmartTrackManager smartTrackManager;
    private final IRDMPersist rdmPersist;

    @Inject
    MapDataUtils(IMapObjectManager mapObjectManager, ISmartTrackManager smartTrackManager, IRDMPersist rdmPersist)
    {
        this.mapObjectManager = mapObjectManager;
        this.smartTrackManager = smartTrackManager;
        this.rdmPersist = rdmPersist;
    }

    /**
     * Determine if a given map object is viewable on the globe.  This is based on direct
     * visibility and the current security filter.
     *
     * @param mapObject The map object of interest
     * @return True if tracks are viewable
     * @see IMapObject#isVisible()
     * @see ITrackStyleDefinition#isVisible()
     */
    public boolean isMapObjectViewable(IMapObject mapObject)
    {
        // Protect against null MOs without killing things
        if (mapObject == null)
        {
            logger.error("NULL mapObject", new RuntimeException("NULL"));
            return false;
        }

        return mapObject.isVisible();
    }

    /**
     * Determine if a given map object's data (tracks) are viewable on the globe.  This is based on direct
     * visibility, show tracks settings, and the current security filter.
     *
     * @param mapObject The map object of interest
     * @return True if tracks are viewable
     * @see IMapObject#isVisible()
     * @see ITrackStyleDefinition#isVisible()
     */
    public boolean isMapDataViewable(IMapObject mapObject)
    {
        return mapObject.canHaveStyledTrackData()
                && isMapObjectViewable(mapObject)
                && smartTrackManager.getTrackStylingContext(mapObject).isVisible();
    }

    /**
     * Determine if two points are proximal to one another.  Basically, test to see if point2 lies within
     * the lat/lon bounding box created by adding and subtracting the proximity from point1 lat/lon position.
     *
     * @param proximity Proximity tolerance
     * @param point1    Point to compare against
     * @param point2    Point to test
     * @return true if  point2 is within the bounding box created around point1 +/- proximity
     */
    public boolean pointsAreProximal(double proximity, MapPoint point1, MapPoint point2)
    {
        return (isPointWithinArea(point2,
                point1.getLatitude() - proximity,
                point1.getLongitude() - proximity,
                point1.getLatitude() + proximity,
                point1.getLongitude() + proximity));
    }

    /**
     * Determine if two points are proximal to one another.  Basically, test to see if point2 lies within
     * the lat/lon bounding box created by adding and subtracting the proximity from point1 lat/lon position.
     *
     * @param proximity Proximity tolerance
     * @param point1    Point to compare against
     * @param point2    Point to test
     * @return true if  point2 is within the bounding box created around point1 +/- proximity
     */
    public boolean pointsAreProximal(double proximity, TrackPoint point1, TrackPoint point2)
    {
        return (isPointWithinArea(point2,
                point1.latitude - proximity,
                point1.longitude - proximity,
                point1.latitude + proximity,
                point1.longitude + proximity));
    }

    /**
     * Determine if a given MapPoint lies within the specified lat/lon bounding box.
     *
     * @param point  point to test
     * @param minLat Latitude of minimum boundary
     * @param minLon Longitude of minimum boundary
     * @param maxLat Latitude of maximum boundary
     * @param maxLon Longitude of maximum boundary
     * @return true of the point falls within the bounding box
     */
    public boolean isPointWithinArea(MapPoint point, double minLat, double minLon, double maxLat, double maxLon)
    {
        return (point.getLatitude() >= minLat &&
                point.getLatitude() <= maxLat &&
                point.getLongitude() >= minLon &&
                point.getLongitude() <= maxLon);
    }

    /**
     * Determine if a given TrackPoint lies within the specified lat/lon bounding box.
     *
     * @param point  point to test
     * @param minLat Latitude of minimum boundary
     * @param minLon Longitude of minimum boundary
     * @param maxLat Latitude of maximum boundary
     * @param maxLon Longitude of maximum boundary
     * @return true of the point falls within the bounding box
     */
    public boolean isPointWithinArea(TrackPoint point, double minLat, double minLon, double maxLat, double maxLon)
    {
        return (point.latitude >= minLat &&
                point.latitude <= maxLat &&
                point.longitude >= minLon &&
                point.longitude <= maxLon);
    }

    /**
     * Calculate a fair map point allocation for a set of buckets imposing a given total limit across all buckets.
     * <p/>
     * The basic algorithm here is to take the limit given and evenly divide the map
     * point limit among the buckets. If any bucket has fewer points to display than
     * are allowed, give the overage to the other buckets.
     *
     * @param mapPointLimit        The total map point limit
     * @param mapPointCounts       The number of potentially visible map points for each bucket
     * @param allocation           The initial allocation for each bucket
     * @param totalPossiblyVisible The total number of map points that could be shown
     * @param allocations          Array of allocations to be filled out
     */
    public void calculateAllocations(int mapPointLimit, int[] mapPointCounts, int allocation, int totalPossiblyVisible, int[] allocations)
    {
        final int bucketCount = allocations.length;

        if (logger.isDebugEnabled())
        {
            logger.debug("   mapPointLimit={} count={} allocation={}", mapPointLimit, bucketCount, allocation);
        }

        // If we have more points to show than our limit, then go back through the buckets looking for those
        // that have less then their allocation.  We will take that excess and apply it to the buckets that
        // have more data to show than their allocation.

        if (totalPossiblyVisible > mapPointLimit)
        {
            int numWithMoreToShow = 0; // How many buckets have more data to show thane their allocation
            int excess = 0; // Number of points that can be given to other buckets

            for (int i = 0; i < bucketCount; i++)
            {
                final int count = mapPointCounts[i];

                if (count == 0)
                {
                    allocations[i] = 0;
                    continue; // buckets with no data are not processed in the allocations
                }

                if (count < allocation)
                {
                    excess += (allocation - count);
                    allocations[i] = count;
                } else
                {
                    allocations[i] = allocation;

                    if (count > allocation)
                    {
                        numWithMoreToShow += 1; // This one has more to show
                    }
                }
            }

            // While we have excess capacity, dole it out evenly
            while (excess > 0 && numWithMoreToShow > 0)
            {
                int excessAllocation = excess / numWithMoreToShow;
                if (excessAllocation == 0) excessAllocation = excess; // Avoid rounding down to 0

                if (logger.isDebugEnabled())
                {
                    logger.debug("     numWithMTS={} excess={} ea={}", numWithMoreToShow, excess, excessAllocation);
                }

                numWithMoreToShow = 0; // reset for this iteration

                for (int i = 0; i < bucketCount && excess > 0; i++)
                {
                    final int count = mapPointCounts[i];

                    if (count == 0) continue; // buckets with no data are not processed in the allocations

                    if (allocations[i] < count)
                    {  // This is one that wants to show more
                        if (allocations[i] + excessAllocation > count)
                        { // Doesn't need all of it
                            int diff = count - allocations[i];
                            allocations[i] = count;  // Same as += diff, but faster
                            excess -= diff;
                        } else
                        {
                            allocations[i] += excessAllocation; // Give it the whole excess allocation
                            excess -= excessAllocation;

                            if (allocations[i] < count)
                            {
                                numWithMoreToShow += 1; // This one still has more to show
                            }
                        }
                    }
                }
            }
        } else
        {
            // It's Christmas and everybody gets what they want
            System.arraycopy(mapPointCounts, 0, allocations, 0, mapPointCounts.length);
        }
    }

    /**
     * Find the map data (RDMs) within a given sector boundary.  This only includes data from Map Objects that
     * use styled tracks.
     *
     * @param bounds Bounds within which to search
     * @return map data found
     */
    public IRendererSearchResult getViewableMessagesForArea(Sector bounds)
    {
        if (bounds == null) return null;

        // There's no easy way to get the count that we really want.  Mainly due to the fact that the CMOs are
        // not persisted on every property change, so one could be marked as invisible and the database wouldn't
        // know it.  So, we loop over each CMO and count the results we would get for each.  That will give us the
        // total count.  We then go back through and select points up until we hit the MAX limit.  Given this,
        // we can construct the table model with the total count and with the selected messages.

        long mapPointCount = 0;
        int mapObjectCount = 0;
        long countForThisObject;

        final var mapObjects = mapObjectManager.getMapObjects();

        for (IMapObject mapObject : mapObjects)
        {
            if (mapObject.canHaveStyledTrackData() && isMapDataViewable(mapObject))
            {
                countForThisObject = countMapData(mapObject, bounds);
                if (countForThisObject > 0)
                {
                    mapPointCount += countForThisObject;
                    mapObjectCount++;
                }
            }
        }

        // Figure out the maximum number of points we can take for each map object without going over our target.
        // We may want to replace this with a weighted average approach in the future.
        if (mapObjectCount == 0) logger.warn("No map objects found in map area");

        int maxPointsPerObject = mapObjectCount == 0 ? MAX_SUMMARY_POINTS : MAX_SUMMARY_POINTS / mapObjectCount;

        final Map<IMapObject, List<IRaptorDataMessage>> dataMap = new HashMap<>();

        for (IMapObject mapObject : mapObjects)
        {
            if (mapObject.canHaveStyledTrackData() && isMapDataViewable(mapObject))
            {
                List<IRaptorDataMessage> messages = getMapData(mapObject, bounds, maxPointsPerObject);

                if (!messages.isEmpty()) dataMap.put(mapObject, messages);
            }
        }

        return new RendererSearchResult(mapPointCount, bounds, dataMap);
    }

    /**
     * Count the number of visible messages associated with the map object in the given region.
     *
     * @param mapObject map object of interest
     * @param bounds    Region bounds
     * @return number of visible messages in region
     */
    private long countMapData(IMapObject mapObject, Sector bounds)
    {
        final ITrackStylingContext tsc = smartTrackManager.getTrackStylingContext(mapObject);
        final long now = System.currentTimeMillis();

        long count;

        if (tsc.hasTimeThreshold() && !mapObject.inPlaybackMode())
        {
            // This map object has a tracks time threshold, so use that to limit the map points we select.

            final long threshold = now - tsc.getTimeThreshold();

            count = rdmPersist.countLatLongTimeThreshold(mapObject, bounds, threshold);
        } else
        {
            count = rdmPersist.countLatLong(mapObject, bounds);
        }

        return count;
    }

    /**
     * Get the visible messages associated with the map object in the given region up to the max messages specified.
     *
     * @param mapObject   map object of interest
     * @param bounds      Region bounds
     * @param maxMessages The maximum number of messages to return
     * @return visible messages in region
     */
    public List<IRaptorDataMessage> getMapData(IMapObject mapObject, Sector bounds, int maxMessages)
    {
        final ITrackStylingContext tsc = smartTrackManager.getTrackStylingContext(mapObject);

        List<IRaptorDataMessage> messages;

        final long now = System.currentTimeMillis();

        if (tsc.hasTimeThreshold() && !mapObject.inPlaybackMode())
        {
            // This map object has a tracks time threshold, so use that to limit the map points we select.

            final long threshold = now - tsc.getTimeThreshold();

            messages = rdmPersist.searchLatLongTimeThreshold(mapObject, bounds, threshold, maxMessages, IRDMPersist.SortOrder.ASCENDING);
        } else
        {
            messages = rdmPersist.searchLatLong(mapObject, bounds, maxMessages);
        }

        return messages;
    }
}
