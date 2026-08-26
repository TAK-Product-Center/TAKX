package gov.takx.core.globe;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.tak.elevation.ElevationDownloaderLayer;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages which {@link IRaptorLayer IRaptorLayers} have their bounding boxes rendered to the map.
 * @since 5.2
 */
@ApplicationScoped
public class LayerBoundsManager implements IRaptorLayerManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // TODO TAKX-3629 Keeping this in a static list is brittle and we should consider refactoring it somewhere else
    private static final List<LayerGroup> SUPPORTS_BOUNDS_LAYER_GROUPS = List.of(LayerGroup.IMAGERY, LayerGroup.SURFACE_IMAGERY, LayerGroup.ELEVATION);

    private final Map<IRaptorLayer, Registration> registrations = new ConcurrentHashMap<>();

    private final InMemoryFeatureDataStore dataStore = new InMemoryFeatureDataStore();
    private final FeatureLayer3 featureLayer = new FeatureLayer3("LayerBoundsLayer", dataStore);

    private final Style imageryOutline = new BasicStrokeStyle(Color.YELLOW.getRGB(), 1f);
    private final Style elevationOutline = new BasicStrokeStyle(Color.GREEN.getRGB(), 1f);

    private final ApplicationGlobeManager globeManager;
    private final IRaptorLayerManager raptorLayerManager;

    private long featureSetId;

    @Inject
    private LayerBoundsManager(ApplicationGlobeManager globeManager, IRaptorLayerManager raptorLayerManager)
    {
        this.globeManager = globeManager;
        this.raptorLayerManager = raptorLayerManager;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_LayerViewManager + 1) StartProjectServices ignored, @Headless boolean headless)
    {
        if (headless) return;
        try
        {
            dataStore.acquireModifyLock(true);
            FeatureSet featureSet = new FeatureSet("LayerBoundsManager", "LayerBoundsLayer", "LayerBoundsLayer", Double.MAX_VALUE, 0);
            featureSetId = dataStore.insertFeatureSet(featureSet);
        } catch (Exception e)
        {
            logger.error("Failed to create LayerBoundsManager feature set", e);
        } finally
        {
            dataStore.releaseModifyLock();
        }

        globeManager.doWhenGlobeReady(g -> {
            raptorLayerManager.getLayers().forEach(this::registerLayer);
            raptorLayerManager.getModelLayers().forEach(this::registerLayer);
        });
        raptorLayerManager.addRaptorLayerManagerListener(this);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        try
        {
            // InMemoryFeatureDataStore cascades a delete action through the features
            dataStore.acquireModifyLock(true);
            dataStore.deleteFeatureSet(featureSetId);
        } catch (Exception e)
        {
            logger.error("Failed to delete LayerBoundsManager feature set.", e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
        removeAllLayers();
        raptorLayerManager.removeRaptorLayerManagerListener(this);
    }

    /**
     * @param layer The layer to check.
     * @return {@code true} if the layer's bounding box is currently showing.
     */
    public boolean isShowingBoundingBox(@Nonnull IRaptorLayer layer)
    {
        Registration registration = registrations.get(layer);
        return registration != null && registration.featureId != FeatureDataStore2.FEATURE_ID_NONE;
    }

    /**
     * Check if the bounding box of a given {@link IRaptorLayer} can be shown.
     * <p/>
     * Bounding boxes can be shown if:
     * <ul>
     *     <li>The layer contains either Imagery or Elevation data</li>
     *     <li>The layer's {@link Sector} is smaller than 90° x 90°</li>
     * </ul>
     * @param layer The layer to check.
     * @return {@code true} if the bounding box can be shown.
     */
    public boolean canShowBoundingBox(@Nonnull IRaptorLayer layer)
    {
        return registrations.containsKey(layer) && isValidSector(layer.getSector());
    }

    /**
     * @return {@code true} if the given {@code layer} defines a valid sector
     * @see IRaptorLayer#getSector()
     * @see #isValidSector(Sector)
     * @since 5.4
     */
    public boolean supportsGoTo(@Nonnull IRaptorLayer layer)
    {
        return isValidSector(layer.getSector());
    }

    /**
     * @return {@code true} if the given {@code sector} is non-null and is smaller than 90° x 90°
     * @see IRaptorLayer#getSector()
     * @since 5.4
     */
    private boolean isValidSector(@Nullable Sector sector)
    {
        return sector != null && sector.getDeltaLonDegrees() < 90.0 && sector.getDeltaLatDegrees() < 90.0;
    }

    /**
     * The {@link FeatureLayer3} for rendering the layer bounding boxes.
     *
     * @return the layer containing the {@link Feature Features}
     */
    public FeatureLayer3 getLayer()
    {
        return featureLayer;
    }

    @Override
    public void raptorLayerAdded(@Nonnull IRaptorLayer layer)
    {
        registerLayer(layer);
    }

    @Override
    public void raptorLayerRemoved(@Nonnull IRaptorLayer layer)
    {
        unregisterLayer(layer);
    }

    @Override
    public void raptorModelAdded(@Nonnull IRaptorLayer layer)
    {
        registerLayer(layer);
    }

    @Override
    public void raptorModelRemoved(@Nonnull IRaptorLayer layer)
    {
        unregisterLayer(layer);
    }

    @Override
    public void raptorLayersRemoved()
    {
        removeAllLayers();
    }

    /**
     * Register a {@link IRaptorLayer layer} for bounding box rendering. If the layer is not capable
     * of bounding box rendering, this results in a no-op.
     * @param layer the layer to register
     */
    private void registerLayer(@Nonnull IRaptorLayer layer)
    {
        // Don't register the ElevationDownloaderLayer
        if (ElevationDownloaderLayer.class.getName().equals(layer.getClassName())) return;

        // Only register if the layer group supports bounds
        if (!SUPPORTS_BOUNDS_LAYER_GROUPS.contains(layer.getLayerGroup())) return;

        // Don't re-register if we're already registered
        registrations.computeIfAbsent(layer, l -> new Registration(FeatureDataStore2.FEATURE_ID_NONE));
    }

    /**
     * Unregister a {@link IRaptorLayer layer} for bounding box rendering. If the layer is not registered
     * for bounding box rendering, this results in a no-op.
     * @param layer the layer to unregister
     */
    private void unregisterLayer(@Nonnull IRaptorLayer layer)
    {
        final var registration = registrations.remove(layer);
        if (registration != null) removeLayerFeatures(registration.featureId);
    }

    private void removeAllLayers()
    {
        registrations.keySet().forEach(this::unregisterLayer);
    }

    /**
     * Insert or update the feature that represents the bounding box for an {@link IRaptorLayer}.
     * @param layer The layer to render the box around
     * @param sector The sector to render as the box
     * @param featureId The existing feature id for the layer, or {@link FeatureDataStore2#FEATURE_ID_NONE FEATURE_ID_NONE} to insert a new record
     * @return the feature id representing the layer
     */
    private long createLayerFeatures(@Nonnull IRaptorLayer layer, @Nonnull Sector sector, long featureId)
    {
        try
        {
            dataStore.acquireModifyLock(true);
            LineString boundingBox = createBoundingLinestring(sector);
            if (featureId == FeatureDataStore2.FEATURE_ID_NONE)
            {
                Style lineStyle = layer.getLayerGroup() == LayerGroup.IMAGERY ? imageryOutline : elevationOutline;
                Feature feature = new Feature(featureSetId, null, boundingBox, lineStyle, null, Feature.AltitudeMode.ClampToGround, 0.0);
                return dataStore.insertFeature(feature);
            } else
            {
                dataStore.updateFeature(featureId, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY, null, boundingBox, null, null, 0);
                return featureId;
            }
        } catch (Exception e)
        {
            logger.error("Error inserting feature into LayerBoundsManager datastore.", e);
            return FeatureDataStore2.FEATURE_ID_NONE;
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Remove a feature for a layer's bounding box.
     * @param featureId The featureId of the layer bounding box
     * @return Always {@link FeatureDataStore2#FEATURE_ID_NONE}
     */
    @SuppressWarnings("SameReturnValue")
    private long removeLayerFeatures(long featureId)
    {
        if (featureId == FeatureDataStore2.FEATURE_ID_NONE) return FeatureDataStore2.FEATURE_ID_NONE;
        try
        {
            dataStore.acquireModifyLock(true);
            dataStore.deleteFeature(featureId);
        } catch (Exception e)
        {
            logger.error("Error deleting feature from LayerBoundsManager datastore.", e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
        return FeatureDataStore2.FEATURE_ID_NONE;
    }

    /**
     * Toggle the bounding box visibility of the {@link IRaptorLayer}.
     * @param layer the layer to toggle the visibility of the bounding box.
     */
    public void toggleBoundsVisibility(@Nonnull IRaptorLayer layer)
    {
        Registration registration = registrations.get(layer);
        if (registration != null)
        {
            boolean isVisible = registration.featureId != FeatureDataStore2.FEATURE_ID_NONE;
            setBoundsVisibility(layer, !isVisible);
        }
    }

    /**
     * Set bounding box visibility for a given {@link IRaptorLayer}.
     * @param layer the layer to set the visibility of the bounding box.
     * @param visible {@code true} to render the bounding box.
     */
    public void setBoundsVisibility(@Nonnull IRaptorLayer layer, boolean visible)
    {
        if (!canShowBoundingBox(layer)) return;

        Registration registration = registrations.get(layer);
        if (registration == null) return;

        final long featureId = visible
                ? createLayerFeatures(layer, layer.getSector(), registration.featureId)
                : removeLayerFeatures(registration.featureId);

        registrations.put(layer, new Registration(featureId));
    }

    private LineString createBoundingLinestring(@Nonnull Sector sector)
    {
        var lineString = new LineString(2);
        final LatLon[] corners = sector.getCorners();
        Stream.concat(Arrays.stream(corners), Stream.of(corners[0])).forEach(point -> lineString.addPoint(point.longitude.degrees, point.latitude.degrees));
        return lineString;
    }

    /**
     * A bounds registration for an {@link IRaptorLayer}.
     * @param featureId the feature id for the bounds
     * @since 5.3
     */
    private record Registration(long featureId)
    {
    }
}
