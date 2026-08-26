package gov.takx.core.kml;

import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataSource.Content.ContentPointer;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.ModelInfo;
import com.atakmap.map.layer.model.ModelInfoFactory;
import com.atakmap.map.layer.model.SceneLayer;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.RuntimeRasterDataStore;
import com.atakmap.util.ConfigOptions;
import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.set.hash.HashLongSet;
import com.koloboke.collect.set.hash.HashLongSets;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Model;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.ScreenOverlay;
import gov.tak.core.map.layer.IPickContext;
import gov.tak.core.map.layer.feature.IFeatureRendererManager;
import gov.tak.core.map.layer.feature.IPickedFeatureProvider;
import gov.tak.core.map.layer.feature.datastore.SharedFeatureDataStore;
import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IWidget;
import gov.takx.api.globe.IWidgetInstance;
import gov.takx.api.globe.IWidgetLayerManager.IWidgetFactory;
import gov.takx.api.globe.WidgetLayoutPosition;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.core.configuration.KernelConfigOptionConstants;
import gov.takx.core.globe.tak.ModelLayerManager;
import gov.takx.core.globe.tak.TakLayerManager;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.globe.tak.features.IFeatureProvider;
import gov.takx.core.globe.widgets.KmlScreenOverlayWidget;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that manages {@link KmlVisual visuals} correlated to KML features as either TME Features for Placemarks,
 * or Widgets for screen overlays.
 *
 * @since 5.5
 */
@ApplicationScoped
public class KmlVisualManager implements IPickedFeatureProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Each screen overlay needs to create widgets with a unique name
    private static final AtomicInteger SCREEN_OVERLAY_COUNTER = new AtomicInteger(0);

    private final LongObjMap<KmlModelAndFeature> fidToKmlFeature = HashLongObjMaps.newMutableMap();
    private final LongObjMap<HashLongSet> featureIdsByFeatureSet = HashLongObjMaps.newMutableMap();

    private final InMemoryFeatureDataStore dataStore;
    private final WidgetLayerManager widgetLayerManager;
    private final TakLayerManager takLayerManager;

    private RuntimeRasterDataStore rasterDataStore;
    private RasterLayer2 imageryRasterLayer;
    private SceneLayer sceneLayer;

    /**
     * A holder for a {@link KmlModel} and a {@link Feature} within that model.  Currently used as the object produced
     * when a KML feature is "picked" (clicked by the user).
     */
    public record KmlModelAndFeature(KmlModel model, Feature feature)
    {
    }

    @Inject
    KmlVisualManager(@SharedFeatureDataStore InMemoryFeatureDataStore dataStore, IFeatureRendererManager featureRendererManager,
                     WidgetLayerManager widgetLayerManager, TakLayerManager takLayerManager)
    {
        this.dataStore = dataStore;
        this.widgetLayerManager = widgetLayerManager;
        this.takLayerManager = takLayerManager;

        featureRendererManager.addPickedFeatureProvider(this);
    }

    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        if (imageryRasterLayer != null)
        {
            takLayerManager.removeLayer(imageryRasterLayer, LayerGroup.KML_OVERLAY);
            rasterDataStore.dispose();
            imageryRasterLayer = null;
            rasterDataStore = null;
        }
        if (sceneLayer != null)
        {
            takLayerManager.removeLayer(sceneLayer);
            sceneLayer = null;
        }
    }

    /**
     * Given a {@link KmlModel}, produce {@link KmlVisual} elements for the KML Features.
     * <p>
     * {@link Placemark Placemarks} that have non-empty geometries will be translated into TME Features,
     * and added to the shared feature data store. These TME features are captured in a {@link KmlVisual} attached to
     * the JAK KML Feature node.
     * <p>
     * {@link ScreenOverlay ScreenOverlays} are translated into Widget instances, and captured in a {@link KmlVisual} attached to the JAK KML
     * Feature node.
     * <p>
     * {@link GroundOverlay GroundOverlays} are translated into Imagery layers, and captured in a {@link KmlVisual} attached to the JAK KML
     * Feature node.
     * <p>
     * {@link Model Models} are translated into Model layers, and captured in a {@link KmlVisual} attached to the JAK KML
     * Feature node.
     *
     * @param model Model to process
     * @param taskStatusHandler For reporting our progress
     * @throws IOException If parsing fails
     */
    public void createVisuals(@Nonnull KmlModel model, ITaskStatusHandler taskStatusHandler) throws IOException
    {
        createScreenOverlayVisuals(model);
        createFeatureVisuals(model, taskStatusHandler);
        createGroundOverlayVisuals(model);
        createModelVisuals(model);
    }

    /**
     * Remove all the {@link KmlVisual visuals} associated with the given KML model.
     */
    @InvokeOnEDT
    public void removeVisuals(@Nonnull KmlModel model)
    {
        removeScreenOverlayWidgets(model);
        removeFeatureSets(model);
        removeGroundOverlays(model);
        removeModels(model);
    }

    @Nullable
    @Override
    public Object getPickObject(@Nonnull IPickContext context, long fid)
    {
        return fidToKmlFeature.get(fid);
    }

    /**
     * Process all {@link ScreenOverlay} features in the given model.
     */
    private void createScreenOverlayVisuals(@Nonnull KmlModel model)
    {
        model.visitFeaturesOfType(ScreenOverlay.class, screenOverlay -> createScreenOverlayWidgetFactory(model, screenOverlay));
    }

    /**
     * Create the widget factory and {@link KmlVisual} for the given screen overlay.
     * Attach the factory and visual to the KML Feature node.
     *
     * @see #removeScreenOverlayWidgets(KmlModel)
     */
    @InvokeAndWaitOnEDT
    private void createScreenOverlayWidgetFactory(KmlModel model, ScreenOverlay screenOverlay)
    {
        // Need unique names for the widgets associated with this screen overlay
        final String name = "ScreenOverlay_" + SCREEN_OVERLAY_COUNTER.incrementAndGet();
        final var factory = new ScreenOverlayWidgetFactory(name, model, screenOverlay);

        widgetLayerManager.addWidgetFactory(factory, WidgetLayoutPosition.ABSOLUTE);

        final var visual = new KmlVisual(
                () -> widgetLayerManager.getWidgetInstancesByName(name).stream().anyMatch(IWidgetInstance::isVisible),
                visible -> widgetLayerManager.getWidgetInstancesByName(name).forEach(w -> w.setVisible(visible)),
                null);

        model.addAttachment(screenOverlay, factory);
        model.addAttachment(screenOverlay, visual);
    }

    /**
     * Given a {@link KmlModel}, parse the source file to get the renderable (geographic) features and add them to our
     * shared feature data store, correlating the produced TME features to the KML model.  This process will augment the
     * KmlModel with {@link KmlVisual visuals}, and track the FeatureSet ids that correlate to KML Containers.
     *
     * @param model Model to process
     * @param taskStatusHandler For reporting our progress and checking for cancellation
     * @throws IOException If parsing fails
     */
    private void createFeatureVisuals(KmlModel model, ITaskStatusHandler taskStatusHandler) throws IOException
    {
        // GDAL emits stupid warnings about folder layers
        org.gdal.gdal.gdal.PushErrorHandler();
        final var content = FeatureDataSourceContentFactory.parse(model.sourceFile(), null);
        org.gdal.gdal.gdal.PopErrorHandler();

        if (content == null) throw new IOException("Failed to parse: " + model.sourceFile());

        final var featureCount = new AtomicInteger(0);
        model.visitFeaturesOfType(Feature.class, feature -> featureCount.incrementAndGet());
        taskStatusHandler.setProgressIndeterminate(false);
        taskStatusHandler.setMinProgress(0);
        taskStatusHandler.setMaxProgress(featureCount.intValue());

        try
        {
            final var correlator = new FeatureCorrelator(model);

            dataStore.acquireModifyLock(true);
            final var featureIds = getNewMutableSet();
            int featuresProcessed = 0;

            while (content.moveToNext(ContentPointer.FEATURE_SET))
            {
                if (taskStatusHandler.isCanceled()) break;

                final String featureSetName = content.getFeatureSetName();

                final var featureSet = new FeatureSet(content.getProvider(), IFeatureProvider.ALL_GLOBES_GROUP_NAME, featureSetName, content.getMinResolution(), content.getMaxResolution());
                final long featureSetId = dataStore.insertFeatureSet(featureSet);
                logger.debug("FEATURE SET: id {}, name '{}'", featureSetId, featureSetName);

                featureIdsByFeatureSet.put(featureSetId, featureIds);

                correlator.processFeatureSet(featureSetName, featureSetId);

                while (content.moveToNext(ContentPointer.FEATURE))
                {
                    if (taskStatusHandler.isCanceled()) break;

                    final var featureDefinition = content.get();
                    if (featureDefinition != null)
                    {
                        final var feature = featureDefinition.get();
                        final var featureName = featureDefinition.getName();
                        final var kmlFeature = correlator.processFeature(featureName);
                        // GroundOverlay will be processed by createGroundOverlayVisuals()
                        if (kmlFeature instanceof GroundOverlay) continue;

                        final long fid = dataStore.insertFeature(featureSetId, FeatureDataStore2.FEATURE_ID_NONE, featureDefinition, 1);
                        logger.debug("FEATURE: id {}, name {}", fid, featureName);

                        featureIds.add(fid);
                        taskStatusHandler.setProgress(++featuresProcessed);

                        if (kmlFeature != null)
                        {
                            fidToKmlFeature.put(fid, new KmlModelAndFeature(model, kmlFeature));
                            attachFeatureVisual(kmlFeature, fid, feature.getGeometry().getEnvelope());
                        }
                    }
                }

                featureIds.clear();
            }
        } catch (Exception e)
        {
            throw new IOException("Failed to parse: " + model.sourceFile(), e);
        } finally
        {
            dataStore.releaseModifyLock();
            content.close();
        }
    }

    /**
     * This has to be broken out so we can force the use of the Client class loader, which is needed to have the
     * Koloboke library properly initialize.  We can't annotate the calling method, {@link #createFeatureVisuals}
     * because it calls into native code, and that breaks if we pushed the Client TCCL.
     *
     * @since 5.5
     */
    @UseClientClassLoader
    @Nonnull
    private static HashLongSet getNewMutableSet()
    {
        return HashLongSets.newMutableSet();
    }

    /**
     * Create and add the {@link KmlVisual} to control the TME Feature.
     */
    private void attachFeatureVisual(Feature kmlFeature, long fid, Envelope envelope)
    {
        final var visual = new KmlVisual(
                () -> dataStore.isFeatureVisible(fid),
                visible -> setFeatureVisibleNoThrows(fid, visible),
                envelope);

        kmlFeature.addToFeatureSimpleExtension(visual);
    }

    /**
     * Set the visibility of a feature, suppressing any exception.
     */
    private void setFeatureVisibleNoThrows(long fid, boolean visible)
    {
        try
        {
            dataStore.setFeatureVisible(fid, visible);
        } catch (DataStoreException e)
        {
            logger.warn("Failed to set Feature visibility: FID {}", fid);
        }
    }

    /**
     * Process all {@link GroundOverlay} features in the given model.
     */
    private void createGroundOverlayVisuals(@Nonnull KmlModel model)
    {
        final String cacheDirPath = ConfigOptions.getOption(KernelConfigOptionConstants.IMAGERY_OFFLINE_CACHE_DIR, null);
        final File cacheDir = cacheDirPath != null ? new File(cacheDirPath, "kmz") : null;

        // GDAL gets a little noisy here.
        org.gdal.gdal.gdal.PushErrorHandler();
        var descriptors = DatasetDescriptorFactory2.create(model.sourceFile(), cacheDir, "kmz", null);
        org.gdal.gdal.gdal.PopErrorHandler();

        if (descriptors != null && !descriptors.isEmpty())
        {
            ensureGroundOverlayLayer();
            model.visitFeaturesOfType(GroundOverlay.class, groundOverlay -> createGroundOverlayLayer(model, groundOverlay, descriptors));
        }
    }

    /**
     * Create the ground overlay layer and {@link KmlVisual} for the given ground overlay.
     * Attach the layer and visual to the KML Feature node.
     *
     * @see #removeGroundOverlays(KmlModel)
     */
    private void createGroundOverlayLayer(@Nonnull KmlModel model, @Nonnull GroundOverlay groundOverlay, @Nonnull Set<DatasetDescriptor> descriptors)
    {
        final String overlayName = groundOverlay.getName();
        descriptors.stream().filter(dd -> dd.getName().equals(overlayName)).findFirst().ifPresent(datasetDescriptor -> {
            datasetDescriptor = rasterDataStore.add(datasetDescriptor);

            final var visual = new KmlVisual(
                    () -> imageryRasterLayer.isVisible(overlayName),
                    v -> imageryRasterLayer.setVisible(overlayName, v),
                    datasetDescriptor.getMinimumBoundingBox());

            model.addAttachment(groundOverlay, visual);
            model.addAttachment(groundOverlay, datasetDescriptor);
        });
    }

    /**
     * Ensure the ground overlay layer is created and added to the maps.
     */
    private void ensureGroundOverlayLayer()
    {
        if (rasterDataStore == null) rasterDataStore = new RuntimeRasterDataStore();

        if (imageryRasterLayer == null)
        {
            var filter = new RasterDataStore.DatasetQueryParameters();
            filter.order = Collections.singletonList(RasterDataStore.DatasetQueryParameters.GSD.INSTANCE);
            imageryRasterLayer = new DatasetRasterLayer2("KML Screen Overlays", rasterDataStore, filter, Integer.MAX_VALUE);
            takLayerManager.addLayer(imageryRasterLayer, LayerGroup.KML_OVERLAY);
        }
    }

    /**
     * Process all 3D {@link Model} features in the given model.
     */
    private void createModelVisuals(@Nonnull KmlModel model)
    {
        String uri = model.sourceFile().getPath();
        if (ModelInfoFactory.isSupported(uri, "KMZ"))
        {
            ensureModelLayer();
            if (!sceneLayer.contains(uri)) sceneLayer.add(uri, "KMZ");

            Set<ModelInfo> modelInfos = ModelInfoFactory.create(uri, "KMZ");
            if (modelInfos == null || modelInfos.isEmpty()) return;

            final String sourceFileName = model.sourceFile().getName();
            model.visitFeaturesOfType(Placemark.class, placemark -> {
                if (placemark.getGeometry() instanceof Model)
                {
                    modelInfos.stream()
                            .filter(mi -> placemark.getName().equals(mi.name) || sourceFileName.equals(mi.name))
                            .findFirst()
                            .ifPresent(modelInfo -> createModelVisual(model, modelInfo, placemark));
                }
            });
        }
    }

    /**
     * Create the {@link KmlVisual} for the given 3D {@code modelInfo} and attach it to the {@code placemark} node.
     *
     * @see #removeModels(KmlModel)
     */
    private void createModelVisual(@Nonnull KmlModel model, @Nonnull ModelInfo modelInfo, @Nonnull Placemark placemark)
    {
        final var visual = new KmlVisual(
                () -> isModelVisible(modelInfo.name),
                v -> setModelVisible(modelInfo.name, v),
                ModelLayerManager.getModelEnvelope(modelInfo));

        model.addAttachment(placemark, visual);
    }

    /**
     * Ensure the 3D model layer is created and added to the maps.
     */
    private void ensureModelLayer()
    {
        if (sceneLayer == null)
        {
            sceneLayer = new SceneLayer("KML Scene Layer", null);
            takLayerManager.addLayer(sceneLayer);
        }
    }

    private boolean isModelVisible(@Nullable String name)
    {
        try (var cursor = querySceneLayer(name))
        {
            return cursor.moveToNext() && sceneLayer.isVisible(cursor.getId());
        }
    }

    private void setModelVisible(@Nullable String name, boolean visible)
    {
        try (var cursor = querySceneLayer(name))
        {
            while (cursor.moveToNext()) sceneLayer.setVisible(cursor.getId(), visible);
        }
    }

    /**
     * @param name feature name within the sceneLayer
     * @return Feature cursor of all features in the sceneLayer with the given name
     * @since 5.7
     */
    private FeatureCursor querySceneLayer(@Nullable String name)
    {
        if (name == null) return FeatureCursor.EMPTY;

        final var params = new FeatureDataStore2.FeatureQueryParameters();
        params.names = Set.of(name);
        return sceneLayer.query(params);
    }

    /**
     * Remove all the widgets created to represent {@link ScreenOverlay ScreenOverlays} in the given model.
     */
    private void removeScreenOverlayWidgets(KmlModel model)
    {
        model.visitFeaturesOfType(ScreenOverlay.class,
                screenOverlay -> model.visitAttachment(screenOverlay, ScreenOverlayWidgetFactory.class, widgetFactory -> {
                    widgetLayerManager.removeWidgetFactory(widgetFactory);
                    widgetLayerManager.removeWidgetsByName(widgetFactory.name);
                }));
    }

    /**
     * Remove all the {@link FeatureSet}s (which inherently removes all the individual TME Features)
     * associated with the given KML model.
     */
    private void removeFeatureSets(KmlModel model)
    {
        try
        {
            dataStore.acquireModifyLock(true);

            for (long id : model.featureSetIds())
            {
                dataStore.deleteFeatureSet(id);

                // And remove all the entries from the feature map
                final var featureIds = featureIdsByFeatureSet.remove(id);
                if (featureIds != null)
                {
                    for (long featureId : featureIds) fidToKmlFeature.remove(featureId);
                }
            }
        } catch (Exception e)
        {
            logger.error("Failed to remove features for {}", model.sourceFile(), e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Remove all the layers created to represent {@link GroundOverlay GroundOverlays} in the given model.
     */
    private void removeGroundOverlays(@Nonnull KmlModel model)
    {
        model.visitFeaturesOfType(GroundOverlay.class,
                groundOverlay -> model.visitAttachment(groundOverlay, DatasetDescriptor.class, dd -> rasterDataStore.remove(dd)));
    }

    /**
     * Remove all the features created to represent 3D {@link Model Models} in the given kml model.
     */
    private void removeModels(@Nonnull KmlModel model)
    {
        if (sceneLayer != null) sceneLayer.remove(model.sourceFile().getPath());
    }

    /**
     * Custom widget factory that creates widgets with specific names.  This will allow us to find all widgets created
     * by this factory when we need to delete the visuals.
     */
    private record ScreenOverlayWidgetFactory(String name, KmlModel model, ScreenOverlay screenOverlay) implements IWidgetFactory
    {
        @Override
        public IWidget create(IGlobeDelegate globe)
        {
            return new KmlScreenOverlayWidget(globe, model, screenOverlay, name);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            var that = (ScreenOverlayWidgetFactory) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
    }
}
