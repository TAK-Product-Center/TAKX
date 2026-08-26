package gov.takx.platform.query;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IExtendedQueryConstraints;
import gov.takx.api.query.IExtendedQueryService;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.QueryExecutionException;
import gov.takx.api.query.ShapeConstraint;
import jakarta.annotation.Nonnull;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An in-memory message (RDM) store that can be queried like a database using {@link IQueryConstraints} produced
 * using the {@link IQueryService}.
 * <p>
 * Currently, this store uses a single index (by latitude) to make location/shape constraint tests faster.  In the
 * future, additional indices will be configurable.
 *
 * @since 2.0
 */
@SuppressWarnings("unused") // Called from plugins
@Experimental
public class MessageStore<Self extends MessageStore, RDM extends IRaptorDataMessage> implements IExtendedQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final boolean locationRequired;

    private final double[] newPointCoords = new double[2];

    /**
     * Organize our messages by MO Id.
     */
    @SuppressWarnings("UnstableApiUsage")
    private final ListMultimap<Integer, RDM> messagesPerMapObject = MultimapBuilder.treeKeys().arrayListValues().build();

    /**
     * Track the latest receive time of any message in the store.  This optimizes what would be a sort of the entire
     * store on receive time just to obtain the newest one.
     */
    private long latestReceiveTime = 0L;

    /**
     * constructor.  By default, messages that have no location (where {@link IRaptorDataMessage#hasLocation()}
     * is {@code false}) will not be added to the message store
     */
    public MessageStore()
    {
        this(true);
    }

    /**
     * Construct a message store.
     *
     * @param locationRequired Pass {@code true} if a message must have a location in order to be stored
     */
    public MessageStore(boolean locationRequired)
    {
        this.locationRequired = locationRequired;
    }

    /**
     * @return The latest receive time of all RDMs within this store
     */
    public long getLatestReceiveTime()
    {
        return latestReceiveTime;
    }

    /**
     * Clear out all the data in this store.
     */
    public synchronized void clear()
    {
        messagesPerMapObject.clear();
    }

    /**
     * @return {@code true} if a location is required for RDMs in this store
     */
    public boolean isLocationRequired()
    {
        return locationRequired;
    }

    /**
     * Load given messages into the store.
     */
    public synchronized void loadMessages(Iterable<RDM> messages)
    {
        for (RDM rdm : messages)
        {
            if (!locationRequired || rdm.hasLocation())
            {
                internalAddMessage(rdm);
            }
        }
    }

    /**
     * Add a single message to the store.
     *
     * @param rdm Message to add
     * @throws IllegalArgumentException If RDM is not of the configured message type
     */
    public void addMessage(@Nonnull RDM rdm) throws IllegalArgumentException
    {
        // Only messages with locations get added to the store, if configured
        if (!locationRequired || rdm.hasLocation())
        {
            internalAddMessage(rdm);
        }
    }

    /**
     * Get the list of RDMs associated with the given Map Object Id.  This method returns data from directly within
     * our message store.  It is up to the caller to properly synchronize access to the data in the returned list
     * (typically, by synchronizing on this message store).
     *
     * @param moId Map Object Id
     * @return List of messages for the Map Object
     * @since 2.31
     */
    public List<RDM> getMessagesForMapObject(Integer moId)
    {
        return messagesPerMapObject.get(moId);
    }

    /**
     * Partition this message store into a collection of message stores where the contents of each partition
     * are determined by an identifier function.  The results are returned in a map from the key returned by
     * the identifier function to a new MessageStore instance.
     * <p>
     * Note that this operation uses a parallel stream to perform the partitioning.  You need to ensure that the
     * {@code identifier} function can be executed properly in parallel.
     *
     * @param identifier Function taking an RDM and returning the partition id, return {@link Optional#empty()} if the
     *                   rdm shouldn't be included in any partition
     * @param <T>        Partition identifier type
     * @return Map from T to MessageStore containing all messages whose identifier are the key
     */
    public synchronized <T> Map<T, Self> partition(Function<RDM, Optional<T>> identifier)
    {
        final Map<T, Self> partitions = new ConcurrentHashMap<>();

        //noinspection unchecked
        all().parallel().forEach(rdm -> identifier.apply(rdm)
                .ifPresent(pid -> partitions
                        .computeIfAbsent(pid, (k) -> createPartitionStore())
                        .internalAddMessage(rdm)));

        return partitions;
    }

    /**
     * Creates and returns a new MessageStore instance, and adds the values from the calling MessageStore.
     * <p>
     * This method's behaviour is similar to the {@link MessageStore#partition(Function)} method, except a Map is not
     * returned, and an identifier is not employed to filter and sort the messages.
     *
     * @param constraints The constraints used to filter the RDMs in the returned MessageStore.
     * @return A new MessageStore containing all the messages in the calling MessageStore instance.
     */
    public synchronized <T> Self copyMessageStore(IQueryConstraints constraints)
    {
        final Self newMessageStore = createPartitionStore();
        //noinspection unchecked
        queryStream(constraints).parallel().forEach(newMessageStore::internalAddMessage);

        return newMessageStore;
    }

    /**
     * @return A new message store for a partition.  Subclasses should override as needed.
     * @see #partition(Function)
     */
    protected Self createPartitionStore()
    {
        //noinspection unchecked
        return (Self) new MessageStore(locationRequired);
    }

    /**
     * Add a message to the store with no conditional checks and no synchronization.
     * Caller must be properly synchronized.
     *
     * @param rdm Message to add
     */
    protected void internalAddMessage(RDM rdm)
    {
        final double latitude = rdm.getLatitude();
        final long receiveTime = rdm.getReceiveTime();

        synchronized (this)
        {
            messagesPerMapObject.put(rdm.getMapObject().getId(), rdm);

            // Update our latest receive time
            latestReceiveTime = Math.max(latestReceiveTime, receiveTime);
        }
    }

    /**
     * This method is called prior to actual use of any of this message store's data.  This implementation does
     * nothing.  Derived classes can perform any operations they need to ensure the data is "ready" to use.
     */
    protected void usePreCheck()
    {
        // Nothing to do
    }

    /**
     * @return An iterable over all the messages in this store
     */
    public Stream<RDM> all()
    {
        usePreCheck();
        synchronized (this)
        {
            return messagesPerMapObject.values().stream();
        }
    }

    /**
     * @return The number of messages in this store
     */
    public int size()
    {
        usePreCheck();
        synchronized (this)
        {
            return messagesPerMapObject.values().size();
        }
    }

    @Override
    public IExtendedQueryConstraints createEmptyConstraints()
    {
        return new QueryConstraints();
    }

    @Override
    public boolean matches(IRaptorDataMessage rdm, IQueryConstraints constraints)
    {
        return new MessageMatcher(constraints).matches(rdm);
    }

    @Override
    public Iterator<IRaptorDataMessage> iterator(IQueryConstraints constraints) throws QueryExecutionException
    {
        return query(constraints).iterator();
    }

    @Override
    public List<Object[]> queryForFields(IQueryConstraints constraints, FieldSelector fieldSelector) throws IllegalArgumentException
    {
        if (fieldSelector.hasAggregateFields())
        {
            throw new IllegalArgumentException("Aggregate field selectors may not be used with FieldExtractor");
        }

        usePreCheck();
        synchronized (this)
        {
            return FieldExtractor.getFields(getQueryStream(constraints), fieldSelector);
        }
    }

    @Override
    public List queryForFieldsAsBean(IQueryConstraints constraints, FieldSelector fieldSelector, Class dtoType)
    {
        if (fieldSelector.hasAggregateFields())
        {
            throw new IllegalArgumentException("Aggregate field selectors may not be used with FieldExtractor");
        }

        usePreCheck();
        synchronized (this)
        {
            return FieldExtractor.getFieldsAsBean(getQueryStream(constraints), fieldSelector, Transformers.aliasToBean(dtoType));
        }
    }

    @Override
    public void closeIterator(Iterator iterator)
    {
        // Nothing to do on in-memory iterators
    }

    @Override
    public List<IRaptorDataMessage> query(final IQueryConstraints constraints)
    {
        usePreCheck();
        synchronized (this)
        {
            return getQueryStream(constraints).collect(Collectors.toList());
        }
    }

    @Override
    public List<IRaptorDataMessage> query(IQueryConstraints constraints, boolean prefetch) throws QueryExecutionException
    {
        return query(constraints);
    }

    /**
     * Query for the RDMs that match the given constraints, returning a Stream.
     *
     * @param constraints Query constraints to match
     * @return Stream of matching RDMs
     * @since 2.2
     */
    public Stream<RDM> queryStream(final IQueryConstraints constraints)
    {
        usePreCheck();
        synchronized (this)
        {
            return getQueryStream(constraints);
        }
    }

    @Override
    public List<Map<String, Object>> queryForFieldsAsMap(IQueryConstraints constraints, FieldSelector selector)
    {
        usePreCheck();
        synchronized (this)
        {
            final Stream<RDM> matches = getQueryStream(constraints);

            // Handle grouping, if specified
            return constraints.isGrouping()
                    ? new GroupingHandler(constraints, selector).performGrouping(matches)
                    : FieldExtractor.getFieldsAsMap(matches, selector);
        }
    }

    @Override
    public long count(final IQueryConstraints constraints)
    {
        usePreCheck();
        synchronized (this)
        {
            return (int) getMatchingStream(constraints).count();
        }
    }

    /**
     * Query for the IDs of the RDMs that match the given constraints.
     *
     * @param constraints Query constraints to match
     * @return Set of ids of matching RDMs
     */
    public Set<Integer> queryForIds(final IQueryConstraints constraints)
    {
        usePreCheck();
        synchronized (this)
        {
            return getQueryStream(constraints)
                    .map(IRaptorMessage::getId)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Construct a stream that incorporates the matching criteria, sorting, and limit/offset from the
     * given constraints.  The returned stream is over the matching messages.
     * <p>
     * Note: <em>If grouping is enabled on the constraints, then no limit/offset is applied here.</em>
     *
     * @param constraints Constraints to process
     * @return Iterable of matching messages properly ordered and limited
     */
    private Stream<RDM> getQueryStream(IQueryConstraints constraints)
    {
        // Get the initial matching messages
        Stream<RDM> matches = getMatchingStream(constraints);

        // Sort them if requested
        if (constraints.isSorted()) matches = matches.sorted(getSortComparator(constraints));

        // And, if we're not grouping, apply any limit/offset specified
        final boolean usingLimits = !constraints.isGrouping();
        final int skip = constraints.getStartMessageCount();
        final int limit = constraints.getMaxMessageCount();

        if (usingLimits && skip > 0) matches = matches.skip(skip);
        if (usingLimits && limit > 0) matches = matches.limit(limit);

        return matches;
    }

    /**
     * Create a Stream that will provide messages from this store that match the given constraints.
     *
     * @param constraints Query constraints on which to match
     * @return stream of matching messages
     * @see IQueryService#matches(IRaptorDataMessage, IQueryConstraints)
     */
    private Stream<RDM> getMatchingStream(IQueryConstraints constraints)
    {
        final BoundingBox boundingBox = getBoundingBox(constraints);

        Stream<RDM> messageStream = all();

        // If we have a shape constraint, pre-filter the messages before matching constraints
        if (boundingBox != null)
        {
            messageStream = messageStream.filter(rdm -> boundingBox.contains(rdm.getLatitude(), rdm.getLongitude()));
        }

        final MessageMatcher messageMatcher = new MessageMatcher(constraints);

        return messageStream.filter(messageMatcher::matches);
    }

    /**
     * Get the bounding box for any shape constraint.  Currently, only rectangle and polygons are supported.
     *
     * @param constraints Constraints to process
     * @return Extracted bounding box if an appropriate shape constraint was found, {@code null} otherwise
     * @since 2.30
     */
    private BoundingBox getBoundingBox(IQueryConstraints constraints)
    {
        for (IConstraint c : constraints.getShapeConstraints())
        {
            final ShapeConstraint sc = (ShapeConstraint) c;
            final ShapeConstraint.ShapeType shapeType = sc.getShapeType();

            if (shapeType == ShapeConstraint.ShapeType.rectangle)
            {
                final ILocation minCorner = sc.getMinCorner();
                final ILocation maxCorner = sc.getMaxCorner();

                return new BoundingBox(minCorner.getLatitude(), maxCorner.getLatitude(), minCorner.getLongitude(), maxCorner.getLongitude());
            } else if (shapeType == ShapeConstraint.ShapeType.polygon)
            {
                return getPolygonBoundingBox(sc.getVertices());
            }
        }

        return null;
    }

    /**
     * Construct the bounding box from a polygon's vertices.
     *
     * @param vertices Vertices from polygon
     * @return Bounding box covering the polygon coordinates
     */
    private BoundingBox getPolygonBoundingBox(ILocation[] vertices)
    {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        for (ILocation vertex : vertices)
        {
            final double latitude = vertex.getLatitude();
            if (latitude < minLat) minLat = latitude;
            if (latitude > maxLat) maxLat = latitude;

            final double longitude = vertex.getLongitude();
            if (longitude < minLon) minLon = longitude;
            if (longitude > maxLon) maxLon = longitude;
        }

        return new BoundingBox(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Remove all the data from the given Map Object.
     *
     * @param mapObject map object that was deleted
     * @return the messages that were deleted
     */
    public synchronized List<RDM> removeDataForMapObject(IMapObject mapObject)
    {
        final List<RDM> removed = messagesPerMapObject.removeAll(mapObject.getId());

        // If any messages have been removed, we need to fully recalculate the latest receive time
        if (!removed.isEmpty())
        {
            latestReceiveTime = messagesPerMapObject.values().stream()
                    .mapToLong(IRaptorDataMessage::getReceiveTime)
                    .max()
                    .orElse(0);
        }

        return removed;
    }

    /**
     * Obtain a comparator to use according to the sorting criteria specified in the query constraints.
     *
     * @param constraints Query constraints to process
     * @return Comparator to use
     */
    private Comparator<IRaptorDataMessage> getSortComparator(IQueryConstraints constraints)
    {
        final boolean ascending = constraints.getSortOrder() == IQueryConstraints.SortOrder.ASCENDING;

        switch (constraints.getSortOn())
        {
            case DEVICE_DATE:
                return ascending ? BY_DEVICE_TIME : BY_DEVICE_TIME_REVERSE;
            case RECEIVE_DATE:
                return ascending ? BY_RECEIVE_TIME : BY_RECEIVE_TIME_REVERSE;
            case RELEVANT_TIME:
                return BY_RELEVANT_TIME;
            case LOCATION:
                return ascending ? BY_LOCATION : BY_LOCATION_REVERSE;
            case FIELD:
                Comparator<IRaptorDataMessage> comparator = getSortFieldComparator(constraints);
                return ascending ? comparator : comparator.reversed();
            default:
                // will only happen if someone creates a new SortOn, or explicitly sets sortOn to null
                throw new IllegalArgumentException("Unsupported SortOn: " + constraints.getSortOn());
        }
    }

    /**
     * If we are sorting on a field, this returns the comparator used for sorting on that field
     *
     * @param constraints The constraints to get the sort parameters from
     * @return The comparator
     */
    private Comparator<IRaptorDataMessage> getSortFieldComparator(IQueryConstraints constraints)
    {
        final ASimpleFieldSelector sortOnFieldSelector = constraints.getSortOnFieldSelector();
        final Function<IRaptorDataMessage, Comparable> fieldExtractor;

        if (sortOnFieldSelector.isCoreField())
        {
            final CoreField field = ((CoreFieldSelector) sortOnFieldSelector).getCoreField();
            fieldExtractor = rdm -> (Comparable) FieldExtractor.getCoreField(rdm, field);
        } else
        { // is a structure field
            IDataField sortDataField = constraints.getMessageStructureMap()
                    .get(sortOnFieldSelector.getStructureName()).stream()
                    .map(structure -> structure.getField(sortOnFieldSelector.getFieldName()))
                    .findFirst()
                    .orElse(null);

            if (sortDataField != null && sortDataField.isSortable())
            {
                fieldExtractor = rdm -> (Comparable) rdm.getField(sortDataField.getFieldName());
            } else
            {
                throw new IllegalArgumentException("Unsupported sort field: " + sortOnFieldSelector.getFieldName());
            }
        }

        return Comparator.comparing(fieldExtractor, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public synchronized String toString()
    {
        return "MessageStore{" +
                "size=" + messagesPerMapObject.size() +
                '}';
    }

    /**
     * Bounding box (min/max corner).
     */
    private static final class BoundingBox
    {
        private final double minLatitude;
        private final double maxLatitude;
        private final double minLongitude;
        private final double maxLongitude;

        private BoundingBox(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude)
        {
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
        }

        /**
         * Determine if the given latitude and longitude aer within the boundaries of this box.
         *
         * @param latitude  Latitude to test
         * @param longitude Longitude to test
         * @return {@code true} if the coordinates are within this bounding box
         * @since 2.30
         */
        private boolean contains(double latitude, double longitude)
        {
            return latitude >= minLatitude && latitude <= maxLatitude
                    && longitude >= minLongitude && longitude <= maxLongitude;
        }
    }

    /**
     * Comparator to order RDMs by receive time.
     */
    private static final Comparator<IRaptorDataMessage> BY_RECEIVE_TIME = Comparator.comparingLong(IRaptorDataMessage::getReceiveTime).thenComparingInt(IRaptorMessage::getIdAsInt);

    /**
     * Comparator to order RDMs by REVERSE receive time.
     */
    private static final Comparator<IRaptorDataMessage> BY_RECEIVE_TIME_REVERSE = BY_RECEIVE_TIME.reversed();

    /**
     * Comparator to order RDMs by device time.
     */
    private static final Comparator<IRaptorDataMessage> BY_DEVICE_TIME = Comparator.comparingLong(IRaptorDataMessage::getTime).thenComparingInt(IRaptorMessage::getIdAsInt);

    /**
     * Comparator to order RDMs by REVERSE device time.
     */
    private static final Comparator<IRaptorDataMessage> BY_DEVICE_TIME_REVERSE = BY_DEVICE_TIME.reversed();

    /**
     * Comparator to order RDMs by relevant time.
     */
    private static final Comparator<IRaptorDataMessage> BY_RELEVANT_TIME = Comparator.comparingLong(IRaptorDataMessage::getTime).thenComparingInt(IRaptorMessage::getIdAsInt);

    /**
     * Comparator to order RDMs by REVERSE relevant time.
     */
    private static final Comparator<IRaptorDataMessage> BY_RELEVANT_TIME_REVERSE = BY_RELEVANT_TIME.reversed();

    /**
     * Comparator to order RDMs by latitude.
     */
    private static final Comparator<IRaptorDataMessage> BY_LATITUDE = Comparator.comparingDouble(IRaptorDataMessage::getLatitude);

    /**
     * Comparator to order RDMs by longitude.
     */
    private static final Comparator<IRaptorDataMessage> BY_LONGITUDE = Comparator.comparingDouble(IRaptorDataMessage::getLongitude);

    /**
     * Comparator to order RDMs by location (latitude then longitude).
     */
    private static final Comparator<IRaptorDataMessage> BY_LOCATION = BY_LATITUDE.thenComparing(BY_LONGITUDE);

    /**
     * Comparator to order RDMs by REVERSE location.
     */
    private static final Comparator<IRaptorDataMessage> BY_LOCATION_REVERSE = BY_LOCATION.reversed();
}
