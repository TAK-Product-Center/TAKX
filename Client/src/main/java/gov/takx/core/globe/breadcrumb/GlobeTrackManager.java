package gov.takx.core.globe.breadcrumb;

import android.graphics.Color;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.PatternStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrackDeleted;
import gov.takx.api.breadcrumb.event.TrackUpdated;
import gov.takx.api.breadcrumb.event.TrackVisibilityChanged;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the rendering of {@link gov.tak.api.maps.Marker} {@link ITrack tracks}. A {@link ITrack} is a set of
 * {@link IBreadcrumb breadcrumbs} that may be rendered as individual points or as a path.
 *
 * @since 5.3
 */
@ApplicationScoped
public class GlobeTrackManager extends AGlobeBreadcrumbManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TRACK_START_ICON = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "track_start.png").toString();
    private static final String TRACK_END_ICON = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "track_end.png").toString();
    private static final int TRACK_ENDPOINT_ICON_SIZE = 32;
    private static final float PATH_STROKE_WIDTH = 4f;
    // pattern multiplier (number of times each pattern bit will be repeated)
    private static final short DASHED_STIPPLE_FACTOR = 6;
    // defines the pattern: 0x5555 => 101010101010101 (dashes)
    private static final short DASHED_STIPPLE_PATTERN = (short) 0x5555;
    private final Map<String, GlobeTrack> trackUidToGlobeTrackMap = new ConcurrentHashMap<>();

    @Inject
    GlobeTrackManager(ISharedFeatureDataStore dataStore, IBreadcrumbManager breadcrumbManager)
    {
        super(dataStore, breadcrumbManager);
    }

    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    void onProjectClosed(@Observes ProjectClosed ignore)
    {
        lockAndExecute(() -> {
            for (GlobeTrack globeTrack : trackUidToGlobeTrackMap.values())
            {
                dataStore.deleteFeatureSet(globeTrack.arrowsFsid);
                dataStore.deleteFeatureSet(globeTrack.pathFsid);
                dataStore.deleteFeatureSet(globeTrack.endpointsFsid);
            }
            trackUidToGlobeTrackMap.clear();
        }, "clear");
    }

    @Override
    Set<Long> getArrowFeatureSetIds()
    {
        return trackUidToGlobeTrackMap.values().stream().mapToLong(gt -> gt.arrowsFsid).boxed().collect(Collectors.toSet());
    }

    /**
     * Hides and displays a {@link gov.takx.core.persist.breadcrumb.Track}, creating its {@link FeatureSet feature sets}
     * and {@link Feature features} if necessary.
     *
     * @param event The event containing data about the {@link gov.takx.core.persist.breadcrumb.Track} and its visibility
     */
    @VisibleForTesting
    void trackVisibilityChanged(@Observes TrackVisibilityChanged event)
    {
        final ITrack track = event.track();

        if (track.getBreadcrumbCount() == 0) return;

        lockAndExecute(() -> setTrackVisible(trackUidToGlobeTrackMap.get(track.getUid()), track.isVisible()),
                "set track visibility");
    }

    @VisibleForTesting
    void trackBreadcrumbsUpdated(@Observes TrackBreadcrumbsUpdated event)
    {
        final var track = event.track();
        final var trackUid = track.getUid();

        lockAndExecute(() -> {
            final var globeTrack = trackUidToGlobeTrackMap.computeIfAbsent(trackUid, uid -> new GlobeTrack(track));

            synchronized (globeTrack)
            {
                if (globeTrack.arrowsFsid == 0) // New track
                {
                    initialize(globeTrack, event.breadcrumbs());
                    return;
                }
            }

            updateBreadcrumbs(globeTrack, event.breadcrumbs());
        }, "update track breadcrumb");
    }

    /**
     * Updates a {@link gov.takx.core.persist.breadcrumb.Track track's} {@link Feature features}.
     *
     * @param event The event containing data about the {@link gov.takx.core.persist.breadcrumb.Track} update
     */
    @VisibleForTesting
    void trackUpdated(@Observes TrackUpdated event)
    {
        final GlobeTrack globeTrack = trackUidToGlobeTrackMap.get(event.track().getUid());

        // may be null if not rendered
        if (globeTrack != null) lockAndExecute(() -> updateTrack(globeTrack), "update track");
    }

    /**
     * Deletes a {@link gov.takx.core.persist.breadcrumb.Track track's} {@link Feature}.
     *
     * @param event The event containing data about the deleted track
     */
    @VisibleForTesting
    void trackDeleted(@Observes TrackDeleted event)
    {
        final GlobeTrack globeTrack = trackUidToGlobeTrackMap.remove(event.track().getUid());

        if (globeTrack != null) // may be null if not rendered
        {
            lockAndExecute(() -> {
                dataStore.deleteFeatureSet(globeTrack.arrowsFsid);
                dataStore.deleteFeatureSet(globeTrack.pathFsid);
                dataStore.deleteFeatureSet(globeTrack.endpointsFsid);
            }, "delete track");
        }
    }

    /**
     * Initializes the {@link FeatureSet feature sets} and {@link Feature features} required to display {@link ITrack},
     * adds the {@link ITrack track's} {@link IBreadcrumb} {@link Feature features}.
     *
     * @param globeTrack The globe track to be initialized
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    private void initialize(@Nonnull GlobeTrack globeTrack, @Nonnull List<? extends IBreadcrumb> breadcrumbs) throws DataStoreException
    {
        final var track = globeTrack.track;
        final var trackUid = track.getUid();
        final var color = track.getColor();

        if (breadcrumbs.isEmpty()) throw new IllegalArgumentException("The track has no breadcrumbs");

        // Add the endpoints feature set and features
        final FeatureSet endpointsFeatureSet = createFeatureSet("Track " + trackUid + " Endpoints");
        globeTrack.endpointsFsid = dataStore.insertFeatureSet(endpointsFeatureSet);
        final Feature startFeature = createBreadcrumbFeature(globeTrack.endpointsFsid, breadcrumbs.get(0), TRACK_START_ICON,
                color, TRACK_ENDPOINT_ICON_SIZE, 0d);
        final Feature endFeature = createBreadcrumbFeature(globeTrack.endpointsFsid, breadcrumbs.get(breadcrumbs.size() - 1),
                TRACK_END_ICON, color, TRACK_ENDPOINT_ICON_SIZE, 0d);
        globeTrack.startIconFid = dataStore.insertFeature(startFeature);
        globeTrack.endIconFid = dataStore.insertFeature(endFeature);

        // Add the path feature set and feature
        final FeatureSet pathFeatureSet = createFeatureSet("Track " + trackUid + " Path");
        globeTrack.pathFsid = dataStore.insertFeatureSet(pathFeatureSet);
        final Feature pathFeature = new Feature(globeTrack.pathFsid, trackUid, getTrackPathGeometry(breadcrumbs),
                getTrackPathStyle(track), null);
        globeTrack.pathFid = dataStore.insertFeature(pathFeature);

        // Add the breadcrumb arrows feature set and features for existing breadcrumbs
        final FeatureSet trackArrrowsFeatureSet = createFeatureSet("Track " + trackUid + " arrows");
        globeTrack.arrowsFsid = dataStore.insertFeatureSet(trackArrrowsFeatureSet);

        for (IBreadcrumb breadcrumb : breadcrumbs)
        {
            final long fid = addBreadcrumbArrow(globeTrack.arrowsFsid, breadcrumb, track.getColor());
            globeTrack.breadcrumbUidToPointFidMap.put(breadcrumb.getUid(), fid);
        }

        final var visible = track.isVisible();
        final var isArrows = track.getStyle() == ITrack.Style.Arrow;

        dataStore.setFeatureSetVisible(globeTrack.endpointsFsid, visible); // endpoints
        dataStore.setFeatureSetVisible(isArrows ? globeTrack.arrowsFsid : globeTrack.pathFsid, visible); // selected
        dataStore.setFeatureSetVisible(isArrows ? globeTrack.pathFsid : globeTrack.arrowsFsid, false); // unselected
    }

    /**
     * Updates or adds a {@link ITrack track} {@link IBreadcrumb} {@link Feature features}. If no {@link Feature} for a
     * {@link IBreadcrumb} exists, a new one will be added. Otherwise, its existing {@link Feature} will be updated. The
     * caller should handle {@link FeatureDataStore4 data store} locking.
     *
     * @param globeTrack The track to update
     * @param breadcrumbs The breadcrumbs that were updated
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    private void updateBreadcrumbs(@Nonnull GlobeTrack globeTrack, @Nonnull List<? extends IBreadcrumb> breadcrumbs) throws DataStoreException
    {
        for (var breadcrumb : breadcrumbs) updateBreadcrumb(globeTrack, breadcrumb);
    }

    /**
     * Updates or adds a {@link ITrack track} {@link IBreadcrumb} {@link Feature feature}. If no {@link Feature} for the
     * {@link IBreadcrumb} exists, a new one will be added. Otherwise, its existing {@link Feature} will be updated. The
     * caller should handle {@link FeatureDataStore4 data store} locking.
     *
     * @param globeTrack The track to update
     * @param breadcrumb The breadcrumb that was updated
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    private void updateBreadcrumb(@Nonnull GlobeTrack globeTrack, @Nonnull IBreadcrumb breadcrumb) throws DataStoreException
    {
        final ITrack track = globeTrack.track;
        final int color = track.getColor();
        final String breadcrumbUid = breadcrumb.getUid();
        Long fid = globeTrack.breadcrumbUidToPointFidMap.get(breadcrumbUid);

        // Add a new end point
        if (fid == null)
        {
            // Add new arrow
            fid = addBreadcrumbArrow(globeTrack.arrowsFsid, breadcrumb, color);
            globeTrack.breadcrumbUidToPointFidMap.put(breadcrumbUid, fid);

            // Update path points
            dataStore.updateFeature(globeTrack.pathFid, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY,
                    track.getUid(), getTrackPathGeometry(breadcrumbManager.getTrackBreadcrumbs(track)), null,
                    null, 0);

            // Update end marker location
            dataStore.updateFeature(globeTrack.endIconFid, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY,
                    breadcrumbUid, getPointGeometry(breadcrumb.getGeoPointMetaData().get()), null,
                    null, 0);
        }
        // Update an existing breadcrumb
        else
        {
            // A single point's style changed (e.g. heading); update the feature's style
            updateBreadcrumbArrowStyle(fid, breadcrumb, color);
        }
    }

    /**
     * Updates a {@link ITrack track's} features, switching between arrow and path rendering or updating the track's
     * style.
     * 
     * @param globeTrack The track to update
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    private void updateTrack(@Nonnull GlobeTrack globeTrack) throws DataStoreException
    {
        final ITrack track = globeTrack.track;
        final boolean showArrows = track.getStyle() == ITrack.Style.Arrow;

        // Track style changed; show/hide arrows/path
        if (globeTrack.showArrows != showArrows)
        {
            globeTrack.showArrows = showArrows;

            if (globeTrack.visible)
            {
                dataStore.setFeatureSetVisible(globeTrack.arrowsFsid, showArrows);
                dataStore.setFeatureSetVisible(globeTrack.pathFsid, !showArrows);
            }
        }
        // Color, size, etc. changed; update all breadcrumbs and the path
        else
        {
            // Update the track breadcrumbs' style
            final List<IBreadcrumb> breadcrumbs = breadcrumbManager.getTrackBreadcrumbs(track);

            for (IBreadcrumb b : breadcrumbs)
            {
                final Long fid = globeTrack.breadcrumbUidToPointFidMap.get(b.getUid());

                if (fid == null)
                {
                    logger.warn("Unknown breadcrumb found during track update");
                    continue;
                }

                updateBreadcrumbArrowStyle(fid, b, track.getColor());
            }

            // Update the track path style
            dataStore.updateFeature(globeTrack.pathFid,
                    FeatureDataStore2.PROPERTY_FEATURE_STYLE, track.getUid(), null, getTrackPathStyle(track),
                    null, 0);

            // Update the start point style
            final IBreadcrumb firstBreadcrumb = breadcrumbs.get(0);
            updateBreadcrumbStyle(globeTrack.startIconFid, firstBreadcrumb.getUid(), TRACK_START_ICON, track.getColor(),
                    TRACK_ENDPOINT_ICON_SIZE, 0d);

            // Update the end point style
            final IBreadcrumb lastBreadcrumb = breadcrumbs.get(breadcrumbs.size() - 1);
            updateBreadcrumbStyle(globeTrack.endIconFid, lastBreadcrumb.getUid(), TRACK_END_ICON, track.getColor(),
                    TRACK_ENDPOINT_ICON_SIZE, 0d);
        }
    }

    /**
     * Update a {@link GlobeTrack globe track's} visibility.
     *
     * @param globeTrack The track to update
     * @param visible {@code true} if the track should be visible
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    private void setTrackVisible(@Nonnull GlobeTrack globeTrack, boolean visible) throws DataStoreException
    {
        if (globeTrack.visible == visible) return;

        globeTrack.visible = visible;

        dataStore.setFeatureSetVisible(globeTrack.endpointsFsid, visible);
        dataStore.setFeatureSetVisible(globeTrack.arrowsFsid, visible && globeTrack.showArrows);
        dataStore.setFeatureSetVisible(globeTrack.pathFsid, visible && !globeTrack.showArrows);
    }

    /**
     * @param track The track to render
     * @return The style to use when rendering the track
     */
    private Style getTrackPathStyle(@Nonnull ITrack track)
    {
        Style pathStyle;

        if (track.getStyle() == ITrack.Style.Dash)
        {
            final int color = track.getColor();
            float r = Color.red(color) / 255.f;
            float g = Color.green(color) / 255.f;
            float b = Color.blue(color) / 255.f;
            float a = Color.alpha(color) / 255.f;

            pathStyle = new PatternStrokeStyle(DASHED_STIPPLE_FACTOR, DASHED_STIPPLE_PATTERN, r, g, b, a, PATH_STROKE_WIDTH);
        } else
        {
            pathStyle = new BasicStrokeStyle(track.getColor(), PATH_STROKE_WIDTH);
        }

        return FeatureUtility.makeNoLabelStyle(pathStyle);
    }

    /**
     * Stores {@link ITrack} rendering state.
     */
    private static class GlobeTrack
    {
        final ITrack track;
        final Map<String, Long> breadcrumbUidToPointFidMap = new HashMap<>();

        long arrowsFsid;
        long endpointsFsid;
        long startIconFid;
        long endIconFid;
        long pathFsid;
        long pathFid;
        boolean showArrows = true; // default is to show arrows
        boolean visible;

        /**
         * @param track The track to display
         */
        GlobeTrack(@Nonnull ITrack track)
        {
            this.track = track;
        }
    }
}
