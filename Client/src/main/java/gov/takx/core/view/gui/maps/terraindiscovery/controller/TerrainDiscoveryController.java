package gov.takx.core.view.gui.maps.terraindiscovery.controller;

import com.atakmap.map.elevation.ElevationSource;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.terrain.experimental.ElevationSourceUtils;
import gov.takx.api.terrain.experimental.IImmutableHighestResolutionTerrain;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.ITerrainListener;
import gov.takx.api.terrain.experimental.ITerrainService;
import gov.takx.api.terrain.experimental.TerrainMessage;
import gov.takx.api.terrain.experimental.proposed.tile.TileCollection;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.calculator.TerrainDiscoveryCalculator;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.ITerrainRenderable;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.RenderableElevationLayer;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.ResolutionRenderable;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.SurfaceMultiSector;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.TerrainSurfaceImage;
import gov.takx.core.view.gui.maps.terraindiscovery.model.ITerrainDiscoveryModel;
import gov.takx.core.view.gui.maps.terraindiscovery.model.TerrainDiscoveryModel;
import gov.takx.core.view.gui.maps.terraindiscovery.view.ITerrainDiscoveryView;
import gov.takx.core.view.gui.maps.terraindiscovery.view.TerrainDiscoveryView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
@ApplicationScoped
public class TerrainDiscoveryController implements ITerrainDiscoveryController, ITerrainListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Color[] COLORS =
            {
                    new Color(102, 159, 208),
                    new Color(204, 188, 0),
                    new Color(155, 0, 1),
                    new Color(41, 192, 0),
                    new Color(147, 86, 83),
                    new Color(163, 125, 83),
                    new Color(0, 117, 248),
                    new Color(127, 64, 12),
                    new Color(66, 183, 155),
                    new Color(200, 148, 68),
                    new Color(191, 79, 59),
                    new Color(202, 218, 115),
                    new Color(75, 122, 53),
                    new Color(77, 191, 168),
                    new Color(80, 159, 29),
                    new Color(151, 135, 59)
            };
    private static final String NOT_AVAILABLE = "N/A";

    private final IExtendedGlobeManager globeManager;
    private final IRaptorLayerManager layerManager;
    private final ITerrainService terrainService;
    private final ElevationLayerManager elevationLayerManager;
    private final ITerrainDiscoveryModel model;
    private ITerrainDiscoveryView view;
    private final double earthRadius;
    private final AtomicInteger resolutionCount;
    private final TerrainDiscoveryCalculator calculator;
    private boolean projectClosing = false;
    private final DecimalFormat coverageFormat;

    @Inject
    TerrainDiscoveryController(IExtendedGlobeManager globeManager, IRaptorLayerManager layerManager, ITerrainService terrainService, ElevationLayerManager elevationLayerManager)
    {
        this.globeManager = globeManager;
        this.layerManager = layerManager;
        this.terrainService = terrainService;
        this.elevationLayerManager = elevationLayerManager;

        coverageFormat = new DecimalFormat("0.0###");
        coverageFormat.setMaximumFractionDigits(4);
        model = new TerrainDiscoveryModel(terrainService);
        view = null;
        model.addListener(this);
        earthRadius = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getModel().getGlobe().getRadius();
        resolutionCount = new AtomicInteger(0);
        calculator = new TerrainDiscoveryCalculator(this);
    }

    @Override
    @InvokeOnEDT
    public void show()
    {
        if (view == null)
        {
            view = new TerrainDiscoveryView(this);
        }
        initialize();
        view.setVisible(true);
    }

    @Override
    public ITerrainDiscoveryModel getModel()
    {
        return model;
    }

    @Override
    public ITerrainDiscoveryView getView()
    {
        return view;
    }

    @Override
    public void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible)
    {
        model.setElevationLayerVisible(elevationLayer, visible);
    }

    @Override
    public void setElevationLayerColor(IRaptorLayer elevationLayer, Color color)
    {
        model.setElevationLayerColor(elevationLayer, color);
    }

    @Override
    public void setSurfaceShape(IRaptorLayer elevationLayer, SurfaceMultiSector shape)
    {
        model.setSurfaceShape(elevationLayer, shape);
    }

    @Override
    public void setSurfaceImage(IRaptorLayer elevationLayer, TerrainSurfaceImage image)
    {
        model.setSurfaceImage(elevationLayer, image);
    }

    @Override
    public void setElevationLayerAvailability(IRaptorLayer elevationLayer, Double availability)
    {
        model.setElevationLayerAvailability(elevationLayer, availability);
    }

    @Override
    public void clearVisualizations()
    {
        getModel().setNegativeSpaceShape(null);

        RenderableLayer elevationBoundsLayer = model.getElevationBoundsLayer();
        if (elevationBoundsLayer != null)
        {
            elevationBoundsLayer.removeAllRenderables();
        }

        model.clearSurfaceShapes();
        model.clearSurfaceImages();
        model.clearResolutionShape();

        if (view != null)
        {
            view.clearTemp();
        }

        calculator.clearSector();
        redraw();
    }

    @Override
    public void setSelectedGeofence(IRectangle geofence)
    {
        model.setSelectedGeofence(geofence);
    }

    @Override
    public void setOpacity(double opacity)
    {
        model.setSurfaceOpacity(opacity);
    }

    @Override
    public void clearTemp()
    {
        clearVisualizations();

        removeAnalyticsLayers();

        model.clearTemp();

        if (view != null)
        {
            view.clearTemp();
            terrainService.getHighestResolutionTerrain().removeTerrainListener(this);
        }

        calculator.reset();
        model.flushTiles();
    }

    @Override
    public void clearAll()
    {
        clearTemp();
        model.clearAll();
        if (view != null) view.clearAll();
    }

    @Override
    public void initialize()
    {
        clearTemp();
        calculator.initialize();

        final RaptorLayerManagerListener listener = new RaptorLayerManagerListener();

        layerManager.addRaptorLayerManagerListener(listener);

        model.setElevationLayers(layerManager.getModelLayers());

        terrainService.getHighestResolutionTerrain().addTerrainListener(this);

        updateViewFromElevationLayers();
    }

    @Override
    public synchronized void calculationComplete()
    {
        view.setProgressStopped();
        flushTileRasters();
        updateVisibility();
    }

    @Override
    public synchronized void calculate()
    {
        calculator.calculate();
    }

    @Override
    public void cancelCalculation()
    {
        calculator.cancelWork();
    }

    @Override
    public void setProgressStarted()
    {
        view.setProgressStarted();
    }

    @Override
    public void setProgress(double progress)
    {
        view.setProgressUpdated(progress * 100d);
    }

    @Override
    public void setProgressText(String text)
    {
        view.setProgressText(text);
    }

    @Override
    public void setProgressStopped()
    {
        view.setProgressStopped();
    }

    @Override
    public void setPosition(Position position)
    {
        model.setPosition(position);
    }

    @Override
    public Sector getBoundingSector()
    {
        return TerrainDiscoveryUtils.rectangleToSector(model.getSelectedGeofence());
    }

    @Override
    public void updateElevationLayerCoverage(IRaptorLayer elevationLayer)
    {
        view.setElevationLayerCoverage(elevationLayer, getCoverage(elevationLayer));
        view.setElevationLayerResolution(elevationLayer, getResolution(elevationLayer));
    }

    @Override
    public void setElevationLayerToTop(IRaptorLayer elevationLayer)
    {
        model.setElevationLayerToTop(elevationLayer);
    }

    @Override
    public void setTileBoundsVisible(boolean visible)
    {
        model.setTileBoundariesVisible(visible);
    }

    @Override
    public Iterable<ITerrainRenderable> getTerrainRenderables()
    {
        List<ITerrainRenderable> renderables = new ArrayList<>();
        ITerrainDiscoveryModel.Visualization vis = model.getVisualizationMode();
        if (vis == ITerrainDiscoveryModel.Visualization.RESOLUTION)
        {
            addTerrainRenderable(renderables, model.getResolutionShape());
        }

        if (vis != ITerrainDiscoveryModel.Visualization.RESOLUTION)
        {
            addTerrainRenderable(renderables, model.getNegativeSpaceShape());
        }

        List<IRaptorLayer> layers = model.getElevationLayers();
        for (int i = layers.size() - 1; i >= 0; i--)
        {
            IRaptorLayer layer = layers.get(i);
            if (layer != null)
            {
                switch (vis)
                {
                    case RESOLUTION -> logger.debug("Ignoring RESOLUTION");
                    case TERRAIN_IMAGE -> addTerrainRenderable(renderables, this.model.getSurfaceImage(layer));
                    case COVERAGE -> addTerrainRenderable(renderables, this.model.getSurfaceShape(layer));
                }
            }
        }
        return renderables;
    }

    @Override
    public void setUseCoverageVisualization()
    {
        model.setVisualizationMode(ITerrainDiscoveryModel.Visualization.COVERAGE);
    }

    @Override
    public void setUseResolutionVisualization()
    {
        model.setVisualizationMode(ITerrainDiscoveryModel.Visualization.RESOLUTION);
    }

    @Override
    public void setUseTerrainImageVisualization()
    {
        model.setVisualizationMode(ITerrainDiscoveryModel.Visualization.TERRAIN_IMAGE);
    }

    @Override
    public synchronized Color getColorForResolution(double resolution)
    {
        Color c = model.getColorForResolution(resolution);
        if (c == null)
        {
            int index = resolutionCount.getAndIncrement();
            c = COLORS[index % COLORS.length];
            model.setColorForResolution(resolution, c);
        }
        return c;
    }

    @Override
    public void updateTileCollectionsEnabled()
    {
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            model.getTileCollection(elevationLayer).setEnabled(model.isElevationLayerVisible(elevationLayer));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        Object newValue = evt.getNewValue();
        switch (evt.getPropertyName())
        {
            case ITerrainDiscoveryModel.ELEVATION_LAYERS:
                view.setElevationLayers(model.getElevationLayers());
                break;

            case ITerrainDiscoveryModel.ELEVATION_LAYER_COLOR:
                IRaptorLayer em = (IRaptorLayer) newValue;
                doSetElevationLayerColor(em, model.getElevationLayerColor(em));
                break;

            case ITerrainDiscoveryModel.SURFACE_SHAPE:
            case ITerrainDiscoveryModel.SURFACE_IMAGE:
            case ITerrainDiscoveryModel.SURFACE_IMAGES_VISIBLE:
            case ITerrainDiscoveryModel.SURFACE_SHAPES_VISIBLE:
            case ITerrainDiscoveryModel.TILE_BOUNDARIES_VISIBLE:
                updateVisibility();
                break;

            case ITerrainDiscoveryModel.ELEVATION_LAYER_VISIBLE:
                if (newValue instanceof IRaptorLayer raptorLayer)
                {
                    updateElevationLayerVisibility(raptorLayer);
                }

                updateVisibility();

                // Attempt to refresh the resolution values.  This includes updating the elevation bounds and creating
                // a new resolutions shape.  These need to be refreshed because both are affected by the visibility of
                // all elevation layers.
                calculator.refreshResolutions();
                break;

            case ITerrainDiscoveryModel.SURFACE_OPACITY:
                updateOpacity();
                break;

            case ITerrainDiscoveryModel.SELECTED_GEOFENCE:
                clearVisualizations();
                removeAnalyticsLayers();
                break;

            case ITerrainDiscoveryModel.POSITION:
                doUpdatePosition(model.getPosition());
                break;

            case ITerrainDiscoveryModel.ELEVATION_LAYER_TOP:
                updateElevationLayerOrder();
                break;

            case ITerrainDiscoveryModel.ENABLE_IMAGE_CALCULATIONS:
                if ((Boolean) newValue && !model.areSurfaceImagesAvailable())
                {
                    calculate();
                }
                break;

            case ITerrainDiscoveryModel.VISUALIZATION_MODE:
                model.setImageCalculationEnabled(newValue == ITerrainDiscoveryModel.Visualization.TERRAIN_IMAGE);
                updateElevationBoundsLayerVisibility();

                // upon redraw, the layer will request renderables from the controller.
                // the controller will return an iterable of renderables based upon the current visualization mode.
                redraw();
                break;

            default:
                break;
        }
    }

    /**
     * Creates an {@link IRaptorLayer} for the terrain coverage visualization and a layer for the boundaries of the
     * calculation. This method should be called at the beginning of each calculation as the layers are removed when the
     * calculation is no longer valid (i.e. user selects a new geofence, user clicks clear, etc)
     */
    @Override
    public void createAnalyticsLayers()
    {
        // only create the layers if they don't exist

        IApplicationGlobe globe = globeManager.getPrimaryGlobeDelegate();

        if (model.getRaptorTerrainLayer() == null)
        {
            RenderableElevationLayer terrainLayer = new RenderableElevationLayer(this);
            IRaptorLayer raptorTerrainLayer = globe.addLayer(terrainLayer, LayerGroup.ANALYTIC_SURFACES);
            raptorTerrainLayer.setDisplayName("Terrain Discovery Coverage");
            model.setTerrainLayer(raptorTerrainLayer, terrainLayer);
        }

        if (model.getRaptorElevationBoundsLayer() == null)
        {
            RenderableLayer elevationBoundsLayer = new RenderableLayer();
            IRaptorLayer raptorElevationBoundsLayer = globe.addLayer(elevationBoundsLayer, LayerGroup.ANALYTIC_SURFACES);
            raptorElevationBoundsLayer.setDisplayName("Terrain Discovery Bounds");
            model.setElevationBoundsLayer(raptorElevationBoundsLayer, elevationBoundsLayer);
        }
    }

    /**
     * removes the terrain coverage visualization and the boundaries of the calculation
     * This method should be called when a calculation is no longer valid (i.e. user selects a new geofence,
     * user clicks clear, etc)
     */
    @Override
    public void removeAnalyticsLayers()
    {
        IApplicationGlobe globe = globeManager.getPrimaryGlobeDelegate();

        IRaptorLayer raptorTerrainLayer = model.getRaptorTerrainLayer();
        if (raptorTerrainLayer != null)
        {
            globe.removeLayer(raptorTerrainLayer);
            model.setTerrainLayer(null, null);
        }

        IRaptorLayer raptorElevationBoundsLayer = model.getRaptorElevationBoundsLayer();
        if (raptorElevationBoundsLayer != null)
        {
            globe.removeLayer(raptorElevationBoundsLayer);
            model.setElevationBoundsLayer(null, null);
        }
    }

    @Override
    public boolean isProjectClosing()
    {
        return projectClosing;
    }

    @Override
    public void notify(IImmutableHighestResolutionTerrain terrain, TerrainMessage message, String information)
    {
        // do nothing
    }

    @Override
    public void trackResolution(double resolutionInMeters)
    {
        // do nothing
    }

    @Override
    public void elevationModelsChanged(List<ElevationModel> newModels)
    {
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            ITakElevationModel em = elevationLayerManager.getElevationModel(elevationLayer);
            model.setElevationLayerVisible(elevationLayer, newModels.contains(em));
        }
    }

    private void updateViewFromElevationLayers()
    {
        for (IRaptorLayer elevationLayer : layerManager.getModelLayers())
        {
            view.setElevationLayerVisible(elevationLayer, elevationLayer.isVisible());
        }
    }

    private void updateElevationLayerVisibility(IRaptorLayer elevationLayer)
    {
        boolean visible = model.isElevationLayerVisible(elevationLayer);
        if (elevationLayer.isVisible() != visible)
        {
            elevationLayer.setVisible(visible);
        }
    }

    private void handleProjectClosing(@Observes(notifyObserver = Reception.IF_EXISTS) ProjectClosing event)
    {
        projectClosing = true;
        clearAll();
    }

    private void handleProjectClosed(@Observes(notifyObserver = Reception.IF_EXISTS) ProjectClosed event)
    {
        projectClosing = false;
    }

    private void updateElevationBoundsLayerVisibility()
    {
        RenderableLayer boundsLayer = model.getElevationBoundsLayer();
        if (boundsLayer != null)
        {
            boundsLayer.setEnabled(model.getVisualizationMode() != ITerrainDiscoveryModel.Visualization.RESOLUTION);
        }
    }

    private void updateVisibility()
    {
        updateElevationBoundsLayerVisibility();
        boolean tileBoundariesVisible = model.areTileBoundariesVisible();
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            boolean layerVisible = model.isElevationLayerVisible(elevationLayer);

            TileCollection tileCollection = model.getTileCollection(elevationLayer);
            if (tileCollection != null)
            {
                tileCollection.setEnabled(layerVisible);
            }

            TerrainSurfaceImage surfaceImage = model.getSurfaceImage(elevationLayer);
            if (surfaceImage != null)
            {
                surfaceImage.setVisible(layerVisible);
            }

            SurfaceMultiSector surfaceShape = model.getSurfaceShape(elevationLayer);
            if (surfaceShape != null)
            {
                surfaceShape.setInteriorVisible(layerVisible);
                surfaceShape.setOutlineVisible(tileBoundariesVisible && layerVisible);
            }

            view.setElevationLayerVisible(elevationLayer, layerVisible);
        }

        updateOpacity();
        redraw();
    }

    private void flushTileRasters()
    {
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            model.getTileCollection(elevationLayer).flushRasters();
        }
    }

    private void updateElevationLayerOrder()
    {
        view.updateElevationLayerOrder(model.getElevationLayers());
        redraw();
    }

    private void doUpdatePosition(Position position)
    {
        if (position == null)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Updated position is null");
            }
            setNoPosition();
            return;
        }

        IRectangle geofence = model.getSelectedGeofence();
        if (!TerrainDiscoveryUtils.contains(geofence, position.latitude.degrees, position.longitude.degrees))
        {
            setNoPosition();
            return;
        }

        final double lat = position.getLatitude().getDegrees();
        final double lon = position.getLongitude().getDegrees();
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            String elevation = NOT_AVAILABLE;
            if (getBoundingSector().containsDegrees(lat, lon))
            {
                ElevationSource elevationSource = elevationLayerManager.getElevationSource(elevationLayer);
                if (elevationSource != null)
                {
                    double value = ElevationSourceUtils.queryElevation(elevationSource, lat, lon, Double.MIN_VALUE, null);
                    if (!Double.isNaN(value))
                    {
                        elevation = String.format("%.1f m", value);
                    }
                }
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Layer {} elevation: {}", elevationLayer.getName(), elevation);
            }
            view.setElevationAtMouseCursor(elevationLayer, elevation);
        }
    }

    private void addTerrainRenderable(List<ITerrainRenderable> renderables, ITerrainRenderable renderable)
    {
        if (renderable != null)
        {
            renderables.add(renderable);
        }
    }

    private void setNoPosition()
    {
        for (IRaptorLayer elevationLayer : model.getElevationLayers())
        {
            view.setElevationAtMouseCursor(elevationLayer, NOT_AVAILABLE);
        }
    }

    private String getResolution(IRaptorLayer elevationLayer)
    {
        ITakElevationModel elevationModel = elevationLayerManager.getElevationModel(elevationLayer);
        if (elevationModel == null) return "";
        double[] resolutions = TerrainDiscoveryUtils.getResolutions(
                globeManager.getPrimaryGlobeDelegate().getWorldWindow(),
                elevationModel,
                model.getSelectedGeofence());
        return TerrainDiscoveryUtils.getResolutionString(resolutions, earthRadius);
    }

    private void updateOpacity()
    {
        double opacity = model.getSurfaceOpacity();
        for (TerrainSurfaceImage surfaceImage : model.getSurfaceImages())
        {
            surfaceImage.setOpacity(opacity);
        }

        for (SurfaceMultiSector surfaceShape : model.getSurfaceShapes())
        {
            surfaceShape.setOpacity(opacity);
        }

        ResolutionRenderable resolutionShape = model.getResolutionShape();
        if (resolutionShape != null)
        {
            resolutionShape.setOpacity(opacity);
        }

        redraw();
    }

    private void doSetElevationLayerColor(IRaptorLayer elevationLayer, Color color)
    {
        TerrainSurfaceImage surfaceImage = model.getSurfaceImage(elevationLayer);
        if (surfaceImage != null)
        {
            surfaceImage.setColor(color);
        }

        SurfaceMultiSector surfaceShape = model.getSurfaceShape(elevationLayer);
        if (surfaceShape != null)
        {
            surfaceShape.setSurfaceColor(color);
        }

        redraw();
    }

    private String getCoverage(IRaptorLayer elevationLayer)
    {
        // Calculate the local data availability across the sector of interest
        String coverage = "N/A";
        Double availability = model.getElevationLayerAvailability(elevationLayer);
        if (availability != null)
        {
            coverage = getCoverageString(availability);
        }
        return coverage;
    }

    private synchronized String getCoverageString(double coverage)
    {
        return coverageFormat.format(coverage * 100d) + " %";
    }

    private void redraw()
    {
        // Redraw the globe now!
        globeManager.repaintAllGlobes();
    }

    private class RaptorLayerManagerListener implements IRaptorLayerManagerListener
    {
        /**
         * Listen for analytic layer deletion and remove the layer from the TD's model.
         * This prevents misbehavior when the user manually deletes an analytic layer
         */
        @Override
        public void raptorLayerRemoved(IRaptorLayer raptorLayer)
        {
            IRaptorLayer raptorTerrainLayer = model.getRaptorTerrainLayer();
            if (raptorTerrainLayer != null && raptorTerrainLayer.equals(raptorLayer))
            {
                model.setTerrainLayer(null, null);
            }

            IRaptorLayer raptorElevationBoundsLayer = model.getRaptorElevationBoundsLayer();
            if (raptorElevationBoundsLayer != null && raptorElevationBoundsLayer.equals(raptorLayer))
            {
                model.setElevationBoundsLayer(null, null);
            }
        }
    }
}
