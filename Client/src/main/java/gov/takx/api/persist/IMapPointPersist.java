package gov.takx.api.persist;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.mapdata.MapPoint;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * Query helpers for MapPoint objects.
 */
public interface IMapPointPersist
{

    /**
     * Query for all map points associated with the given MapObject (which
     * represents a unique instance of a map object generating RDMs).
     *
     * @param mo The MapObject for the map object of interest
     * @return list of map points
     */
    List<MapPoint> searchMapObject(@Nonnull IMapObject mo);

    /**
     * Query for the MapPoints that have the given RDM Ids.
     *
     * @param rdmIds Collection of RDM Ids to query against
     * @return list of map points
     * @since 5.2 - signature changed
     */
    List<MapPoint> getByIds(@Nonnull Collection<Integer> rdmIds);

    /**
     * Query the database for map points that have a <i>relevant</i> time greather than the threshold.
     * The relevant time is device time, if it is set, otherwise receive time.
     *
     * @param mo        The MapObject for the map object of interest
     * @param threshold The time threshold, only messages with a relevant time &gt;= to this value will be returned
     * @return list of MapPoint objects representing RDMs found within the search area
     */
    List<MapPoint> searchTimeThreshold(@Nonnull IMapObject mo, long threshold);

    /**
     * Query the database for map points within the bounding box of the minLoc and maxLoc that have a <i>relevant</i>
     * time greather than the threshold.  The relevant time is device time, if it is set, otherwise receive time.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     *
     * @param mo        The MapObject for the map object of interest
     * @param minLoc    minimum location of search boundary
     * @param maxLoc    maximum location of search boundary
     * @param threshold The time threshold, only messages with a relevant time &gt;= to this value will be returned
     * @return list of MapPoint objects representing RDMs found within the search area
     */
    List<MapPoint> searchLatLongTimeThreshold(@Nonnull IMapObject mo, @Nonnull ILocation minLoc, @Nonnull ILocation maxLoc, long threshold);

    /**
     * Query the database for map points within the bounding box of the minLoc and maxLoc.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     *
     * @param mo     The MapObject for the map object of interest
     * @param minLoc minimum location of search boundary
     * @param maxLoc maximum location of search boundary
     * @return list of MapPoint objects representing RDMs found within the search area
     */
    List<MapPoint> searchLatLong(@Nonnull IMapObject mo, @Nonnull ILocation minLoc, @Nonnull ILocation maxLoc);

    /**
     * Query the database for map points with the bounding box of the minLoc and maxLoc and also
     * the timebox of minDT and maxDT. The query will "normalize" the values and push them to correct
     * upper or lower boundary (so you don't need to worry about the min actually being smaller than
     * the max).
     *
     * @param mo     The MapObject for the map object of interest, may be {@code null} to indicate all object types
     * @param minLoc min location in range
     * @param maxLoc max location in range
     * @param minDT  min receive date in range
     * @param maxDT  max receive date in range
     * @return list of MapPoint objects representing RDMs found within the search area
     */
    List<MapPoint> searchLatLongDate(@Nonnull IMapObject mo, @Nonnull ILocation minLoc, @Nonnull ILocation maxLoc, long minDT, long maxDT);

    /**
     * Query the database for map points with the bounding receive timebox of the minDT and maxDT.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     *
     * @param mo    The MapObject for the map object of interest, may be {@code null} to indicate all object types
     * @param minDT min receive date in range
     * @param maxDT max receive date in range
     * @return list of MapPoint objects representing RDMs found within the search area
     */
    List<MapPoint> searchReceiveDate(@Nonnull IMapObject mo, long minDT, long maxDT);
}
