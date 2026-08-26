package gov.takx.core.globe.breadcrumb;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.maps.Marker;
import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbAdded;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbUpdated;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsRemoved;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrailColorUpdated;
import gov.takx.api.breadcrumb.event.TrailDeleted;
import gov.takx.api.breadcrumb.event.TrailVisibilityChanged;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/**
 * Manages the rendering of {@link Marker} trails. A trail is a set of
 * {@link gov.takx.core.persist.breadcrumb.Breadcrumb breadcrumbs} that are rendered as individual points.
 *
 * @since 5.3
 */
@ApplicationScoped
public class GlobeTrailManager extends AGlobeBreadcrumbManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<String, GlobeTrail> markerUidToGlobeTrailMap = new ConcurrentHashMap<>();

    @Inject
    GlobeTrailManager(ISharedFeatureDataStore dataStore, IBreadcrumbManager breadcrumbManager)
    {
        super(dataStore, breadcrumbManager);

        // We're relying on InMemoryFeatureDataStore to generate sequential feature ids
        if (!(dataStore instanceof InMemoryFeatureDataStore))
        {
            logger.warn("Unexpected data store type {}. Confirm the data store guarantees sequential FIDs or " +
                    "updateTrailColor may produce unexpected results", dataStore.getClass());
        }
    }

    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    void onProjectClosed(@Observes ProjectClosed ignore)
    {
        lockAndExecute(() -> {
            for (GlobeTrail globeTrail : markerUidToGlobeTrailMap.values())
            {
                dataStore.deleteFeatureSet(globeTrail.arrowsFsid);
            }
            markerUidToGlobeTrailMap.clear();
        }, "clear");
    }

    @Override
    Set<Long> getArrowFeatureSetIds()
    {
        return markerUidToGlobeTrailMap.values().stream().mapToLong(gt -> gt.arrowsFsid).boxed().collect(Collectors.toSet());
    }

    /**
     * Adds a {@link Feature} representing a {@link gov.takx.core.persist.breadcrumb.Breadcrumb} to the
     * {@link Marker marker's} trail {@link FeatureSet}.
     * 
     * @param event The event containing data about the newly added {@link gov.takx.core.persist.breadcrumb.Breadcrumb}
     */
    @VisibleForTesting
    void trailBreadcrumbAdded(@Observes TrailBreadcrumbAdded event)
    {
        final String markerUid = event.markerUid();

        lockAndExecute(() -> {
            final GlobeTrail globeTrail = markerUidToGlobeTrailMap.computeIfAbsent(markerUid, uid -> new GlobeTrail(event.trailColor()));

            synchronized (globeTrail)
            {
                if (globeTrail.arrowsFsid == 0)
                {
                    globeTrail.arrowsFsid = dataStore.insertFeatureSet(createFeatureSet(markerUid));
                    dataStore.setFeatureSetVisible(globeTrail.arrowsFsid, globeTrail.visible);
                }
            }

            final IBreadcrumb breadcrumb = event.breadcrumb();
            final String breadcrumbUid = breadcrumb.getUid();

            // Add the breadcrumb feature and store its FID
            final long fid = addBreadcrumbArrow(globeTrail.arrowsFsid, breadcrumb, globeTrail.color);
            globeTrail.breadcrumbUidToPointFidMap.put(breadcrumbUid, fid);
        }, "add trail breadcrumb");
    }

    /**
     * Updates a trail {@link gov.takx.core.persist.breadcrumb.Breadcrumb breadcrumb's} {@link Feature}.
     * 
     * @param event The event containing data about the updated {@link gov.takx.core.persist.breadcrumb.Breadcrumb}
     */
    @VisibleForTesting
    void trailBreadcrumbUpdated(@Observes TrailBreadcrumbUpdated event)
    {
        final GlobeTrail globeTrail = markerUidToGlobeTrailMap.get(event.markerUid());

        if (globeTrail != null)
        {
            final IBreadcrumb breadcrumb = event.breadcrumb();
            final Long fid = globeTrail.breadcrumbUidToPointFidMap.get(breadcrumb.getUid());

            if (fid != null)
            {
                lockAndExecute(() -> updateBreadcrumbArrowStyle(fid, breadcrumb, globeTrail.color),
                        "update trail breadcrumb");
            }
        }
    }

    /**
     * Updates breadcrumb opacities when trail breadcrumbs are added and/or removed.
     *
     * @param event The event containing info about the trail that changed
     * @since 5.4
     */
    @VisibleForTesting
    void trailBreadcrumbsUpdated(@Observes TrailBreadcrumbsUpdated event)
    {
        final GlobeTrail globeTrail = markerUidToGlobeTrailMap.get(event.markerUid());

        if (globeTrail != null)
        {
            final var count = globeTrail.breadcrumbUidToPointFidMap.size();
            final var maxTrailBreadcrumbs = PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS.getIntegerValue();

            // Recalculate opacities
            final var opacityIncrement = Math.max(1, 255 / maxTrailBreadcrumbs);
            final var alpha = new AtomicInteger(255 - (count * opacityIncrement)); // Start with the oldest feature's opacity
            final IntUnaryOperator increment = x -> x + opacityIncrement;

            lockAndExecute(() -> updateTrailColor(globeTrail.arrowsFsid,
                    color -> (color & 0x00FFFFFF) | (Math.min(255, alpha.getAndUpdate(increment)) << 24)), "update trail fade");
        }
    }

    /**
     * Updates the visibility of a trail's {@link gov.takx.core.persist.breadcrumb.Breadcrumb breadcrumbs}.
     *
     * @param event The event containing data about the updated trail
     */
    @VisibleForTesting
    void trailVisibilityChanged(@Observes TrailVisibilityChanged event)
    {
        final GlobeTrail globeTrail = markerUidToGlobeTrailMap.get(event.markerUid());
        final boolean visible = event.visible();

        if (globeTrail != null && globeTrail.visible != visible)
        {
            globeTrail.visible = visible;
            lockAndExecute(() -> dataStore.setFeatureSetVisible(globeTrail.arrowsFsid, visible),
                    "update trail visibility");
        }
    }

    /**
     * Removes a trail {@link gov.takx.core.persist.breadcrumb.Breadcrumb breadcrumb's} {@link Feature}.
     *
     * @param event The event containing data about the removed {@link gov.takx.core.persist.breadcrumb.Breadcrumb}
     */
    @VisibleForTesting
    void trailBreadcrumbsRemoved(@Observes TrailBreadcrumbsRemoved event)
    {
        final GlobeTrail globeTrail = markerUidToGlobeTrailMap.get(event.markerUid());

        if (globeTrail != null)
        {
            final var breadcrumbsToRemove = event.breadcrumbs();

            if (breadcrumbsToRemove.size() == 1)
            {
                final Long fid = globeTrail.breadcrumbUidToPointFidMap.remove(event.breadcrumbs().get(0).getUid());
                if (fid != null) lockAndExecute(() -> dataStore.deleteFeature(fid), "remove breadcrumb");
            }
            // This will only happen if the user adjusts the max breadcrumb preference
            else
            {
                final var ids = new HashSet<Long>();

                event.breadcrumbs().forEach(breadcrumb -> {
                    final Long fid = globeTrail.breadcrumbUidToPointFidMap.remove(breadcrumb.getUid());
                    if (fid != null) ids.add(fid);
                });

                if (!ids.isEmpty())
                {
                    final var params = new FeatureDataStore4.FeatureQueryParameters();
                    params.ids = ids;
                    lockAndExecute(() -> dataStore.deleteFeatures(params), "remove breadcrumbs");
                }
            }
        }
    }

    /**
     * Updates the trail when its style changes.
     *
     * @param event The event containing data about the updated trail.
     */
    @VisibleForTesting
    void trailColorUpdated(@Observes TrailColorUpdated event)
    {
        final var globeTrail = markerUidToGlobeTrailMap.get(event.markerUid());

        if (globeTrail != null)
        {
            globeTrail.color = event.color();
            // Maintain existing opacities
            lockAndExecute(() -> updateTrailColor(globeTrail.arrowsFsid, color -> (globeTrail.color & 0x00FFFFFF) | (color & 0xFF000000)),
                    "update trail color");
        }
    }

    /**
     * Removes a trail's {@link FeatureSet}.
     *
     * @param event The event containing data about the removed trail
     */
    @VisibleForTesting
    void trailDeleted(@Observes TrailDeleted event)
    {
        final GlobeTrail globeTrail = markerUidToGlobeTrailMap.remove(event.markerUid());

        if (globeTrail != null) lockAndExecute(() -> dataStore.deleteFeatureSet(globeTrail.arrowsFsid), "delete trail");
    }

    /**
     * Updates a trail's color.
     *
     * @param fsid The trail's {@link FeatureSet} id
     * @param colorFunction Function that takes the current feature color and returns the new color
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     * @since 5.4
     */
    void updateTrailColor(long fsid, Function<Integer, Integer> colorFunction)
            throws DataStoreException
    {
        final var parameters = new FeatureDataStore4.FeatureQueryParameters();
        parameters.order = Set.of(new FeatureDataStore4.FeatureQueryParameters.Order.ID()); // Oldest to newest
        parameters.featureSetFilter = new FeatureDataStore4.FeatureSetQueryParameters();
        parameters.featureSetFilter.ids = Collections.singleton(fsid);

        // Iterate through all the set's features and update each feature's color while maintaining its other options
        try (var featureCursor = dataStore.queryFeatures(parameters))
        {
            while (featureCursor.moveToNext())
            {
                final var feature = featureCursor.get();
                final var style = (IconPointStyle) ((CompositeStyle) feature.getStyle()).getStyle(0);
                final var color = colorFunction.apply(style.getColor());
                final var newStyle = new IconPointStyle(color, style.getIconUri(), style.getIconWidth(),
                        style.getIconHeight(), style.getIconAlignmentX(), style.getIconAligmnentY(),
                        style.getIconRotation(), style.isRotationAbsolute());

                dataStore.updateFeature(feature.getId(), FeatureDataStore4.PROPERTY_FEATURE_STYLE, feature.getName(),
                        null, FeatureUtility.makeNoLabelStyle(newStyle), null, 0);
            }
        }
    }

    /**
     * Stores trail rendering state.
     */
    private static class GlobeTrail
    {
        final Map<String, Long> breadcrumbUidToPointFidMap = new HashMap<>();
        long arrowsFsid;
        int color;
        boolean visible;

        GlobeTrail(int color)
        {
            this.color = color;
        }
    }
}
