package gov.takx.core.globe;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.core.map.layer.feature.datastore.SharedFeatureDataStore;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.features.IFeatureProvider;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;

/**
 * Displays a point on all available globes that corresponds to a user's selection in the
 * {@link gov.takx.core.view.gui.maptools.ElevationProfilePanel}.
 *
 * @since 5.6
 */
@ApplicationScoped
public class ElevationProfileLocationManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String LOCATION_ICON = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "elevation_profile_location.png").toString();
    private static final String LOCATION_FEATURE_NAME = "Elevation Profile Location";

    private final FeatureDataStore4 dataStore;
    private long featureSetId;
    private long featureId;

    @Inject
    ElevationProfileLocationManager(@SharedFeatureDataStore FeatureDataStore4 dataStore)
    {
        this.dataStore = dataStore;
    }

    @SuppressWarnings("unused")
    private void startServices(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        lockAndExecute(this::init, "init");
    }

    @SuppressWarnings("unused")
    private void stopServices(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        lockAndExecute(() -> dataStore.deleteFeatureSet(featureSetId), "stop");
    }

    /**
     * @param visible Pass {@code true} to set the point icon visible, {@code false} to hide it
     */
    public void setVisible(boolean visible)
    {
        lockAndExecute(() -> dataStore.setFeatureSetVisible(featureSetId, visible), "set visibility");
    }

    /**
     * @param geoPoint The location to place the point icon
     */
    public void setLocation(@Nonnull IGeoPoint geoPoint)
    {
        final var point = new Point(geoPoint.getLongitude(), geoPoint.getLatitude(), geoPoint.getAltitude());

        lockAndExecute(() -> dataStore.updateFeature(featureId, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY,
                LOCATION_FEATURE_NAME, point, null, null, 0),
                "set location");
    }

    /**
     * Creates a feature set and feature for the location icon.
     *
     * @throws DataStoreException thrown on insertion and visibility update errors
     */
    private void init() throws DataStoreException
    {
        final var featureSet = new FeatureSet(LOCATION_FEATURE_NAME, IFeatureProvider.ALL_GLOBES_GROUP_NAME, LOCATION_FEATURE_NAME, Double.MAX_VALUE, 0);
        featureSetId = dataStore.insertFeatureSet(featureSet);
        dataStore.setFeatureSetVisible(featureSetId, false);

        final var style = new IconPointStyle(Color.CYAN.getRGB(), LOCATION_ICON, 12f, 12f, 0, 0, 0f, true);
        final var compositeStyle = FeatureUtility.makeNoLabelStyle(style);
        final var feature = new Feature(featureSetId, LOCATION_FEATURE_NAME, null, compositeStyle, null);
        featureId = dataStore.insertFeature(feature);
    }

    /**
     * Handles feature data store locking and exception handling for a runnable.
     *
     * @param runnable The runnable
     * @param actionName The name of the action to log if errors occur
     */
    private void lockAndExecute(@Nonnull ICheckedRunnable runnable, @Nonnull String actionName)
    {
        try
        {
            dataStore.acquireModifyLock(false);
            runnable.run();
        } catch (DataStoreException | InterruptedException e)
        {
            logger.error("Failed to update datastore while attempting to {}", actionName, e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Runnable-like functional interface that re-throws {@link DataStoreException}.
     */
    @FunctionalInterface
    private interface ICheckedRunnable
    {
        void run() throws DataStoreException;
    }
}
