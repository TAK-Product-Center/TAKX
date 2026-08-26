package gov.takx.api.persist;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Persistence service helper for {@link IRaptorDataMessage} objects. A set of search methods are provided to locate
 * RDMs using various criteria.
 */
@NoPluginImplementations
public interface IRDMPersist
{
    enum SortOrder
    {
        ASCENDING,
        DESCENDING,
        NONE
    }

    /**
     * Query for all {@link IRaptorDataMessage} objects associated with the given MapObject.
     *
     * @param mo The map object of interest
     * @return list of messages
     */
    List<IRaptorDataMessage> searchMapObject(@Nonnull IMapObject mo);

    /**
     * Query for all {@link IRaptorDataMessage} objects associated with the given MapObject.
     *
     * @param mo      The map object of interest
     * @param maxRows The maximum number of messages to return, may be {@code null} to indicate no maximum
     * @return list of messages
     */
    List<IRaptorDataMessage> searchMapObject(@Nonnull IMapObject mo, @Nullable Integer maxRows);

    /**
     * Query the database for {@link IRaptorDataMessage}s within the region bounds.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated are included
     *
     * @param mo     The map object of interest
     * @param bounds sector describing the search area
     * @return list of {@link IRaptorDataMessage}s found within the search area
     */
    List<IRaptorDataMessage> searchLatLong(@Nullable IMapObject mo, @Nonnull Sector bounds);

    /**
     * Query the database for {@link IRaptorDataMessage}s within the region bounds.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo      The map object of interest
     * @param bounds  sector describing the search area
     * @param maxRows The maximum number of messages to return, may be {@code null} to indicate no maximum
     * @return list of {@link IRaptorDataMessage}s found within the search area
     */
    List<IRaptorDataMessage> searchLatLong(@Nullable IMapObject mo, @Nonnull Sector bounds, @Nullable Integer maxRows);

    /**
     * Find the most recent RDM that also has a location for the supplied MO.
     *
     * @param mapObject Map object of interest
     * @return Most recent RDM for the given map object with a location, or {@code null} if none found
     */
    @Nullable
    IRaptorDataMessage findMostRecentWithLocation(@Nonnull IMapObject mapObject);

    /**
     * Determine if the given map object has any RDMs received after the given cutoff time.
     *
     * @param mapObject MapObject of interest
     * @param time      Cutoff time
     * @return {@code true} if there are RDMs with a receiveTime greater than the cutoff time
     */
    boolean mapObjectHasNewerMessages(@Nonnull IMapObject mapObject, long time);

    /**
     * Query the database for {@link IRaptorDataMessage}s with the bounding receive timebox of the minDT and maxDT.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     *
     * @param mo    The map object of interest, may be {@code null} to indicate all object types
     * @param minDT min receive date in range
     * @param maxDT max receive date in range
     * @return list of messages
     */
    List<IRaptorDataMessage> searchReceiveDate(@Nullable IMapObject mo, long minDT, long maxDT);

    /**
     * Query the database for {@link IRaptorDataMessage}s with the bounding receive timebox of the minDT and maxDT.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     * If sort order is specified as {@code null} or {@link SortOrder#NONE}, then no ordering is applied to the results.
     *
     * @param mo        The map object of interest, may be {@code null} to indicate all object types
     * @param minDT     min receive date in range
     * @param maxDT     max receive date in range
     * @param maxRows   The maximum number of messages to return, may be {@code null} to indicate no maximum
     * @param sortOrder Sort ordering for results, may be {@code null} which is the same as {@link SortOrder#NONE}
     * @return list of messages
     */
    List<IRaptorDataMessage> searchReceiveDate(@Nullable IMapObject mo, long minDT, long maxDT,
                                               @Nullable Integer maxRows, @Nullable SortOrder sortOrder);

    /**
     * Query the database for {@link IRaptorDataMessage}s with the bounding device timebox of the minDT and maxDT.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     * The results are unordered.
     *
     * @param mo    The map object of interest, may be {@code null} to indicate all object types
     * @param minDT min receive date in range
     * @param maxDT max receive date in range
     * @return list of messages
     */
    List<IRaptorDataMessage> searchDeviceDate(@Nullable IMapObject mo, long minDT, long maxDT);

    /**
     * Query the database for {@link IRaptorDataMessage}s with the bounding device timebox of the minDT and maxDT.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     * If sort order is specified as {@code null} or {@link SortOrder#NONE}, then no ordering is applied to the results.
     *
     * @param mo        The map object of interest, may be {@code null} to indicate all object types
     * @param minDT     min receive date in range
     * @param maxDT     max receive date in range
     * @param sortOrder Sort ordering for results, may be {@code null} which is the same as {@link SortOrder#NONE}
     * @return list of messages
     */
    List<IRaptorDataMessage> searchDeviceDate(@Nullable IMapObject mo, long minDT, long maxDT, @Nullable SortOrder sortOrder);

    /**
     * Query the database for {@link IRaptorDataMessage}s with the region bounds and also the time box of minDT and maxDT.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo     The map object of interest, may be {@code null} to indicate all object types
     * @param bounds sector describing the search area
     * @param minDT  min receive date in range
     * @param maxDT  max receive date in range
     * @return list of messages
     */
    List<IRaptorDataMessage> searchLatLongDate(@Nullable IMapObject mo, @Nonnull Sector bounds, long minDT, long maxDT);

    /**
     * Query the database for {@link IRaptorDataMessage}s within the given sector bounds and also the time box of minDT and maxDT.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo      The map object of interest, may be {@code null} to indicate all object types
     * @param bounds  sector describing the search area
     * @param minDT   min receive date in range
     * @param maxDT   max receive date in range
     * @param maxRows The maximum number of messages to return, may be {@code null} to indicate no maximum
     * @return list of messages
     */
    List<IRaptorDataMessage> searchLatLongDate(@Nullable IMapObject mo, @Nonnull Sector bounds, long minDT, long maxDT,
                                               @Nullable Integer maxRows);

    /**
     * Query the {@link IRaptorDataMessage}s within the region bounds that have a <i>relevant</i>
     * time greater than the threshold.  The relevant time is device time, if it is set, otherwise receive time.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo        The map object of interest
     * @param bounds    sector describing the search area
     * @param threshold The time threshold, only messages with a relevant time &gt;= to this value will be returned
     * @param maxRows   The maximum number of messages to return, may be {@code null} to indicate no maximum
     * @param sortOrder The order in which messages are sorted. Messages will be sorted by relevant time.
     *                  Pass {@link SortOrder#NONE} to suppress sorting.
     * @return list of messages
     */
    List<IRaptorDataMessage> searchLatLongTimeThreshold(@Nonnull IMapObject mo, Sector bounds, long threshold, Integer maxRows, SortOrder sortOrder);

    /**
     * Search for a data message by UUID.
     *
     * @param uuid UUID to search for. May not be {@code null}.
     * @return The message with the requested UUID, or {@code null} if no such message is found.
     */
    @Nullable
    IRaptorDataMessage searchUUID(@Nonnull UUID uuid);

    /**
     * Does the database contain a data message with a given UUID?
     *
     * @param uuid UUID to search for. May not be {@code null}.
     * @return True if a message with the requested UUID exists
     */
    boolean hasUUID(@Nonnull UUID uuid);

    /**
     * Find a data message by ID.
     *
     * @param id ID of the desired message.
     * @return Message with the specified ID, or {@code null} if no such message exists.
     */
    @Nullable
    IRaptorDataMessage searchById(int id);

    /**
     * count all of the RDM's for a MapObject. If mo is {@code null} then it will return a count for all RDM's in the DB.
     *
     * @param mo Map object of interest
     * @return number of RDMs associated wit this map object
     */
    long countAll(@Nonnull IMapObject mo);

    /**
     * Count the number of {@link IRaptorDataMessage}s within the bounding box of the minLoc and maxLoc.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo     The map object of interest, may be {@code null} to indicate all object types
     * @param bounds sector describing the search area
     * @return count of matching messages
     */
    long countLatLong(@Nullable IMapObject mo, @Nonnull Sector bounds);

    /**
     * Count the number of {@link IRaptorDataMessage}s within the sector bounds.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo     The map object of interest, may be {@code null} to indicate all object types
     * @param bounds sector describing the search area
     * @param minDT  min receive date in range
     * @param maxDT  max receive date in range
     * @return count of matching messages
     */
    long countLatLongDate(@Nullable IMapObject mo, @Nonnull Sector bounds, long minDT, long maxDT);

    /**
     * Count the number of {@link IRaptorDataMessage}s within the sector bounds that have a <i>relevant</i>
     * time greater than the threshold.  The relevant time is device time, if it is set, otherwise receive time.
     * <p/>
     * NOTE: Only messages that have a location and the location is not manually generated and are renderable are included
     *
     * @param mo        The map object of interest
     * @param bounds    sector describing the search area
     * @param threshold The time threshold, only messages with a relevant time &gt;= to this value will be returned
     * @return list of MapPoint objects representing RDMs found within the search area
     */
    long countLatLongTimeThreshold(@Nonnull IMapObject mo, @Nonnull Sector bounds, long threshold);

    /**
     * Count the number of {@link IRaptorDataMessage}s within the receive time bounding box of the minDT and maxDT.
     * The query will "normalize" the values and push them to correct upper or lower boundary
     * (so you don't need to worry about the min actually being smaller than the max).
     *
     * @param mo    The map object of interest, may be {@code null} to indicate all object types
     * @param minDT min receive date in range
     * @param maxDT max receive date in range
     * @return count of matching messages
     */
    long countDate(@Nullable IMapObject mo, long minDT, long maxDT);

    /**
     * Find the first and last receive times of all RDMs.
     *
     * @return an array of two elements: [min_date, max_date].
     */
    long[] queryReceiveDateRange();

    /**
     * Find the first and last device time of all RDMs.
     *
     * @return an array of two elements: [min_date, max_date].
     */
    long[] queryDeviceDateRange();

    /**
     * Persist the RDM
     *
     * @param rdm RDM to persist
     */
    void persistRDM(@Nonnull IPrePersistRaptorDataMessage rdm);
}
