package gov.takx.core.globe.breadcrumb;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.features.IFeatureProvider;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_SIZE;

/**
 * Base class that offers basic functionality for managing {@link FeatureSet feature sets} and {@link Feature features}
 * that display {@link IBreadcrumb breadcrumbs}.
 *
 * @since 5.3
 */
abstract class AGlobeBreadcrumbManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String FEATURE_SET_PROVIDER_NAME = MethodHandles.lookup().lookupClass().getName();
    private static final String ARROW_ICON = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "breadcrumb_arrow.png").toString();

    protected final FeatureDataStore4 dataStore;
    protected final IBreadcrumbManager breadcrumbManager;

    private int breadcrumbSize;

    /**
     * @param dataStore The data store that will hold the {@link FeatureSet feature sets} and {@link Feature features}
     * @param breadcrumbManager The manager that will provide access to {@link gov.takx.api.breadcrumb.ITrack} data
     */
    AGlobeBreadcrumbManager(@Nonnull FeatureDataStore4 dataStore, @Nonnull IBreadcrumbManager breadcrumbManager)
    {
        this.dataStore = dataStore;
        this.breadcrumbManager = breadcrumbManager;

        final var monitor = BREADCRUMBS_SIZE.newMonitor(BreadcrumbSize.class, bs -> updateBreadcrumbSizes(bs.getSize()));
        this.breadcrumbSize = monitor.getCurrentValue().getSize();
    }

    /**
     * Adds an arrow {@link Feature} for the provided {@link IBreadcrumb}.
     *
     * @param fsid The id of the {@link FeatureSet} that the new {@link Feature} should be added to
     * @param breadcrumb The breadcrumb to add
     * @param color The breadcrumb's color
     * @return The id of the {@link Feature} that was created for the {@link IBreadcrumb}
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    long addBreadcrumbArrow(long fsid, @Nonnull IBreadcrumb breadcrumb, int color)
            throws DataStoreException
    {
        // Create and add a feature for the breadcrumb
        final var feature = createBreadcrumbFeature(fsid, breadcrumb, ARROW_ICON, color, breadcrumbSize, breadcrumb.getBearing());
        return dataStore.insertFeature(feature);
    }

    /**
     * Updates a {@link IBreadcrumb} that is represented by an arrow icon.
     * 
     * @param fid The {@link IBreadcrumb breadcrumb's} {@link Feature} id
     * @param breadcrumb The {@link IBreadcrumb}
     * @param color The {@link IBreadcrumb breadcrumb's} color
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    void updateBreadcrumbArrowStyle(long fid, @Nonnull IBreadcrumb breadcrumb, int color)
            throws DataStoreException
    {
        updateBreadcrumbStyle(fid, breadcrumb.getUid(), ARROW_ICON, color, breadcrumbSize, breadcrumb.getBearing());
    }

    /**
     * @param fid The {@link IBreadcrumb} {@link Feature feature's} id
     * @param featureName The name to use for the feature (currently unused but required)
     * @param iconPath The icon to use for the {@link IBreadcrumb's} {@link Feature}
     * @param color The color to use for the {@link IBreadcrumb's} {@link Feature}
     * @param size The size to use for the {@link IBreadcrumb's} {@link Feature}
     * @param bearing The bearing to use for the {@link IBreadcrumb's} {@link Feature} or {@code null} if no bearing is
     *                being provided
     * @throws DataStoreException thrown on errors updating the {@link FeatureDataStore4 data store}
     */
    void updateBreadcrumbStyle(long fid, @Nonnull String featureName, @Nonnull String iconPath, int color, int size,
                               @Nullable Double bearing)
            throws DataStoreException
    {
        dataStore.updateFeature(fid, FeatureDataStore2.PROPERTY_FEATURE_STYLE, featureName, null,
                getBreadcrumbStyle(iconPath, color, size, bearing), null, 0);
    }

    /**
     * @param iconPath The path to the {@link IBreadcrumb breadcrumb's} icon
     * @param color   The {@link IBreadcrumb breadcrumb's} color
     * @param size    The {@link IBreadcrumb breadcrumb's} size
     * @param bearing The {@link IBreadcrumb breadcrumb's} bearing
     * @return The style to use when rendering the {@link IBreadcrumb}
     */
    Style getBreadcrumbStyle(@Nonnull String iconPath, int color, double size, @Nullable Double bearing)
    {
        final float rotation = bearing != null ? -bearing.floatValue() : 0f; // rotation is counter-clockwise
        final var iconPointStyle = new IconPointStyle(color, iconPath, (float) size, (float) size, 0, 0,
                rotation, true);

        return FeatureUtility.makeNoLabelStyle(iconPointStyle);
    }

    /**
     * Creates a new {@link FeatureSet feature set}.
     * 
     * @param name The {@link FeatureSet feature set} name. Required but currently unused.
     * @return A new feature set configured with default values 
     */
    @Nonnull
    FeatureSet createFeatureSet(@Nonnull String name)
    {
        return new FeatureSet(FEATURE_SET_PROVIDER_NAME, IFeatureProvider.ALL_GLOBES_GROUP_NAME, name, Double.MAX_VALUE, 0);
    }

    /**
     * @param fsid The {@link FeatureSet feature set's} id
     * @param breadcrumb The breadcrumb that will be rendered
     * @param iconPath The icon to use for the {@link IBreadcrumb's} {@link Feature}
     * @param color The color to use for the {@link IBreadcrumb's} {@link Feature}
     * @param size The size to use for the {@link IBreadcrumb's} {@link Feature}
     * @param bearing The bearing to use for the {@link IBreadcrumb's} {@link Feature} or {@code null} if no bearing is
     *                being provided
     * @return The breadcrumb feature
     */
    Feature createBreadcrumbFeature(long fsid, @Nonnull IBreadcrumb breadcrumb, @Nonnull String iconPath,
                                    int color, double size, @Nullable Double bearing)
    {
        return new Feature(fsid, breadcrumb.getUid(), getPointGeometry(breadcrumb.getGeoPointMetaData().get()),
                getBreadcrumbStyle(iconPath, color, size, bearing), null);
    }

    /**
     * @param geoPoint The point to render
     * @return The geometry to use when rendering the point
     */
    Geometry getPointGeometry(@Nonnull GeoPoint geoPoint)
    {
        return new Point(geoPoint.getLongitude(), geoPoint.getLatitude(), geoPoint.getAltitude());
    }

    /**
     * @param breadcrumbs The {@link IBreadcrumb breadcrumbs} to render
     * @return The geometry to use when rendering the track as a path (solid or dashed)
     * @since 5.5 (moved from GlobeTrackManager)
     */
    @Nonnull
    Geometry getTrackPathGeometry(@Nonnull List<? extends IBreadcrumb> breadcrumbs)
    {
        // Map breadcrumbs to GeoPoints
        final List<GeoPoint> geoPoints = breadcrumbs.stream()
                .map(breadcrumb -> breadcrumb.getGeoPointMetaData().get())
                .toList();

        final LineString lineString = new LineString(3);
        final double[] previousLatLonAlt = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

        geoPoints.forEach(point -> {
            final double latitude = point.getLatitude();
            final double longitude = point.getLongitude();
            final double hae = !point.hasElevation() ? 0 : point.getElevation() + ElevationManager.getGeoidHeight(latitude, longitude);

            if (latitude != previousLatLonAlt[0] || longitude != previousLatLonAlt[1] || hae != previousLatLonAlt[2])
            {
                lineString.addPoint(longitude, latitude, hae);
            }

            previousLatLonAlt[0] = latitude;
            previousLatLonAlt[1] = longitude;
            previousLatLonAlt[2] = hae;
        });

        return lineString;
    }

    /**
     * Handles feature data store locking and exception handling for a runnable.
     *
     * @param runnable The runnable
     * @param actionName The name of the action to log if errors occur
     */
    void lockAndExecute(@Nonnull ICheckedRunnable runnable, @Nonnull String actionName)
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
     * @return All arrow feature set ids
     * @since 5.5
     */
    abstract Set<Long> getArrowFeatureSetIds();

    /**
     * Updates all breadcrumbs sizes based on the provided size value.
     *
     * @param breadcrumbSize The new breadcrumb size
     * @since 5.5
     * @see gov.takx.platform.preferences.PreferenceEntry#BREADCRUMBS_SIZE
     */
    private void updateBreadcrumbSizes(int breadcrumbSize)
    {
        this.breadcrumbSize = breadcrumbSize;

        final var parameters = new FeatureDataStore4.FeatureQueryParameters();
        parameters.featureSetFilter = new FeatureDataStore4.FeatureSetQueryParameters();
        parameters.featureSetFilter.ids = getArrowFeatureSetIds();

        lockAndExecute(() -> {
            // Update each feature's size while maintaining its other options
            try (var featureCursor = dataStore.queryFeatures(parameters))
            {
                while (featureCursor.moveToNext())
                {
                    final var feature = featureCursor.get();
                    final var style = (IconPointStyle) ((CompositeStyle) feature.getStyle()).getStyle(0);
                    final var newStyle = new IconPointStyle(style.getColor(), style.getIconUri(), breadcrumbSize,
                            breadcrumbSize, style.getIconAlignmentX(), style.getIconAligmnentY(),
                            style.getIconRotation(), style.isRotationAbsolute());

                    dataStore.updateFeature(feature.getId(), FeatureDataStore2.PROPERTY_FEATURE_STYLE, feature.getName(),
                            null, FeatureUtility.makeNoLabelStyle(newStyle), null, 0);
                }
            } catch (DataStoreException e)
            {
                logger.error("Error querying breadcrumbs to update sizes", e);
            }
        }, "Update breadcrumb sizes");
    }

    /**
     * Runnable-like functional interface that re-throws {@link DataStoreException}.
     */
    @FunctionalInterface
    interface ICheckedRunnable
    {
        void run() throws DataStoreException;
    }
}
