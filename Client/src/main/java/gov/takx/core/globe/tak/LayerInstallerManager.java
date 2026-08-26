
package gov.takx.core.globe.tak;

import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.layer.ILayerService;
import gov.takx.core.globe.LayerBoundsManager;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.globe.tak.layer.AfterRenderingLayer;
import gov.takx.core.globe.tak.layer.AnimationLayer;
import gov.takx.core.globe.tak.layer.BeforeRenderingLayer;
import gov.takx.core.globe.tak.layer.FrameRateOverlay;
import gov.takx.core.globe.tak.layer.ScreenAnnotationLayer;
import gov.takx.core.globe.tak.layer.WorldWindSpriteRenderLayer;
import gov.takx.core.globe.tak.layer.WorldWindSurfaceRenderLayer;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A service that adds necessary TAK layers to the TAK globe.
 *
 * @since 3.5
 */
@ApplicationScoped
public class LayerInstallerManager
{
    private final ILayerService layerService;
    private final TakLayerManager takLayerManager;
    private final ElevationLayerManager elevationLayerManager;
    private final ImageryLayerManager imageryLayerManager;
    private final FeatureRendererManager featureRendererManager;
    private final RenderableFeatureManager renderableFeatureManager;
    private final GlobeAnnotationMapObjectManager globeAnnotationMapObjectManager;
    private final RenderableAnnotationManager renderableAnnotationManager;
    private final ModelLayerManager modelLayerManager;
    private final WidgetLayerManager widgetLayerManager;
    private final PickedObjectManager pickedObjectManager;
    private final LayerBoundsManager layerBoundsManager;

    @Inject
    LayerInstallerManager(ILayerService layerService, TakLayerManager takLayerManager,
                          ImageryLayerManager imageryLayerManager, FeatureRendererManager featureRendererManager, ElevationLayerManager elevationLayerManager,
                          RenderableFeatureManager renderableFeatureManager,
                          GlobeAnnotationMapObjectManager globeAnnotationMapObjectManager, RenderableAnnotationManager renderableAnnotationManager,
                          ModelLayerManager modelLayerManager, WidgetLayerManager widgetLayerManager,
                          PickedObjectManager pickedObjectManager, LayerBoundsManager layerBoundsManager)
    {
        this.layerService = layerService;
        this.takLayerManager = takLayerManager;
        this.imageryLayerManager = imageryLayerManager;
        this.featureRendererManager = featureRendererManager;
        this.elevationLayerManager = elevationLayerManager;
        this.renderableFeatureManager = renderableFeatureManager;
        this.globeAnnotationMapObjectManager = globeAnnotationMapObjectManager;
        this.renderableAnnotationManager = renderableAnnotationManager;
        this.modelLayerManager = modelLayerManager;
        this.widgetLayerManager = widgetLayerManager;
        this.pickedObjectManager = pickedObjectManager;
        this.layerBoundsManager = layerBoundsManager;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_InstallTAKLayers) StartProjectServices event, @Headless boolean headless)
    {
        if (headless) return;
        takLayerManager.addLayer(new BeforeRenderingLayer());
        takLayerManager.addLayer(imageryLayerManager.getLayer());
        takLayerManager.addLayer(elevationLayerManager.getLayer());
        takLayerManager.addLayer(new WorldWindSpriteRenderLayer());
        takLayerManager.addLayer(new WorldWindSurfaceRenderLayer());
        takLayerManager.addLayer(pickedObjectManager.getLayer());
        takLayerManager.addLayer(takLayerManager.getShapeFileLayers());
        takLayerManager.addLayer(takLayerManager.getKmlOverlayLayers());
        takLayerManager.addLayer(takLayerManager.getUncategorizedLayers());
        takLayerManager.addLayer(takLayerManager.getPluginLayers());
        takLayerManager.addLayer(takLayerManager.getMapDataLayers());
        takLayerManager.addLayer(renderableFeatureManager.getLayer());
        takLayerManager.addLayer(renderableAnnotationManager.getLayer());
        takLayerManager.addLayerFactory((appGlobe, globe) -> globe.addLayer(featureRendererManager.createFeatureRenderer(appGlobe)));
        takLayerManager.addLayer(modelLayerManager.getSceneLayer());
        takLayerManager.addLayer(new ScreenAnnotationLayer());
        takLayerManager.addLayer(widgetLayerManager.getLayer());
        takLayerManager.addLayer(new AfterRenderingLayer());
        takLayerManager.addLayer(new AnimationLayer());

        pickedObjectManager.addPickedObjectProvider(featureRendererManager);
        pickedObjectManager.addPickedObjectProvider(globeAnnotationMapObjectManager);
        pickedObjectManager.addPickedObjectProvider(renderableAnnotationManager);
        pickedObjectManager.addPickedObjectProvider(widgetLayerManager);

        layerService.addLayer(FrameRateOverlay.layerConfiguration);
    }
}