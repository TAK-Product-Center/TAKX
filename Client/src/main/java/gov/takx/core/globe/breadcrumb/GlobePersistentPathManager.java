package gov.takx.core.globe.breadcrumb;

import android.graphics.Color;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.maps.Marker;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.event.TrailBreadcrumbAdded;
import gov.takx.api.breadcrumb.event.TrailVisibilityChanged;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Manages the self {@link gov.takx.api.playback.IMarker marker's} persistent breadcrumb path.
 * 
 * @since 5.5
 * @see gov.takx.platform.preferences.PreferenceEntry#BREADCRUMBS_PERSISTENT_SELF_TRACKS
 */
@ApplicationScoped
public class GlobePersistentPathManager extends AGlobeBreadcrumbManager
{
    private static final String FEATURE_SET_NAME = "Persistent Self Marker Trail";
    private static final String FEATURE_NAME = "Persistent Self Marker Trail Path";
    private static final Style PATH_STYLE = FeatureUtility.makeNoLabelStyle(new BasicStrokeStyle(Color.BLUE, 4));

    private final List<IBreadcrumb> breadcrumbs = new ArrayList<>();
    private final IPreferenceMonitor<Boolean> preferenceMonitor =
            PreferenceEntry.BREADCRUMBS_PERSISTENT_SELF_TRACKS.newBooleanMonitor(this::setEnabled);

    private long pathFsid;
    private long pathFid;
    private boolean enabled; // tracks the user preference
    private boolean trailVisible; // path is only displayed if the marker's trail is visible
    @Nullable
    private String selfMarkerUid;

    @Inject
    GlobePersistentPathManager(ISharedFeatureDataStore dataStore, IBreadcrumbManager breadcrumbManager)
    {
        super(dataStore, breadcrumbManager);
    }

    @VisibleForTesting
    void mapObjectAdded(@Observes MapObjectAdded event)
    {
        final var mapObject = event.getMapObject();

        if (mapObject.isSelfMarker())
        {
            selfMarkerUid = mapObject.getUid();

            lockAndExecute(
                    () -> {
                        final FeatureSet pathFeatureSet = createFeatureSet(FEATURE_SET_NAME);
                        pathFsid = dataStore.insertFeatureSet(pathFeatureSet);

                        final Feature pathFeature = new Feature(pathFsid, FEATURE_NAME, null, PATH_STYLE, null);
                        pathFid = dataStore.insertFeature(pathFeature);
                    },
                    "initialize path");

            setEnabled(preferenceMonitor.getCurrentValue());
        }
    }

    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    void onProjectClosed(@Observes ProjectClosed ignored)
    {
        selfMarkerUid = null;
        breadcrumbs.clear();
        trailVisible = false;

        lockAndExecute(() -> dataStore.deleteFeatureSet(pathFsid), "delete path");
    }

    @VisibleForTesting
    void trailVisibilityChanged(@Observes TrailVisibilityChanged event)
    {
        if (event.markerUid().equals(selfMarkerUid))
        {
            trailVisible = event.visible();
            updateVisibility();
        }
    }

    /**
     * Adds a {@link Feature} representing a {@link gov.takx.core.persist.breadcrumb.Breadcrumb} to the self
     * {@link Marker marker's} persistent path {@link FeatureSet}.
     *
     * @param event The event containing data about the newly added {@link gov.takx.core.persist.breadcrumb.Breadcrumb}
     */
    @VisibleForTesting
    void trailBreadcrumbAdded(@Observes TrailBreadcrumbAdded event)
    {
        if (enabled && event.markerUid().equals(selfMarkerUid)) addBreadcrumb(event.breadcrumb());
    }

    @Override
    Set<Long> getArrowFeatureSetIds()
    {
        return Collections.emptySet();
    }

    /**
     * @param enabled Pass {@code true} if new breadcrumbs should be recorded, {@code false} if existing breadcrumbs
     *                should be cleared and new breadcrumbs should not be recorded
     */
    @VisibleForTesting
    void setEnabled(boolean enabled)
    {
        if (selfMarkerUid == null) return; // this method will be called again when the UID is available

        this.enabled = enabled;

        if (enabled)
        {
            // If the marker has breadcrumbs, add the last one to the new path
            final var breadcrumbs = breadcrumbManager.getTrailBreadcrumbs(selfMarkerUid);
            if (!breadcrumbs.isEmpty()) addBreadcrumb(breadcrumbs.get(breadcrumbs.size() - 1));
        } else
        {
            breadcrumbs.clear();
        }

        updateVisibility();
    }

    /**
     * @param breadcrumb The breadcrumb to add to the path
     */
    private void addBreadcrumb(@Nonnull IBreadcrumb breadcrumb)
    {
        breadcrumbs.add(breadcrumb);

        // Only update the geometry if it's being displayed
        if (enabled && trailVisible) lockAndExecute(() -> updatePath(getTrackPathGeometry(breadcrumbs)), "add breadcrumb");
    }

    /**
     * @param geometry The path geometry; pass {@code null} to clear the path
     */
    private void updatePath(@Nullable Geometry geometry) throws DataStoreException
    {
        dataStore.updateFeature(pathFid, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY, FEATURE_NAME, geometry, null,
                null, 0);

    }

    /**
     * Updates the path's visibility based on the {@link #enabled} status and the
     * {@link gov.takx.api.playback.IMarker marker} trail's visibility.
     */
    private void updateVisibility()
    {
        lockAndExecute(
                () -> {
                    final var visible = trailVisible && enabled;

                    // The path geometry isn't updated when it's invisible, so update it now
                    if (visible) updatePath(getTrackPathGeometry(breadcrumbs));

                    dataStore.setFeatureSetVisible(pathFsid, visible);
                },
                "update path visibility");
    }
}
