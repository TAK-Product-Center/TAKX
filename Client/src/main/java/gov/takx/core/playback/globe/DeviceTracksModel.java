package gov.takx.core.playback.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectPartialDelete;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.api.playback.globe.IDeviceTracksModelListener;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.InvalidFieldSelectorException;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.playback.PlaybackSystem;
import gov.takx.core.playback.TimeLocation;
import gov.takx.platform.playback.TimeBoundary;
import gov.takx.platform.playback.TimeSpanAdapter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This model handles all the track points for a given device (Map Object).  Two views of the data are
 * maintained: the complete set, and the subset that lies within the current playback time span.
 *
 * @since 1.47
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeviceTracksModel
        extends TimeSpanAdapter
        implements IDeviceTracksModel, IMapObjectControllerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ITimeSpan timeSpan;
    private ITimeLocation timeSpanStartKey;
    private ITimeLocation timeSpanEndKey;

    private final IMapObject device; // The device from which our tracks are derived

    private volatile boolean initialized;
    private final Object initializedLock = new Object(); // Controls access to "initialized" member

    private final NavigableSet<ITimeLocation> trackPoints = new ConcurrentSkipListSet<>(); // All of our track points

    private final List<IDeviceTracksModelListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    private PlaybackSystem playbackSystem;

    @Inject
    private IMapObjectManager mom;

    @Inject
    private ExecutorService executorService;

    public DeviceTracksModel(ITimeSpan timeSpan, final IMapObject device)
    {
        this.timeSpan = timeSpan;
        this.device = device;

        timeSpanChanged();
        timeSpan.addListener(this);
    }

    @PostConstruct
    private void postConstruct()
    {
        device.getController().addMapObjectControllerListener(this);

        // Populate the track points using a BG thread
        executorService.execute(() -> {
            Thread.currentThread().setName("Playback Track Loader: " + device.getName());
            initTrackPoints();
        });
    }

    @Override
    public boolean isInitialized()
    {
        return initialized;
    }

    @Override
    public void waitForInitialization(long timeout, TimeUnit unit) throws InterruptedException
    {
        synchronized (initializedLock)
        {
            if (initialized) return;

            initializedLock.wait(unit.toMillis(timeout));
        }
    }

    @Override
    public TimeBoundary getTimeBoundary()
    {
        return trackPoints.isEmpty() ? null : new TimeBoundary(getFirstTrackPoint().getTime(), getLastTrackPoint().getTime());
    }

    @Override
    public NavigableSet<ITimeLocation> getTrackPoints()
    {
        return trackPoints;
    }

    @Override
    public NavigableSet<ITimeLocation> getTrackPointsInPlayback()
    {
        return getTimeSpanSubset();
    }

    @Override
    public NavigableSet<ITimeLocation> getTrackPoints(long start, long end)
    {
        return trackPoints.subSet(TimeLocation.makeTimeKey(start), true,
                TimeLocation.makeTimeKey(end), true);
    }

    @Override
    public ITimeLocation getClosestTrackPointInPlayback(long time)
    {
        // We want the greatest element less than or equal to the given time, i.e., the floor of the key in the set
        return getTimeSpanSubset().floor(TimeLocation.makeTimeKey(time));
    }

    @Override
    public ITimeLocation getPreviousTrackPoint(long time)
    {
        return trackPoints.floor(TimeLocation.makeTimeKey(time));
    }

    @Override
    public ITimeLocation getNextTrackPointInPlayback(long time)
    {
        // We want the least element greater than the given time, i.e., the ceiling of the key+1 in the set
        return getTimeSpanSubset().ceiling(TimeLocation.makeTimeKey(time + 1));
    }

    @Override
    public ITimeLocation getPreviousTrackPointInPlayback(long time)
    {
        // We want the greatest element less than the given time, i.e., the floor of the key-1 in the set
        return getTimeSpanSubset().floor(TimeLocation.makeTimeKey(time - 1));
    }

    @Override
    public ITimeLocation getFirstTrackPoint()
    {
        return trackPoints.isEmpty() ? null : trackPoints.first();
    }

    @Override
    public ITimeLocation getLastTrackPoint()
    {
        return trackPoints.isEmpty() ? null : trackPoints.last();
    }

    @Override
    public void timeSpanChanged()
    {
        // Reconstruct our timespan subset keys
        timeSpanStartKey = TimeLocation.makeTimeKey(timeSpan.getStartTime());
        timeSpanEndKey = TimeLocation.makeTimeKey(timeSpan.getEndTime());
    }

    @Override
    public synchronized void receiveMessage(IRaptorDataMessage rdm)
    {
        // We only care about RDMs with locations that are renderable and not from user "drag" operations
        if (!rdm.hasLocation() || rdm.isManuallyGeneratedLocation() || !rdm.isRenderable()) return;

        final var trackPoint = new TimeLocation(rdm);
        final long relevantTime = trackPoint.getTime();
        final boolean withinTimeSpan = relevantTime > timeSpanStartKey.getTime() && relevantTime < timeSpanEndKey.getTime();
        final boolean outsideDeviceRange = !containsTime(trackPoints, relevantTime);

        trackPoints.add(trackPoint);

        // If the new track point is within the current playback timeline, then notify the listeners
        if (withinTimeSpan) fireTrackPointAdded(trackPoint);

        // If the new track point is outside our current device timeline, we need to notify the listeners
        if (outsideDeviceRange) fireDeviceTimesChanged();
    }

    /**
     * @return The subset of all our tracks within the time boundaries of our time span
     */
    private NavigableSet<ITimeLocation> getTimeSpanSubset()
    {
        return trackPoints.subSet(timeSpanStartKey, true, timeSpanEndKey, true);
    }

    /**
     * Determine if a given time is within the time boundaries of the first and last elements in the given set of
     * track points.
     *
     * @param tracks Set of track points to inspect
     * @param time   The time of interest
     * @return {@code true} if the set is not empty and the time given is between {@code tracks.first().getTime()}
     * and {@code tracks.last().getTime()}, inclusive.
     */
    private boolean containsTime(NavigableSet<ITimeLocation> tracks, long time)
    {
        return !tracks.isEmpty() && time >= tracks.first().getTime() && time <= tracks.last().getTime();
    }

    @Override
    public void addListener(IDeviceTracksModelListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IDeviceTracksModelListener listener)
    {
        listeners.remove(listener);
    }

    @RemoveObserver
    @Override
    public void disposeResources()
    {
        logger.trace("Dispose DTM: {}", device);

        final var moc = device.getController();
        if (moc != null) moc.removeMapObjectControllerListener(this); // Can be null if the MO has been deleted

        listeners.clear();
        trackPoints.clear();
    }

    /**
     * Handle a Map Object being removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        // If we are in playback and the MO is deleted then we need to mark the mo as not in playback which will
        // inform the rest of the system that it is no loner in playback.
        final var mapObject = event.getMapObject();
        if (mapObject.equals(device)) mapObject.setPlaybackMode(false);
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is partially deleted so we can clean out any data prior
     * to the deletion time.
     *
     * @param event The event providing the partially deleted map object
     * @since 2.38 - formerly mapObjectPartialDelete
     */
    private synchronized void onMapObjectPartialDelete(@Observes MapObjectPartialDelete event)
    {
        IMapObject mapObject = event.getMapObject();

        if (!mapObject.equals(device)) return; // Not our MO!
        if (trackPoints.isEmpty()) return; // Nothing to do

        // Create a new track point set containing only data after the given time of deletion

        final ITimeLocation startTime = TimeLocation.makeTimeKey(event.getTimeOfDeletion());
        final ITimeLocation endTime = trackPoints.last();

        if (startTime.getTime() >= endTime.getTime())
        {
            // delete everything
            trackPoints.clear();
        } else
        {
            final SortedSet<ITimeLocation> subset = trackPoints.subSet(startTime, endTime);
            final List<ITimeLocation> keep = new ArrayList<>(subset);

            trackPoints.clear();
            trackPoints.addAll(keep);
        }
        fireDeviceTimesChanged();
    }

    /**
     * Load the tracks for this Map Object into our set of points.
     */
    private synchronized void initTrackPoints()
    {
        long startTime = System.currentTimeMillis();
        boolean gotOne = false;

        try
        {
            // Retrieve the fields needed to construct our playback track points
            final var results = PlaybackQueryUtils.query(device,
                    CoreField.relevantTime, CoreField.latitude, CoreField.longitude, CoreField.hasElevation, CoreField.elevation);

            for (Object[] result : results)
            {
                final Long relevantTime = (Long) result[0];
                final Double latitude = (Double) result[1];
                final Double longitude = (Double) result[2];
                final Boolean hasElevation = (Boolean) result[3];
                final Double elevation = (Double) result[4];

                trackPoints.add(new TimeLocation(relevantTime, latitude, longitude, hasElevation, elevation));
                if (!gotOne)
                {
                    if (!playbackSystem.isLive()) device.setPlaybackMode(true);
                    gotOne = true;
                }
            }

            if (logger.isDebugEnabled())
            {
                long endTime = System.currentTimeMillis();
                logger.debug("TrackPoint Initialization Complete for \"{}\" ({} points in {} ms)", device, trackPoints.size(), endTime - startTime);
            }

            if (trackPoints.isEmpty())
            {
                // Treat this as a special case. this MO has no track points that factor into the playback timeline
                // So in order to give the user something not-too-alarming we will tell the MO to stay on the globe
                // and not actually participate in playback
                device.setPlaybackMode(false);
            }
        } catch (InvalidFieldSelectorException | ConflictingConstraintException e)
        {
            logger.error("Failed to query for tracks", e);
        }

        synchronized (initializedLock)
        {
            initialized = true;
            initializedLock.notifyAll();
        }

        fireDeviceTimesChanged();
    }

    /**
     * Inform all listeners that a track point has been added and its timestamp is within the playback timeline.
     *
     * @param trackPoint the track point that has been added to the playback timeline
     */
    private void fireTrackPointAdded(ITimeLocation trackPoint)
    {
        ExceptionUtils.safeDoOperation(listeners, listener -> listener.trackPointAdded(device, trackPoint));
    }

    private void fireDeviceTimesChanged()
    {
        // inform all timeline model listeners that the time boundary has changed
        final boolean trackPointsEmpty = trackPoints.isEmpty();
        final long now = System.currentTimeMillis();
        final long start = trackPointsEmpty ? now : trackPoints.first().getTime();
        final long end = trackPointsEmpty ? now : trackPoints.last().getTime();
        final var boundary = new TimeBoundary(start, end);

        ExceptionUtils.safeDoOperation(listeners, listener -> listener.deviceTimesChanged(device, boundary));
    }
}
