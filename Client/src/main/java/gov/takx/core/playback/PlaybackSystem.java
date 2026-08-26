package gov.takx.core.playback;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.playback.IPlaybackConfiguration;
import gov.takx.api.playback.IPlaybackEventListener;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.IPlaybackStrategy;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.PlaybackConfigurationAdapter;
import gov.takx.api.playback.PlaybackEvent;
import gov.takx.api.playback.PlaybackLiveEvent;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.api.playback.ui.IPlaybackStage;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.playback.globe.GlobeModel;
import gov.takx.core.playback.globe.GlobePlaybackLane;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.platform.playback.TimeBoundary;
import gov.takx.platform.playback.TimeSpan;
import gov.takx.platform.playback.TimeSpanAdapter;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;

import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the playback system.  It manages the aggregated components, like the timespan, timeline panel,
 * and various models.  The plugin-visible interface is defined in {@link IPlaybackManager}.
 * <p>
 * Components interested in the high-level events should register using
 * {@link #addPlaybackEventListener(IPlaybackEventListener)}.  If access to lower-level events is required, then add
 * listeners on the sub-systems.
 */
@ApplicationScoped
public class PlaybackSystem implements IPlaybackSystem
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ThreadFactory THREAD_FACTORY = new BasicThreadFactory.Builder().namingPattern("Playback Scheduler").build();
    private static final long RATE_30_FPS = 33; // 30 FPS In ms

    // Collection of PlaybackEvent listeners
    private final List<IPlaybackEventListener> playbackEventListeners = new CopyOnWriteArrayList<>();

    private IPlaybackConfiguration playbackConfiguration;
    private IPlaybackStage playbackStage;
    private ITimeSpan timeSpan;
    private GlobeModel globeModel;

    private ScheduledFuture<?> timespanUpdater;

    // The executor to run our "play" tasks
    // Don't convert this to a SafeNameFixingExecutor because the mechanics of that executor
    // cause repeating tasks to never repeat.
    private ScheduledThreadPoolExecutor playbackExecutor;
    private IPlaybackStrategy playbackStrategy;

    private volatile boolean playing;
    private volatile boolean live = true;
    private final Object stateLock = new Object();
    private long playStart;
    private long start;

    private boolean started;

    private GlobePlaybackLane globePlaybackLane;

    private final boolean headless;
    private final IExtendedUISpaceManager uiSpaceManager;
    private final IMapObjectManager mapObjectManager;
    private final ICoreExecutorService executorService;
    private final Event<PlaybackEvent> eventGun;
    private final Event<PlaybackLiveEvent> playbackLiveEvent;

    @Inject
    PlaybackSystem(@Headless boolean headless, IExtendedUISpaceManager uiSpaceManager, IMapObjectManager mapObjectManager,
                   ICoreExecutorService executorService, Event<PlaybackEvent> eventGun, Event<PlaybackLiveEvent> playbackLiveEvent)
    {
        this.headless = headless;
        this.uiSpaceManager = uiSpaceManager;
        this.mapObjectManager = mapObjectManager;
        this.executorService = executorService;
        this.eventGun = eventGun;
        this.playbackLiveEvent = playbackLiveEvent;
    }

    @Override
    public void setPlaybackStage(IPlaybackStage playbackStage)
    {
        this.playbackStage = playbackStage;
    }

    @Override
    public Component getGlobalTimeControl()
    {
        return getGlobePlaybackLane().getGlobalTimeControl();
    }

    @Override
    public IPlaybackStage getPlaybackStage()
    {
        return playbackStage;
    }

    @Override
    public IGlobeModel getGlobeModel()
    {
        return globeModel;
    }

    @Override
    public IPlaybackConfiguration getPlaybackConfiguration()
    {
        return playbackConfiguration;
    }

    @Override
    public void setPlaybackVisible()
    {
        uiSpaceManager.showComponent(CoreComponentType.PLAYBACK_PANEL);
    }

    @Override
    public void setPlaybackInvisible()
    {
        uiSpaceManager.hideComponent(CoreComponentType.PLAYBACK_PANEL);
    }

    @Override
    public ITimeSpan getTimeSpan()
    {
        return timeSpan;
    }

    @Override
    public void addCustomControlComponent(JComponent component)
    {
        playbackStage.getPlaybackControls().addCustomControlComponent(component);
    }

    @Override
    public void removeCustomControlComponent(JComponent component)
    {
        playbackStage.getPlaybackControls().removeCustomControlComponent(component);
    }

    @Override
    public void addPlaybackLanes(IPlaybackLane... playbackLanes)
    {
        playbackConfiguration.addPlaybackLanes(playbackLanes);
    }

    @Override
    public void removePlaybackLanes(IPlaybackLane... playbackLanes)
    {
        playbackConfiguration.removePlaybackLanes(playbackLanes);
    }

    @Override
    public void replacePlaybackLane(IPlaybackLane oldLane, IPlaybackLane newLane)
    {
        playbackConfiguration.replacePlaybackLane(oldLane, newLane);
    }

    @Override
    public List<IPlaybackLane> getPlaybackLanes()
    {
        return playbackConfiguration.getPlaybackLanes();
    }

    @InvokeAndWaitOnEDT
    private void createGlobePlaybackLane()
    {
        if (globePlaybackLane == null)
        {
            globePlaybackLane = new GlobePlaybackLane();
            globePlaybackLane.setMapObjectList(globeModel.getDevicesList());
            addPlaybackLanes(globePlaybackLane);
        }
    }

    @Override
    public void fitTimeSpanToLanes()
    {
        resetTimeSpan();
    }

    @Override
    public void fitTimeSpanToMapObject(IMapObject mo)
    {
        resetTimeSpan(mo);
    }

    /**
     * Reset the time span for playback so that it spans the time boundaries of all our lanes.  If we have no lanes,
     * then the time span is set to "now" plus 4 minutes.
     */
    private void resetTimeSpan()
    {
        final TimeBoundary boundary = getTimeBoundaryOfAllLanes();

        if (boundary != null)
        {
            stop();
            timeSpan.setTimes(boundary);
        }
    }

    /**
     * Reset the time span for playback so that it spans the time boundaries of the MO.  If we have no lanes,
     * then the time span is set to "now" plus 4 minutes.
     */
    private void resetTimeSpan(IMapObject mo)
    {
        final TimeBoundary boundary = getTimeBoundaryForMapObject(mo);

        if (boundary != null)
        {
            stop();
            timeSpan.setTimes(boundary);
        }
    }

    @Override
    public void setPlaybackSpeed(int speed)
    {
        playbackConfiguration.setPlaybackSpeed(speed);
    }

    @Override
    public int getPlaybackSpeed()
    {
        return playbackConfiguration.getPlaybackSpeed();
    }

    @Override
    public IDeviceTracksModel addDevice(IMapObject device)
    {
        globePlaybackLane.addMapObjects(Collections.singletonList(device));
        return globeModel.addDevice(device);
    }

    @Override
    public boolean removeDevice(IMapObject device)
    {
        if (globePlaybackLane != null && globeModel.eligibleForPlayback(device))
        {
            globePlaybackLane.removeMapObjects(Collections.singletonList(device));
            return true;
        }
        return false;
    }

    @Override
    public List<IMapObject> getDeviceList()
    {
        return globePlaybackLane == null ? null : new ArrayList<>(globePlaybackLane.getMapObjects());
    }

    @Override
    public boolean isDeviceInPlayback(IMapObject device)
    {
        return globePlaybackLane.getMapObjects().contains(device);
    }

    @Override
    public void stepBackOneTrackPoint()
    {
        final long playbackTime = getCurrentPlaybackPosition();
        ITimeLocation nextTrackPoint = globeModel.getPreviousTrackPointInPlayback(playbackTime);
        if (nextTrackPoint != null)
        {
            gotoTime(nextTrackPoint.getTime());
        }
    }

    @Override
    public void stepForwardOneTrackPoint()
    {
        final long playbackTime = getCurrentPlaybackPosition();
        ITimeLocation nextTrackPoint = globeModel.getNextTrackPointInPlayback(playbackTime);
        if (nextTrackPoint != null)
        {
            gotoTime(nextTrackPoint.getTime());
        }
    }

    @Override
    public void pause()
    {
        synchronized (stateLock)
        {
            stopLive();
            if (!playing) return;

            if (logger.isDebugEnabled()) logger.debug("Pausing at time: {}", timeSpan.getPosition());
            stopPlaybackTask();
        }

        firePlaybackEvent(PlaybackEvent.Paused);
    }

    @Override
    public void play()
    {
        logger.debug("Playing");

        synchronized (stateLock)
        {
            if (playing) return; // Already playing

            playing = true; // We are now
        }

        start = timeSpan.getDeltaPosition();
        playStart = System.currentTimeMillis();

        firePlaybackEvent(PlaybackEvent.Playing);

        playbackStrategy = playbackConfiguration.getPlaybackStrategy();
        if (playbackStrategy == null)
        {
            playbackStrategy = createDefaultPlaybackStrategy();
        }
        playbackStrategy.play();

        // Animate the playback cursor position at a fixed rate (every 33 ms ~= 30 frames per second).

    }

    private IPlaybackStrategy createDefaultPlaybackStrategy()
    {
        return new IPlaybackStrategy()
        {
            private ScheduledFuture<?> playbackTask; // The animation task during actual playback

            @Override
            public void play()
            {
                stop();
                playbackTask = playbackExecutor.scheduleAtFixedRate(() -> {
                    if (!playing) return;

                    if (!Thread.currentThread().isInterrupted())
                    {
                        final int playbackSpeed = playbackConfiguration.getPlaybackSpeed();
                        final long timeSinceStart = ((System.currentTimeMillis() - playStart) * playbackSpeed) + start;
                        final boolean timePastEnd = timeSinceStart >= timeSpan.getTotalTime();

                        // If this new time would put us past the playback timeline end and auto-scroll is not enabled,
                        // then issue a stop. Otherwise, just update the time span current position.
                        if (timePastEnd && !PreferenceEntry.PLAYBACK_AUTO_SCROLL_AT_END.getBooleanValue())
                        {
                            logger.debug("Auto-stop at end");
                            stop();
                        } else
                        {
                            // Ensure that we are really still playing before we fire this event
                            synchronized (stateLock)
                            {
                                if (playing)
                                {
                                    if (timePastEnd) timeSpan.panTimeByXOffset(ITimeSpan.DEFAULT_PAN_OFFSET);
                                    timeSpan.setPosition(timeSpan.getStartTime() + timeSinceStart);
                                }
                            }
                        }
                    }
                }, 0, RATE_30_FPS, TimeUnit.MILLISECONDS);
            }

            @Override
            public void stop()
            {
                if (playbackTask != null)
                {
                    playbackTask.cancel(true);
                }
                playbackTask = null;
            }
        };
    }

    @Override
    public void goLive()
    {
        if (!live)
        {
            live = true;

            stopPlayback();
            firePlaybackEvent(PlaybackEvent.Live);
            playbackLiveEvent.fire(new PlaybackLiveEvent(live));
        }
    }

    @Override
    public void stopLive()
    {
        if (live)
        {
            live = false;

            firePlaybackEvent(PlaybackEvent.Stopped);
            playbackLiveEvent.fire(new PlaybackLiveEvent(live));
        }
    }

    @Override
    public boolean isLive()
    {
        return live;
    }

    @Override
    public GlobePlaybackLane getGlobePlaybackLane()
    {
        return globePlaybackLane;
    }

    @Override
    public void stop()
    {
        stopLive();
        stopPlayback();
    }

    @Override
    public void stopScrubbing()
    {
        firePlaybackEvent(PlaybackEvent.ScrubbingStopped);
    }

    /**
     * Stop the playback
     */
    private void stopPlayback()
    {
        boolean wasPlaying = false;
        synchronized (stateLock)
        {
            if (playing)
            {
                logger.debug("stopping");

                wasPlaying = true;
                stopPlaybackTask();
            }
        }

        if (wasPlaying)
        {
            firePlaybackEvent(PlaybackEvent.Stopped);
        }

        timeSpan.setPosition(timeSpan.getStartTime()); // Back to the start
    }

    /**
     * Mark ourselves as not playing and cancel the playback timer task.
     */
    private void stopPlaybackTask()
    {
        playing = false;

        if (playbackStrategy != null)
        {
            playbackStrategy.stop();
        }
    }

    /**
     * Set the playback position in the timespan to the given value.
     *
     * @param time New playback time position
     */
    private void gotoTime(long time)
    {
        logger.debug("Going to time {}", time);

        timeSpan.setPosition(time);
        stopLive();
    }

    @Override
    public long getCurrentPlaybackPosition()
    {
        return timeSpan.getPosition();
    }

    @Override
    @Nonnull
    public ILocation getCurrentLocation(IMapObject mapObject)
    {
        final var tracksModel = globeModel.getDeviceTracksModel(mapObject);
        final var previousTrackPoint = tracksModel != null ? tracksModel.getPreviousTrackPoint(getCurrentPlaybackPosition()) : null;
        return previousTrackPoint != null ? previousTrackPoint : GeoPoint.UNKNOWN_POINT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * @apiNote An API request needs to perform the same way as a user clicking on a new location on the timeline ruler.
     * So, if we are playing, pause, change locations, and then resume playing so all registered listeners
     * can properly update the current "playback" origin.
     */
    @Override
    public void setCurrentPlaybackPosition(long currentPosition)
    {
        if (getCurrentPlaybackPosition() != currentPosition)
        {
            synchronized (stateLock)
            {
                // If we're currently playing, we need to pause/restart to ensure all control data is updated
                boolean wasPlaying = playing;

                if (wasPlaying) pause();

                gotoTime(currentPosition);

                if (wasPlaying) play();
            }
        }
    }

    @Override
    public boolean isPlaying()
    {
        return playing;
    }

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_PlaybackSystem) StartProjectServices ignored)
    {
        playbackConfiguration = new PlaybackConfiguration();
        playbackConfiguration.setPlaybackStrategy(createDefaultPlaybackStrategy());

        timeSpan = new TimeSpan();
        globeModel = new GlobeModel(timeSpan);

        timespanUpdater = executorService.scheduleAtFixedRate(this::updateTimespan, 1, 1, TimeUnit.SECONDS);

        playbackExecutor = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY);
        playbackExecutor.setRemoveOnCancelPolicy(true); // Don't leave them hanging around in the queue

        playbackConfiguration.addPlaybackConfigurationListener(new ConfigurationListener());

        // Install a default timeline until we have playback lanes added
        final long now = System.currentTimeMillis();
        timeSpan.setTimes(now, now + TimeUnit.MINUTES.toMillis(4));
        timeSpan.addListener(new TimeSpanEventBridge()); // Translate low-level timespan events in to playback events
        createGlobePlaybackLane();

        started = true;
    }

    /**
     * Update the timespan with the new current time if we are in "live" mode and not playing.
     *
     * @since 3.0
     */
    private void updateTimespan()
    {
        Thread.currentThread().setName("Playback Timespan Updater");

        if (live && !playing)
        {
            final long currentTime = System.currentTimeMillis();
            timeSpan.setTimes(currentTime - timeSpan.getTotalTime(), currentTime);
            timeSpan.setPosition(currentTime);
        }
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        if (!started) return;

        stop();

        timespanUpdater.cancel(true);
        playbackExecutor.shutdownNow();

        playbackConfiguration.disposeResources();
        globePlaybackLane = null;
        globeModel.disposeResources();

        playbackStage = null;

        playbackEventListeners.clear();

        started = false;
    }

    @Override
    public void addPlaybackEventListener(IPlaybackEventListener listener)
    {
        playbackEventListeners.add(listener);
    }

    @Override
    public void removePlaybackEventListener(IPlaybackEventListener listener)
    {
        playbackEventListeners.remove(listener);
    }

    /**
     * Fire a playback event to all registered listeners.
     *
     * @param playbackEvent Event to fire
     */
    private void firePlaybackEvent(PlaybackEvent playbackEvent)
    {
        ExceptionUtils.safeDoOperation(playbackEventListeners, l -> l.onPlaybackEvent(playbackEvent));
        eventGun.fire(playbackEvent);
    }

    /**
     * Determine the spanning time boundary of all the playback lanes.  This may be {@code null} if there are no lanes.
     *
     * @return Spanning time boundary or {@code null}
     */
    private TimeBoundary getTimeBoundaryOfAllLanes()
    {
        final var laneBoundaries = playbackConfiguration.getPlaybackLanes().stream()
                .map(IPlaybackLane::getTimeBoundary)
                .toList();
        return TimeBoundary.union(laneBoundaries);
    }

    /**
     * Determine the spanning time boundary of all the playback lanes.  This may be {@code null} if there are no lanes.
     *
     * @return Spanning time boundary or {@code null}
     */
    private TimeBoundary getTimeBoundaryForMapObject(IMapObject mo)
    {
        return globeModel.getDeviceTracksModel(mo).getTimeBoundary();
    }

    /**
     * Process a ProjectOpened lifecycle event from the event bus
     *
     * @param event The event to process
     */
    public void onProjectOpened(@Observes ProjectOpened event)
    {
        if (!headless) readFromPreferences();
    }

    /**
     * Process a ProjectSaving lifecycle event from the event bus
     *
     * @param event The event to process
     */
    public void onProjectSaving(@Observes ProjectSaving event)
    {
        if (!headless)
        {
            if (globePlaybackLane != null)
            {
                ProjectPreferenceEntry.PLAYBACK_NODE.node()
                        .putAsValues(globePlaybackLane.getMapObjects(), IMapObject::getKey);
            }
        }
    }

    private void readFromPreferences()
    {
        ProjectPreferenceEntry.PLAYBACK_NODE.node().values()
                .stream()
                .map(mapObjectManager::getMapObject)
                .filter(Objects::nonNull)
                .forEach(this::addDevice);

        final GlobePlaybackLane lane = getGlobePlaybackLane();
        if (lane != null)
        {
            lane.setMapObjectList(getDeviceList());
        }
    }

    /**
     * Simple adapter to take timespan events and fire them as playback events.
     */
    private final class TimeSpanEventBridge extends TimeSpanAdapter
    {
        @Override
        public void timeSpanChanged()
        {
            if (isPlaying())
            {
                pause();
                play();
            }
            firePlaybackEvent(PlaybackEvent.TimeSpanChanged);
        }

        @Override
        public void timePositionChanged()
        {
            // If we are playing, then it's a position change, otherwise it's a scrub event
            firePlaybackEvent(playing ? PlaybackEvent.PlayPositionChanged : PlaybackEvent.ScrubPositionChanged);
        }
    }

    /**
     * Listener to handle changes in the playback configuration.
     */
    private final class ConfigurationListener extends PlaybackConfigurationAdapter
    {
        @Override
        public void playbackSpeedChanged(int speed)
        {
            // If we're currently playing, we need to pause/restart to ensure all control data is updated
            boolean wasPlaying = playing;

            if (wasPlaying) pause();

            firePlaybackEvent(PlaybackEvent.PlaybackSpeedChanged);

            if (wasPlaying) play();
        }
    }
}
