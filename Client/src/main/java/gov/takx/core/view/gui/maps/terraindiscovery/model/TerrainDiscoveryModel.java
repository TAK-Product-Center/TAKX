package gov.takx.core.view.gui.maps.terraindiscovery.model;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.ITerrainService;
import gov.takx.api.terrain.experimental.proposed.tile.TileCollection;
import gov.takx.api.terrain.experimental.proposed.tile.TileManager;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.RenderableElevationLayer;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.ResolutionRenderable;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.SurfaceMultiSector;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.TerrainSurfaceImage;
import jakarta.inject.Inject;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 */
@PseudoBean
public class TerrainDiscoveryModel implements ITerrainDiscoveryModel
{
    @Inject
    private ElevationLayerManager elevationLayerManager;

    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final List<IRaptorLayer> elevationLayers = new CopyOnWriteArrayList<>();
    private final Map<IRaptorLayer, Color> layerColors = new ConcurrentHashMap<>();
    private final Map<IRaptorLayer, Boolean> layerVisibility = new ConcurrentHashMap<>();
    private final Map<IRaptorLayer, SurfaceMultiSector> surfaceShapes = new ConcurrentHashMap<>();
    private final Map<IRaptorLayer, TerrainSurfaceImage> surfaceImages = new ConcurrentHashMap<>();
    private final Map<IRaptorLayer, Double> layerAvailability = new ConcurrentHashMap<>();
    private final AtomicReference<IRaptorLayer> raptorElevationBoundsLayer = new AtomicReference<>();
    private final AtomicReference<RenderableLayer> elevationBoundsLayer = new AtomicReference<>();
    private final AtomicReference<IRaptorLayer> raptorTerrainLayer = new AtomicReference<>();
    private final AtomicReference<RenderableElevationLayer> terrainLayer = new AtomicReference<>();
    private final AtomicBoolean tileBoundariesVisible = new AtomicBoolean(false);
    private final AtomicBoolean imageCalculationsEnabled = new AtomicBoolean(false);
    private final AtomicReference<IRectangle> selectedGeofence = new AtomicReference<>();
    private final AtomicReference<Position> position = new AtomicReference<>();
    private final AtomicReference<ResolutionRenderable> resolutionShape = new AtomicReference<>();
    private final AtomicReference<SurfaceMultiSector> negativeSpaceShape = new AtomicReference<>();
    private final ConcurrentHashMap<Double, Color> resolutionColorMap = new ConcurrentHashMap<>();
    private final TileManager tileManager;
    private final AtomicReference<Double> surfaceOpacity = new AtomicReference<>(0.6d);
    private final AtomicReference<Visualization> visualizationMode = new AtomicReference<>(Visualization.COVERAGE);

    public TerrainDiscoveryModel(ITerrainService terrainService)
    {
        tileManager = new TileManager(new ArrayList<>(), terrainService.getHighestResolutionTerrain());
        clearAll();
    }

    @Override
    public synchronized void clearAll()
    {
        clearTemp();
        elevationLayers.clear();
        layerVisibility.clear();
        layerColors.clear();
        selectedGeofence.set(null);
    }

    @Override
    public synchronized void clearTemp()
    {
        surfaceShapes.clear();
        surfaceImages.clear();
        layerAvailability.clear();
        raptorElevationBoundsLayer.set(null);
        elevationBoundsLayer.set(null);
        raptorTerrainLayer.set(null);
        terrainLayer.set(null);
        resolutionShape.set(null);
        negativeSpaceShape.set(null);
        position.set(null);
    }

    @Override
    public void setElevationBoundsLayer(IRaptorLayer raptorElevationBoundsLayer, RenderableLayer elevationBoundsLayer)
    {
        IRaptorLayer old1 = this.raptorElevationBoundsLayer.get();
        if (valueChanged(old1, raptorElevationBoundsLayer))
        {
            this.raptorElevationBoundsLayer.set(raptorElevationBoundsLayer);
            pcs.firePropertyChange(RAPTOR_ELEVATION_BOUNDS_LAYER, old1, raptorElevationBoundsLayer);
        }

        RenderableLayer old2 = this.elevationBoundsLayer.get();
        if (valueChanged(old2, elevationBoundsLayer))
        {
            this.elevationBoundsLayer.set(elevationBoundsLayer);
            pcs.firePropertyChange(ELEVATION_BOUNDS_LAYER, old2, elevationBoundsLayer);
        }
    }

    @Override
    public void setTerrainLayer(IRaptorLayer raptorTerrainLayer, RenderableElevationLayer terrainLayer)
    {
        IRaptorLayer old1 = this.raptorTerrainLayer.get();
        if (valueChanged(old1, raptorTerrainLayer))
        {
            this.raptorTerrainLayer.set(raptorTerrainLayer);
            pcs.firePropertyChange(RAPTOR_TERRAIN_LAYER, old1, raptorTerrainLayer);
        }

        RenderableElevationLayer old2 = this.terrainLayer.get();
        if (valueChanged(old2, terrainLayer))
        {
            this.terrainLayer.set(terrainLayer);
            pcs.firePropertyChange(TERRAIN_LAYER, old2, terrainLayer);
        }
    }

    @Override
    public void setElevationLayerToTop(IRaptorLayer elevationLayer)
    {
        IRaptorLayer old;
        synchronized (this)
        {
            old = elevationLayers.isEmpty() ? null : elevationLayers.get(0);
        }
        if (valueChanged(old, elevationLayer))
        {
            elevationLayers.remove(elevationLayer);
            elevationLayers.add(0, elevationLayer);
            pcs.firePropertyChange(ELEVATION_LAYER_TOP, old, elevationLayer);
        }
    }

    @Override
    public void setElevationLayers(List<IRaptorLayer> elevationLayers)
    {
        List<IRaptorLayer> old = new ArrayList<>(this.elevationLayers);
        if (valueChanged(old, elevationLayers))
        {
            List<ITakElevationModel> elevationModels = elevationLayers.stream()
                    .map(l -> elevationLayerManager.getElevationModel(l))
                    .filter(Objects::nonNull)
                    .toList();

            tileManager.setElevationModels(elevationModels);
            this.elevationLayers.clear();
            this.elevationLayers.addAll(elevationLayers);
            pcs.firePropertyChange(ELEVATION_LAYERS, old, new ArrayList<>(elevationLayers));
        }
    }

    @Override
    public void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible)
    {
        if (valueChanged(layerVisibility.get(elevationLayer), visible))
        {
            layerVisibility.put(elevationLayer, visible);
            pcs.firePropertyChange(ELEVATION_LAYER_VISIBLE, null, elevationLayer);
        }
    }

    @Override
    public void setElevationLayerColor(IRaptorLayer elevationLayer, Color color)
    {
        if (valueChanged(layerColors.get(elevationLayer), color))
        {
            layerColors.put(elevationLayer, color);
            pcs.firePropertyChange(ELEVATION_LAYER_COLOR, null, elevationLayer);
        }
    }

    @Override
    @InvokeOnEDT
    public void setSurfaceShape(IRaptorLayer elevationLayer, SurfaceMultiSector shape)
    {
        if (shape != null)
        {
            surfaceShapes.put(elevationLayer, shape);
        } else
        {
            surfaceShapes.remove(elevationLayer);
        }
        pcs.firePropertyChange(SURFACE_SHAPE, null, elevationLayer);
    }

    @Override
    @InvokeOnEDT
    public void setNegativeSpaceShape(SurfaceMultiSector shape)
    {
        SurfaceMultiSector old = negativeSpaceShape.get();
        if (valueChanged(old, shape))
        {
            negativeSpaceShape.set(shape);
            pcs.firePropertyChange(NEGATIVE_SPACE_SHAPE, old, shape);
        }
    }

    @Override
    @InvokeOnEDT
    public void setSurfaceImage(IRaptorLayer elevationLayer, TerrainSurfaceImage image)
    {
        if (image != null)
        {
            surfaceImages.put(elevationLayer, image);
        } else
        {
            surfaceImages.remove(elevationLayer);
        }
        pcs.firePropertyChange(SURFACE_IMAGE, null, elevationLayer);
    }

    @Override
    public void setResolutionShape(ResolutionRenderable layeredShape)
    {
        ResolutionRenderable old = resolutionShape.get();
        if (valueChanged(old, layeredShape))
        {
            resolutionShape.set(layeredShape);
            pcs.firePropertyChange(RESOLUTION_SHAPE, old, layeredShape);
        }
    }

    @Override
    public void setSurfaceOpacity(double opacity)
    {
        double old = surfaceOpacity.get();
        if (valueChanged(old, opacity))
        {
            surfaceOpacity.set(opacity);
            pcs.firePropertyChange(SURFACE_OPACITY, old, opacity);
        }
    }

    @Override
    public void setElevationLayerAvailability(IRaptorLayer elevationLayer, double availability)
    {
        if (valueChanged(layerAvailability.get(elevationLayer), availability))
        {
            layerAvailability.put(elevationLayer, availability);
            pcs.firePropertyChange(ELEVATION_LAYER_AVAILABILITY, null, elevationLayer);
        }
    }

    @Override
    public void setSelectedGeofence(IRectangle rectangle)
    {
        IRectangle old = selectedGeofence.get();
        if (valueChanged(old, rectangle))
        {
            selectedGeofence.set(rectangle);
            pcs.firePropertyChange(SELECTED_GEOFENCE, old, rectangle);
        }
    }

    @Override
    public void setPosition(Position position)
    {
        Position old = this.position.get();
        if (valueChanged(old, position))
        {
            this.position.set(position);
            pcs.firePropertyChange(POSITION, old, position);
        }
    }

    @Override
    public void setVisualizationMode(Visualization visualizationMode)
    {
        Visualization old = this.visualizationMode.get();
        if (valueChanged(old, visualizationMode))
        {
            this.visualizationMode.set(visualizationMode);
            pcs.firePropertyChange(VISUALIZATION_MODE, old, visualizationMode);
        }
    }

    @Override
    public void setTileBoundariesVisible(boolean visible)
    {
        boolean old = tileBoundariesVisible.get();
        if (valueChanged(old, visible))
        {
            tileBoundariesVisible.set(visible);
            pcs.firePropertyChange(TILE_BOUNDARIES_VISIBLE, old, visible);
        }
    }

    @Override
    public void setImageCalculationEnabled(boolean enabled)
    {
        boolean old = imageCalculationsEnabled.get();
        if (valueChanged(old, enabled))
        {
            imageCalculationsEnabled.set(enabled);
            pcs.firePropertyChange(ENABLE_IMAGE_CALCULATIONS, old, enabled);
        }
    }

    @Override
    public void setColorForResolution(double resolution, Color color)
    {
        Color old = resolutionColorMap.get(resolution);
        if (valueChanged(old, color))
        {
            resolutionColorMap.put(resolution, color);
            pcs.firePropertyChange(RESOLUTION_COLOR, old, color);
        }
    }

    @Override
    public void clearSurfaceShapes()
    {
        surfaceShapes.clear();
    }

    @Override
    public void clearSurfaceImages()
    {
        surfaceImages.clear();
    }

    @Override
    public void clearResolutionShape()
    {
        resolutionShape.set(null);
    }

    @Override
    public synchronized void addListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public synchronized void removeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public List<IRaptorLayer> getElevationLayers()
    {
        return Collections.unmodifiableList(elevationLayers);
    }

    @Override
    public Color getElevationLayerColor(IRaptorLayer elevationLayer)
    {
        return layerColors.get(elevationLayer);
    }

    @Override
    public SurfaceMultiSector getSurfaceShape(IRaptorLayer elevationLayer)
    {
        return surfaceShapes.get(elevationLayer);
    }

    @Override
    public TerrainSurfaceImage getSurfaceImage(IRaptorLayer elevationLayer)
    {
        return surfaceImages.get(elevationLayer);
    }

    @Override
    public Iterable<IRaptorLayer> getVisibleElevationLayers()
    {
        List<IRaptorLayer> visibleElevationLayers = new ArrayList<>();
        for (IRaptorLayer elevationLayer : elevationLayers)
        {
            if (isElevationLayerVisible(elevationLayer))
            {
                visibleElevationLayers.add(elevationLayer);
            }
        }
        return visibleElevationLayers;
    }

    @Override
    public boolean isElevationLayerVisible(IRaptorLayer elevationLayer)
    {
        if (layerVisibility.containsKey(elevationLayer))
        {
            return layerVisibility.get(elevationLayer);
        }

        // Default to visible elevation layers
        return true;
    }

    @Override
    public RenderableElevationLayer getTerrainLayer()
    {
        return terrainLayer.get();
    }

    @Override
    public IRaptorLayer getRaptorTerrainLayer()
    {
        return raptorTerrainLayer.get();
    }

    @Override
    public IRaptorLayer getRaptorElevationBoundsLayer()
    {
        return raptorElevationBoundsLayer.get();
    }

    @Override
    public RenderableLayer getElevationBoundsLayer()
    {
        return elevationBoundsLayer.get();
    }

    @Override
    public Iterable<TerrainSurfaceImage> getSurfaceImages()
    {
        return new HashSet<>(surfaceImages.values());
    }

    @Override
    public Iterable<SurfaceMultiSector> getSurfaceShapes()
    {
        return new HashSet<>(surfaceShapes.values());
    }

    @Override
    public Double getElevationLayerAvailability(IRaptorLayer elevationLayer)
    {
        return layerAvailability.get(elevationLayer);
    }

    @Override
    public double getSurfaceOpacity()
    {
        return surfaceOpacity.get();
    }

    @Override
    public IRectangle getSelectedGeofence()
    {
        return selectedGeofence.get();
    }

    @Override
    public TileCollection getTileCollection(IRaptorLayer elevationLayer)
    {
        ITakElevationModel elevationModel = elevationLayerManager.getElevationModel(elevationLayer);
        return elevationModel != null ? tileManager.getCollectionForModel(elevationModel) : null;
    }

    @Override
    public Position getPosition()
    {
        return position.get();
    }

    @Override
    public boolean areImageCalculationsEnabled()
    {
        return imageCalculationsEnabled.get();
    }

    @Override
    public boolean areTileBoundariesVisible()
    {
        return tileBoundariesVisible.get();
    }

    @Override
    public Visualization getVisualizationMode()
    {
        return visualizationMode.get();
    }

    @Override
    public Iterable<Double> getResolutions()
    {
        return new HashSet<>(resolutionColorMap.keySet());
    }

    @Override
    public SurfaceMultiSector getNegativeSpaceShape()
    {
        return negativeSpaceShape.get();
    }

    @Override
    public Color getColorForResolution(double resolution)
    {
        return resolutionColorMap.get(resolution);
    }

    @Override
    public void flushTiles()
    {
        tileManager.flushAll();
    }

    @Override
    public ResolutionRenderable getResolutionShape()
    {
        return resolutionShape.get();
    }

    @Override
    public boolean areSurfaceImagesAvailable()
    {
        return !surfaceImages.isEmpty();
    }

    protected boolean valueChanged(Object newValue, Object oldValue)
    {
        // Make sure the value are different unless initial restore
        return (newValue != null ? !equalByType(newValue, oldValue) : oldValue != null);
    }

    private boolean equalByType(Object newValue, Object oldValue)
    {
        if (newValue instanceof Object[])
        {
            return oldValue instanceof Object[] && Arrays.equals((Object[]) newValue, (Object[]) oldValue);
        }
        return newValue.equals(oldValue);
    }
}
