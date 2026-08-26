package gov.takx.core.breadcrumb;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.icons.TakColor;
import gov.tak.api.icons.TeamColorMapper;
import gov.tak.api.maps.Marker;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrackCreated;
import gov.takx.api.breadcrumb.event.TrackDeleted;
import gov.takx.api.breadcrumb.event.TrackUpdated;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbAdded;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbUpdated;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsRemoved;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrailColorUpdated;
import gov.takx.api.breadcrumb.event.TrailDeleted;
import gov.takx.api.breadcrumb.event.TrailVisibilityChanged;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.persist.breadcrumb.Breadcrumb;
import gov.takx.core.persist.breadcrumb.BreadcrumbPersist;
import gov.takx.core.persist.breadcrumb.Track;
import gov.takx.core.persist.breadcrumb.TrackPersist;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Manages {@link IBreadcrumb breadcrumbs}, including trails and {@link ITrack tracks}, and notifies listeners when
 * changes occur.
 *
 * @since 5.3
 */
@ApplicationScoped
public class BreadcrumbManager implements IBreadcrumbManager, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // ATAK limits the number of breadcrumbs added to the map to one every two seconds for performance and de-cluttering
    private static final int MILLISECONDS_BETWEEN_TRAIL_BREADCRUMBS = 2000;
    private static final int DEFAULT_BREADCRUMB_COLOR = Color.BLUE.getRGB();
    private static final List<String> PROPERTIES = List.of(IMapObject.LOCATION, IMapObject.ICON);

    private final TrackPersist trackPersist;
    private final BreadcrumbPersist breadcrumbPersist;
    private final Event<TrackBreadcrumbsUpdated> trackBreadcrumbsUpdatedEvent;
    private final Event<TrackCreated> trackCreatedEvent;
    private final Event<TrackUpdated> trackUpdatedEvent;
    private final Event<TrackDeleted> trackDeletedEvent;
    private final Event<TrailBreadcrumbAdded> trailBreadcrumbAddedEvent;
    private final Event<TrailBreadcrumbUpdated> trailBreadcrumbUpdatedEvent;
    private final Event<TrailBreadcrumbsRemoved> trailBreadcrumbRemovedEvent;
    private final Event<TrailBreadcrumbsUpdated> trailBreadcrumbsUpdatedEvent;
    private final Event<TrailVisibilityChanged> trailVisibilityChangedEvent;
    private final Event<TrailColorUpdated> trailUpdatedEvent;
    private final Event<TrailDeleted> trailDeletedEvent;

    private final IMapObjectFinder mapObjectFinder;
    private final ServerTrackQueryService serverTrackQueryService;
    private final boolean platformUnderTest;
    private final Map<String, MarkerBreadcrumbStatus> markerTrackStatusMap = new ConcurrentHashMap<>();
    private final IPreferenceMonitor<Boolean> selfMarkerLoggingMonitor =
            PreferenceEntry.BREADCRUMBS_LOG_SELF_TRACKS.newBooleanMonitor(b -> updateSelfMarkerLogging());
    private String selfMarkerUid;

    @Inject
    BreadcrumbManager(TrackPersist trackPersist, BreadcrumbPersist breadcrumbPersist,
                      Event<TrackBreadcrumbsUpdated> trackBreadcrumbsUpdatedEvent,
                      Event<TrackCreated> trackCreatedEvent, Event<TrackDeleted> trackDeletedEvent,
                      Event<TrackUpdated> trackUpdatedEvent, Event<TrailBreadcrumbAdded> trailBreadcrumbAddedEvent,
                      Event<TrailBreadcrumbUpdated> trailBreadcrumbUpdatedEvent,
                      Event<TrailBreadcrumbsRemoved> trailBreadcrumbRemovedEvent, Event<TrailBreadcrumbsUpdated> trailBreadcrumbsUpdatedEvent,
                      Event<TrailVisibilityChanged> trailVisibilityChangedEvent,
                      Event<TrailColorUpdated> trailUpdatedEvent, Event<TrailDeleted> trailDeletedEvent,
                      IMapObjectFinder mapObjectFinder, ServerTrackQueryService serverTrackQueryService,
                      @PlatformUnderTest boolean platformUnderTest)
    {
        this.trackPersist = trackPersist;
        this.breadcrumbPersist = breadcrumbPersist;
        this.trackBreadcrumbsUpdatedEvent = trackBreadcrumbsUpdatedEvent;
        this.trackCreatedEvent = trackCreatedEvent;
        this.trackDeletedEvent = trackDeletedEvent;
        this.trackUpdatedEvent = trackUpdatedEvent;
        this.trailBreadcrumbAddedEvent = trailBreadcrumbAddedEvent;
        this.trailBreadcrumbUpdatedEvent = trailBreadcrumbUpdatedEvent;
        this.trailBreadcrumbRemovedEvent = trailBreadcrumbRemovedEvent;
        this.trailBreadcrumbsUpdatedEvent = trailBreadcrumbsUpdatedEvent;
        this.trailVisibilityChangedEvent = trailVisibilityChangedEvent;
        this.trailUpdatedEvent = trailUpdatedEvent;
        this.trailDeletedEvent = trailDeletedEvent;
        this.mapObjectFinder = mapObjectFinder;
        this.serverTrackQueryService = serverTrackQueryService;
        this.platformUnderTest = platformUnderTest;
    }

    // START_GenericCoreService - 1 to ensure this gets called before the self marker is created to prevent duplicate tracks
    @SuppressWarnings("unused")
    private void onServicesStarted(@Observes @Priority(START_GenericCoreService - 1) StartProjectServices event)
    {
        // Restore the track cache
        trackPersist.getAllTracks().forEach(track -> {
            getOrCreateTrackStatus(track.getMarkerUid()).tracks.add(track);
            trackBreadcrumbsUpdatedEvent.fire(new TrackBreadcrumbsUpdated(track, getTrackBreadcrumbs(track)));
        });
    }

    @SuppressWarnings("unused")
    private void onServicesStopped(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        markerTrackStatusMap.clear();
    }

    @VisibleForTesting
    void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (event.getMapObject() instanceof Marker marker)
        {
            marker.addPropertyChangeListener(PROPERTIES, this);

            if (marker.isSelfMarker())
            {
                selfMarkerUid = marker.getUid();
                updateSelfMarkerLogging();
            }

            if (marker.getMetaBoolean(StandardAttributeNames.BREADCRUMBS_ENABLED, false))
            {
                startBreadcrumbs(marker.getUid(), null);
            }

            final var attributes = marker.getAttributeSet();
            attributes.removeAttribute(StandardAttributeNames.BREADCRUMBS_ENABLED);
            attributes.addPropertyChangeListener(StandardAttributeNames.BREADCRUMBS_ENABLED, this::breadcrumbsEnabledPropertyChanged);
        }
    }

    @SuppressWarnings("unused")
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (event.getMapObject() instanceof Marker marker)
        {
            final var markerUid = marker.getUid();

            // Remove the cached info and delete each track's breadcrumbs
            final var markerTrackStatus = markerTrackStatusMap.remove(markerUid);

            if (markerTrackStatus != null)
            {
                markerTrackStatus.tracks.forEach(track -> {
                    if (markerTrackStatus.persistable) breadcrumbPersist.deleteTrack(track.getUid());
                    trackDeletedEvent.fire(new TrackDeleted(track));
                });

                trailDeletedEvent.fire(new TrailDeleted(markerUid));

                if (markerTrackStatus.persistable) trackPersist.delete(markerUid); // Finally, delete the tracks
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final Marker marker = (Marker) evt.getSource();
        final String markerUid = marker.getUid();

        if (IMapObject.LOCATION.equals(propertyName))
        {
            if (!marker.getMetaBoolean(StandardAttributeNames.DRAGGING, false)) addBreadcrumb(marker);
        } else if (IMapObject.ICON.equals(propertyName))
        {
            final var markerTrackStatus = markerTrackStatusMap.get(markerUid);

            if (markerTrackStatus != null)
            {
                int color = getTrailBreadcrumbColor(marker);

                if (markerTrackStatus.trailColor != color)
                {
                    markerTrackStatus.trailColor = color;
                    trailUpdatedEvent.fire(new TrailColorUpdated(markerUid, color));
                }
            }
        }
    }

    /**
     * Handle a change on a Marker's attribute set's {@link StandardAttributeNames#BREADCRUMBS_ENABLED} property.
     * @since 5.6
     */
    private void breadcrumbsEnabledPropertyChanged(PropertyChangeEvent pce)
    {
        final var attributeSet = (AttributeSet) pce.getSource();
        final var enable = (Boolean) pce.getNewValue();

        if (enable == null) return; // Ignore attribute removal

        final var uid = attributeSet.getStringAttribute(StandardAttributeNames.UID);

        if (enable)
        {
            startBreadcrumbs(uid, null);
        } else
        {
            stopBreadcrumbs(uid);
        }

        attributeSet.removeAttribute(StandardAttributeNames.BREADCRUMBS_ENABLED);
    }

    /**
     * @return The marker with the given {@code markerUid}, throwing an exception if it is not found or is not a {@link Marker}.
     * @since 5.6
     */
    private Marker getMarker(@Nonnull String markerUid) throws IllegalArgumentException
    {
        if (!(mapObjectFinder.findMapObjectByUid(markerUid) instanceof Marker marker)) throw new IllegalArgumentException("No Marker is associated with UID " + markerUid);
        return marker;
    }

    @Override
    public void startBreadcrumbs(@Nonnull String markerUid, @Nullable String callsign)
    {
        final var markerBreadcrumbStatus = getOrCreateTrackStatus(markerUid);

        if (markerBreadcrumbStatus.currentTrack == null)
        {
            startNewTrack(markerUid, callsign, null);
        } else
        {
            final var marker = getMarker(markerUid);
            markerBreadcrumbStatus.recordBreadcrumbs = true;
            markerBreadcrumbStatus.trailColor = getTrailBreadcrumbColor(marker);
            addBreadcrumb(marker);
        }

        trailVisibilityChangedEvent.fire(new TrailVisibilityChanged(markerUid, true));
    }

    @Override
    public void stopBreadcrumbs(@Nonnull String markerUid)
    {
        final var markerBreadcrumbStatus = markerTrackStatusMap.get(markerUid);

        if (markerBreadcrumbStatus != null)
        {
            markerBreadcrumbStatus.recordBreadcrumbs = false;
            trailVisibilityChangedEvent.fire(new TrailVisibilityChanged(markerUid, false));
        }
    }

    @Nonnull
    @Override
    public List<IBreadcrumb> getTrailBreadcrumbs(@Nonnull String markerUid)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(markerUid);

        return markerBreadcrumbStatus != null ? markerBreadcrumbStatus.trailBreadcrumbs.stream().toList() : Collections.emptyList();
    }

    @Override
    @Nonnull
    public List<IBreadcrumb> getTrackBreadcrumbs(@Nonnull ITrack track)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(track.getMarkerUid());

        return markerBreadcrumbStatus != null && markerBreadcrumbStatus.currentTrack == track
                ? Collections.unmodifiableList(markerBreadcrumbStatus.currentTrackBreadcrumbs)
                : breadcrumbPersist.getBreadcrumbs(track.getUid());
    }

    @Override
    public boolean areBreadcrumbsEnabled(@Nonnull String markerUid)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(markerUid);

        return markerBreadcrumbStatus != null && markerBreadcrumbStatus.recordBreadcrumbs;
    }

    @Override
    public void startNewTrack(@Nonnull String markerUid, @Nullable String callsign, @Nullable String trackName)
    {
        startNewTrack(markerUid, callsign, trackName, true);
    }

    /**
     * @param trackUid The track's UID
     * @param markerUid The marker's UID
     * @param callsign The marker user's callsign
     * @param breadcrumbs The track's breadcrumbs
     * @throws IllegalArgumentException if {@code breadcrumbs} is empty
     * @since 5.4
     */
    public void addTrackFromServer(@Nonnull String trackUid, @Nonnull String markerUid, @Nonnull String callsign,
                                   @Nonnull List<Breadcrumb> breadcrumbs)
            throws IllegalArgumentException
    {
        if (breadcrumbs.isEmpty()) throw new IllegalArgumentException("breadcrumbs cannot be empty");

        final var markerBreadcrumbStatus = getOrCreateTrackStatus(markerUid);
        final var start = breadcrumbs.get(0).getTime();
        final var trackName = "Server " + callsign + " " + getDefaultTrackName(start);

        // Retrieve the existing track or create a new one. No UID is provided, so we have to use the track name based on
        // the first breadcrumb's timestamp to id tracks pulled from the server
        final Track track = (Track) markerBreadcrumbStatus.tracks.stream()
                .filter(t -> t.getName().equals(trackName))
                .findFirst()
                .orElseGet(() -> {
                    var newTrack = new Track(trackUid, markerUid, trackName, callsign, start, markerBreadcrumbStatus.getNextColor());
                    markerBreadcrumbStatus.tracks.add(newTrack);
                    if (markerBreadcrumbStatus.persistable) trackPersist.persist(newTrack);
                    return newTrack;
                });

        final var existingBreadcrumbCount = track.getBreadcrumbCount();

        // Don't do anything if an existing track didn't change
        if (existingBreadcrumbCount >= breadcrumbs.size()) return;

        // Skip existing breadcrumbs
        final var newBreadcrumbs = breadcrumbs.subList(existingBreadcrumbCount, breadcrumbs.size());

        // Add any new breadcrumbs
        newBreadcrumbs.forEach(breadcrumb -> {
            track.update(breadcrumb);
            if (markerBreadcrumbStatus.persistable) breadcrumbPersist.persist(breadcrumb);
        });

        // Persist the final track
        if (markerBreadcrumbStatus.persistable) trackPersist.persist(track);

        // Notify listeners (new track created or existing track's breadcrumbs changed)
        trackBreadcrumbsUpdatedEvent.fire(new TrackBreadcrumbsUpdated(track, newBreadcrumbs));
    }

    @Override
    public void deleteTrack(@Nonnull ITrack track)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(track.getMarkerUid());

        if (markerBreadcrumbStatus == null)
        {
            logger.warn("Tried to remove track with no status. Track UID: {}, Track name: {}, Marker UID: {}",
                    track.getUid(), track.getName(), track.getMarkerUid());
            return;
        }

        // Delete from storage
        if (markerBreadcrumbStatus.persistable) trackPersist.delete(track);

        // Delete from marker's cached set of tracks
        markerBreadcrumbStatus.tracks.remove(track);

        boolean restart = false;
        // If the track is current, reset state
        if (track.equals(markerBreadcrumbStatus.currentTrack))
        {
            markerBreadcrumbStatus.currentTrack = null;
            markerBreadcrumbStatus.currentTrackBreadcrumbs.clear();
            markerBreadcrumbStatus.lastTrackBreadcrumb = null;

            restart = markerBreadcrumbStatus.recordBreadcrumbs;
        }

        trackDeletedEvent.fire(new TrackDeleted(track));

        // If the current track was deleted and recording was active, start recording a new track
        if (restart) startNewTrack(track.getMarkerUid(), track.getCallsign(), null);
    }

    @Override
    @Nullable
    public Track getCurrentTrack(@Nonnull String markerUid)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(markerUid);
        return markerBreadcrumbStatus != null ? markerBreadcrumbStatus.currentTrack : null;
    }

    @Override
    @Nonnull
    public List<ITrack> getTracks(@Nonnull String markerUid)
    {
        return getTracks(markerUid, null, null);
    }

    @Override
    @Nonnull
    public List<ITrack> getTracks(@Nonnull String markerUid, @Nullable Instant minTime, @Nullable Instant maxTime)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(markerUid);
        return markerBreadcrumbStatus == null ? Collections.emptyList() : getFilteredTracks(markerBreadcrumbStatus.tracks, minTime, maxTime);
    }

    @Override
    @Nonnull
    public CompletableFuture<List<ITrack>> getTracks(@Nonnull String markerUid, @Nonnull ITakServerConnection serverConnection,
                                                     @Nullable Instant minTime, @Nullable Instant maxTime)
    {
        return serverTrackQueryService.downloadTracks(serverConnection, markerUid, minTime, maxTime)
                .thenApply(v -> getTracks(markerUid, minTime, maxTime));
    }

    @Override
    public long getTrackCount(@Nonnull String markerUid)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(markerUid);

        return markerBreadcrumbStatus != null ? markerBreadcrumbStatus.tracks.size() : 0;
    }

    @Override
    public int hideAllTracks()
    {
        final var count = new AtomicInteger();

        markerTrackStatusMap.values().stream()
                .flatMap(markerBreadcrumbStatus -> markerBreadcrumbStatus.tracks.stream())
                .filter(ITrack::isVisible)
                .map(Track.class::cast)
                .forEach(track -> {
                    track.setVisible(false);
                    count.incrementAndGet();
                });

        return count.get();
    }

    /**
     * Persists the current version of an {@link ITrack} and notifies listeners of the update.
     *
     * @param track The track to persist
     * @see TrackUpdated
     */
    public void save(@Nonnull ITrack track)
    {
        if (markerTrackStatusMap.get(track.getMarkerUid()).persistable) trackPersist.persist(track);
        trackUpdatedEvent.fire(new TrackUpdated(track));
    }

    /**
     * Updates the self marker's always-recording state.
     *
     * @since 5.5
     */
    private void updateSelfMarkerLogging()
    {
        if (selfMarkerUid == null) return;

        final var alwaysRecordBreadcrumbs = selfMarkerLoggingMonitor.getCurrentValue();
        final var trackStatus = getOrCreateTrackStatus(selfMarkerUid);
        trackStatus.alwaysRecordBreadcrumbs = alwaysRecordBreadcrumbs;

        //noinspection DataFlowIssue
        if (alwaysRecordBreadcrumbs && trackStatus.currentTrack == null
                && mapObjectFinder.findMapObjectByUid(selfMarkerUid).hasLocation())
        {
            startNewTrack(selfMarkerUid, PreferenceEntry.CALL_SIGN.getStringValue(), null, false);
        }
    }

    /**
     * Starts a new track for specified {@link gov.tak.api.maps.Marker} and optionally begins recording
     * {@link IBreadcrumb IBreadcrumbs}.
     *
     * @param markerUid The {@link gov.tak.api.maps.Marker Marker's} UID
     * @param callsign The callsign associated with the {@link gov.tak.api.maps.Marker} or {@code null} if not applicable
     * @param trackName The new {@link ITrack ITrack's} name or {@code null} if the default name should be used
     * @param startRecording Pass {@code true} if track creation should also trigger recording
     * @throws IllegalArgumentException if no {@link gov.tak.api.maps.Marker} is associated with the {@code markerUid}
     * @since 5.5
     */
    public void startNewTrack(@Nonnull String markerUid, @Nullable String callsign, @Nullable String trackName, boolean startRecording)
    {
        final var marker = getMarker(markerUid);
        final var markerBreadcrumbStatus = getOrCreateTrackStatus(markerUid);

        if (callsign == null)
        {
            final var markerCallsign = marker.getMetaString(StandardAttributeNames.CALLSIGN, "");
            // Even though getMetaString is tagged @Nonnull, calling it on test mocks can still return null.
            if (!StringUtils.isBlank(markerCallsign)) callsign = markerCallsign;
        }

        final var now = Instant.now();
        final var track = new Track(UUID.randomUUID().toString(), markerUid,
                trackName != null ? trackName : getDefaultTrackName(now), callsign != null ? callsign : "", now,
                markerBreadcrumbStatus.getNextColor());
        if (markerBreadcrumbStatus.persistable) trackPersist.persist(track);

        markerBreadcrumbStatus.tracks.add(track);
        markerBreadcrumbStatus.currentTrack = track;
        markerBreadcrumbStatus.currentTrackBreadcrumbs.clear();
        markerBreadcrumbStatus.lastTrackBreadcrumb = null;
        if (startRecording) markerBreadcrumbStatus.recordBreadcrumbs = true;
        markerBreadcrumbStatus.trailColor = getTrailBreadcrumbColor(marker);

        trackCreatedEvent.fire(new TrackCreated(track));

        // Add initial breadcrumb
        if (marker.hasLocation()) addBreadcrumb(marker);
    }

    /**
     * Adds a new breadcrumb based on the {@link Marker marker's} current location if the location passes the required
     * criteria (temporal, location, etc.).
     *
     * @param marker The marker
     */
    private void addBreadcrumb(@Nonnull Marker marker)
    {
        final MarkerBreadcrumbStatus markerBreadcrumbStatus = markerTrackStatusMap.get(marker.getUid());

        if (markerBreadcrumbStatus == null) return;

        if (marker.isSelfMarker() && selfMarkerLoggingMonitor.getCurrentValue() && markerBreadcrumbStatus.currentTrack == null)
        {
            startNewTrack(selfMarkerUid, PreferenceEntry.CALL_SIGN.getStringValue(), null, false);
            return;
        }

        final GeoPointMetaData geoPointMetaData = marker.getGeoPointMetaData();
        final GeoPoint geoPoint = geoPointMetaData.get();
        final Track track = markerBreadcrumbStatus.currentTrack;

        // Update the last breadcrumb's bearing
        if (markerBreadcrumbStatus.lastTrackBreadcrumb != null && track != null)
        {
            // Update the last track breadcrumb
            final String markerUid = marker.getUid();
            updateBearing(markerBreadcrumbStatus.lastTrackBreadcrumb, geoPoint);
            if (markerBreadcrumbStatus.persistable) breadcrumbPersist.persist(markerBreadcrumbStatus.lastTrackBreadcrumb);

            // Update the last trail breadcrumb, which may be different from the last track breadcrumb due to filtering
            updateBearing(markerBreadcrumbStatus.lastTrailBreadcrumb, geoPoint);

            // Refresh the modified track breadcrumb
            trackBreadcrumbsUpdatedEvent.fire(new TrackBreadcrumbsUpdated(track, List.of(markerBreadcrumbStatus.lastTrackBreadcrumb)));

            // Refresh the last trail breadcrumb on the map; always do this after adding the corresponding track breadcrumb so it's rendered on top
            trailBreadcrumbUpdatedEvent.fire(new TrailBreadcrumbUpdated(markerUid, markerBreadcrumbStatus.lastTrailBreadcrumb));
        }

        // Only create new breadcrumbs if recording and the point passes the criteria
        if (shouldCreateBreadcrumb(markerBreadcrumbStatus, geoPoint))
        {
            // Create and persist a track breadcrumb
            final Instant now = Instant.now();

            //noinspection DataFlowIssue - shouldCreateBreadcrumb() ensures track is not null
            final Breadcrumb trackBreadcrumb = new Breadcrumb(geoPointMetaData, now, null,
                    marker.hasSpeed() ? marker.getSpeedInMps() : null, track.getUid());

            markerBreadcrumbStatus.currentTrackBreadcrumbs.add(trackBreadcrumb);
            markerBreadcrumbStatus.lastTrackBreadcrumb = trackBreadcrumb;
            if (markerBreadcrumbStatus.persistable) breadcrumbPersist.persist(trackBreadcrumb);

            // Update and persist the breadcrumb
            track.update(trackBreadcrumb);
            if (markerBreadcrumbStatus.persistable) trackPersist.persist(track);

            // Optionally add to the rendered trail if enough time has passed
            final boolean addToTrail = markerBreadcrumbStatus.lastTrailBreadcrumb == null
                    || ChronoUnit.MILLIS.between(markerBreadcrumbStatus.lastTrailBreadcrumb.getTime(), now) > MILLISECONDS_BETWEEN_TRAIL_BREADCRUMBS
                    || platformUnderTest; // don't enforce a time limit during testing

            trackBreadcrumbsUpdatedEvent.fire(new TrackBreadcrumbsUpdated(track, List.of(trackBreadcrumb)));

            // Always do this after adding the corresponding track breadcrumb so it's rendered on top
            if (addToTrail) addTrailBreadcrumb(marker.getUid(), markerBreadcrumbStatus, trackBreadcrumb);
        }
    }

    /**
     * Adds a new breadcrumb to the trail, removes stale breadcrumbs and updates opacity as needed.
     *
     * @param markerUid The marker's UID
     * @param markerBreadcrumbStatus The marker's status object
     * @param trackBreadcrumb The breadcrumb to add
     */
    private void addTrailBreadcrumb(@Nonnull String markerUid, @Nonnull MarkerBreadcrumbStatus markerBreadcrumbStatus,
                                    @Nonnull Breadcrumb trackBreadcrumb)
    {
        // Add the new breadcrumb
        final var trailBreadcrumb = new Breadcrumb(trackBreadcrumb);
        markerBreadcrumbStatus.trailBreadcrumbs.addLast(trailBreadcrumb);
        markerBreadcrumbStatus.lastTrailBreadcrumb = trailBreadcrumb;

        trailBreadcrumbAddedEvent.fire(new TrailBreadcrumbAdded(markerUid, markerBreadcrumbStatus.lastTrailBreadcrumb,
                markerBreadcrumbStatus.trailColor));

        final var breadcrumbsToRemove = markerBreadcrumbStatus.trailBreadcrumbs.size() - PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.getIntegerValue();

        // Remove the oldest breadcrumbs if the max has been exceeded
        if (breadcrumbsToRemove > 0)
        {
            var removed = new ArrayList<IBreadcrumb>();
            var count = 0;

            while (count++ < breadcrumbsToRemove) removed.add(markerBreadcrumbStatus.trailBreadcrumbs.removeFirst());

            trailBreadcrumbRemovedEvent.fire(new TrailBreadcrumbsRemoved(markerUid, removed));
        }

        trailBreadcrumbsUpdatedEvent.fire(new TrailBreadcrumbsUpdated(markerUid)); // Fire after all modifications are complete
    }

    /**
     * Updates a {@link Breadcrumb breadcrumb's} based on the provided next {@link GeoPoint point}.
     * 
     * @param breadcrumb The breadcrumb to update
     * @param geoPoint The next point
     */
    private void updateBearing(@Nonnull Breadcrumb breadcrumb, @Nonnull GeoPoint geoPoint)
    {
        final double bearing = GeoCalculations.bearing(breadcrumb.getGeoPointMetaData().get(), geoPoint);
        breadcrumb.setBearing(bearing);
    }

    /**
     * Creates a default track name based on the current time the track was created. For example,
     * "track_2024-10-30T17-41-29Z"
     *
     * @param instant The time the track was created
     * @return The name to use for the track
     * @since 5.5
     */
    public String getDefaultTrackName(@Nonnull Instant instant)
    {
        return PreferenceEntry.BREADCRUMBS_TRACK_TITLE_PREFIX.getStringValue()
                + instant.with(ChronoField.NANO_OF_SECOND, 0).toString()
                        .replace(':', '-');
    }

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @return The {@link gov.tak.api.maps.Marker marker's} status object
     */
    private MarkerBreadcrumbStatus getOrCreateTrackStatus(@Nonnull String markerUid)
    {
        return markerTrackStatusMap.computeIfAbsent(markerUid, uid -> new MarkerBreadcrumbStatus(!getMarker(uid).isTransient()));
    }

    /**
     * Returns a boolean indicating if a {@link IBreadcrumb} should be created for the point. This only checks the 
     * basic criteria for track breadcrumbs. Trail breadcrumbs may apply additional criteria.
     * 
     * @param markerBreadcrumbStatus The {@link gov.tak.api.maps.Marker marker's} current {@link ITrack} info
     * @param geoPoint The new point
     * @return {@code true} if a breadcrumb should be created for the point
     */
    private boolean shouldCreateBreadcrumb(@Nullable MarkerBreadcrumbStatus markerBreadcrumbStatus, @Nonnull GeoPoint geoPoint)
    {
        return geoPoint.isValid() &&
                markerBreadcrumbStatus != null && markerBreadcrumbStatus.currentTrack != null
                && (markerBreadcrumbStatus.recordBreadcrumbs || markerBreadcrumbStatus.alwaysRecordBreadcrumbs)
                && (markerBreadcrumbStatus.lastTrackBreadcrumb == null
                        || GeoCalculations.slantDistance(
                                markerBreadcrumbStatus.lastTrackBreadcrumb.getGeoPointMetaData().get(), geoPoint)
                                >= PreferenceEntry.BREADCRUMBS_DISTANCE_THRESHOLD.getIntegerValue());
    }

    /**
     * Returns a copy of the provided list of {@link ITrack tracks} filtered by the specified time boundaries.
     *
     * @param tracks The complete list of {@link ITrack tracks}
     * @param minTime The min time or {@code null} if there's no min time
     * @param maxTime The max time or {@code null} if there's no max time
     * @return A list containing the tracks that fall within the time boundaries
     */
    private List<ITrack> getFilteredTracks(@Nonnull List<ITrack> tracks, @Nullable Instant minTime, @Nullable Instant maxTime)
    {
        Stream<ITrack> stream = tracks.stream();

        if (minTime != null) stream = stream.filter(track -> track.getStartTime().isAfter(minTime));
        if (maxTime != null) stream = stream.filter(track -> track.getStartTime().isBefore(maxTime));

        return stream.toList();
    }

    /**
     * @param marker The marker
     * @return The color to use for the marker's breadcrumb trail
     */
    private int getTrailBreadcrumbColor(@Nonnull Marker marker)
    {
        // Check explicit crumb color
        if (marker.hasMetaValue("crumbColor")) return marker.getMetaInteger("crumbColor", DEFAULT_BREADCRUMB_COLOR);

        // Check team color
        if (marker.hasMetaValue(StandardAttributeNames.TEAM))
        {
            return TeamColorMapper.teamToColor(marker.getMetaString(StandardAttributeNames.TEAM, ""));
        }

        // Get the predominant icon color or fallback on default
        final BufferedImage image = marker.getIconImage();
        return image != null
                // Ignore pixels that are 50% transparent (or more); make resulting color opaque
                ? ImageUtils.getPredominantColor(image, DEFAULT_BREADCRUMB_COLOR, 128) | 0xFF000000
                : DEFAULT_BREADCRUMB_COLOR;
    }

    /**
     * Stores a {@link gov.tak.api.maps.Marker marker's} {@link ITrack tracks} and status.
     */
    private static class MarkerBreadcrumbStatus
    {
        private boolean recordBreadcrumbs;
        private boolean alwaysRecordBreadcrumbs;
        private final List<ITrack> tracks = new ArrayList<>();
        private Track currentTrack;
        // Track breadcrumbs are mutable while part of the current track; caching ensures single instances of the
        // database objects
        private final List<IBreadcrumb> currentTrackBreadcrumbs = new ArrayList<>();
        private Breadcrumb lastTrackBreadcrumb;
        // In-memory only collection used to manage the transient breadcrumb trail
        private final Deque<IBreadcrumb> trailBreadcrumbs = new ArrayDeque<>();
        private final boolean persistable;
        private Breadcrumb lastTrailBreadcrumb;
        private int trackColorOffset = -1;
        private int trailColor = Color.BLUE.getRGB();

        private MarkerBreadcrumbStatus(boolean persistable)
        {
            this.persistable = persistable;
        }

        /**
         * @return The next {@link Marker marker's} next {@link ITrack} color
         */
        private int getNextColor()
        {
            if (!PreferenceEntry.BREADCRUMBS_AUTO_ROTATE_COLORS.getBooleanValue())
            {
                return TakColor.valueOf(PreferenceEntry.BREADCRUMBS_DEFAULT_TRACK_COLOR.getStringValue().toUpperCase()).colorValue;
            }

            final List<TakColor> trackColors = TakColor.getTrackPalette();
            final var colorCount = trackColors.size();

            // Restore previous offset
            if (trackColorOffset == -1 && !tracks.isEmpty())
            {
                final var lastColor = tracks.get(tracks.size() - 1).getColor();

                for (trackColorOffset = 0; trackColorOffset < colorCount; trackColorOffset++)
                {
                    if (trackColors.get(trackColorOffset).colorValue == lastColor) break;
                }
            }

            // Increment and bound
            trackColorOffset = ++trackColorOffset % colorCount;

            return trackColors.get(trackColorOffset).colorValue;
        }
    }
}
