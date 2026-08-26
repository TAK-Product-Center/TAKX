package gov.takx.core.view.gui.maps.terraindiscovery.model;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.terrain.experimental.proposed.tile.TileCollection;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.RenderableElevationLayer;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.ResolutionRenderable;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.SurfaceMultiSector;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.TerrainSurfaceImage;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 */
public interface ITerrainDiscoveryModel
{
    public static final String RAPTOR_ELEVATION_BOUNDS_LAYER = "raptorElevationBoundsLayer";
    public static final String ELEVATION_BOUNDS_LAYER = "elevationBoundsLayer";
    public static final String RAPTOR_TERRAIN_LAYER = "raptorTerrainLayer";
    public static final String ELEVATION_LAYER_AVAILABILITY = "elevationLayerAvailability";
    public static final String TERRAIN_LAYER = "terrainLayer";
    public static final String ELEVATION_LAYERS = "elevationLayers";
    public static final String ELEVATION_LAYER_VISIBLE = "elevationLayerVisible";
    public static final String ELEVATION_LAYER_COLOR = "elevationLayerColor";
    public static final String SURFACE_SHAPE = "surfaceShape";
    public static final String SURFACE_IMAGE = "surfaceImage";
    public static final String SURFACE_SHAPES_VISIBLE = "surfaceShapesVisible";
    public static final String SURFACE_IMAGES_VISIBLE = "surfaceImagesVisible";
    public static final String SURFACE_OPACITY = "surfaceOpacity";
    public static final String SELECTED_GEOFENCE = "selectedGeofence";
    public static final String POSITION = "position";
    public static final String ELEVATION_LAYER_TOP = "elevationLayerTop";
    public static final String TILE_BOUNDARIES_VISIBLE = "tileBoundariesVisible";
    public static final String ENABLE_IMAGE_CALCULATIONS = "enableImageCalculations";
    public static final String RESOLUTION_SHAPE = "resolutionShape";
    public static final String VISUALIZATION_MODE = "visualizationMode";
    public static final String RESOLUTION_COLOR = "resolutionColor";
    public static final String NEGATIVE_SPACE_SHAPE = "negativeSpaceShape";

    public static enum Visualization
    {
        COVERAGE, RESOLUTION, TERRAIN_IMAGE
    }

    /**
     * Setters *
     */
    void setElevationBoundsLayer(IRaptorLayer raptorElevationBoundsLayer, RenderableLayer elevationBoundsLayer);

    void setTerrainLayer(IRaptorLayer raptorTerrainLayer, RenderableElevationLayer terrainLayer);

    void setElevationLayerToTop(IRaptorLayer elevationLayer);

    void setElevationLayers(List<IRaptorLayer> elevationLayers);

    void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible);

    void setElevationLayerColor(IRaptorLayer elevationLayer, Color color);

    void setSurfaceShape(IRaptorLayer elevationLayer, SurfaceMultiSector shape);

    void setNegativeSpaceShape(SurfaceMultiSector shape);

    void setSurfaceImage(IRaptorLayer elevationLayer, TerrainSurfaceImage image);

    void setResolutionShape(ResolutionRenderable layeredShape);

    void setSurfaceOpacity(double opacity);

    void setElevationLayerAvailability(IRaptorLayer elevationLayer, double availability);

    void setSelectedGeofence(IRectangle rectangle);

    void setPosition(Position position);

    void setVisualizationMode(Visualization visualizationMode);

    void setTileBoundariesVisible(boolean visible);

    void setImageCalculationEnabled(boolean enabled);

    void setColorForResolution(double resolution, Color color);

    void clearSurfaceShapes();

    void clearSurfaceImages();

    void clearResolutionShape();

    void addListener(PropertyChangeListener listener);

    void removeListener(PropertyChangeListener listener);

    /**
     * Clear temporary data.  Typically called if the Terrain Discovery dialog is being hidden.
     */
    void clearTemp();

    /**
     * Clear/reset all data. This will be called upon layer instantiation and project close.
     */
    void clearAll();

    /**
     * Getters *
     */

    List<IRaptorLayer> getElevationLayers();

    Color getElevationLayerColor(IRaptorLayer elevationLayer);

    SurfaceMultiSector getSurfaceShape(IRaptorLayer elevationLayer);

    TerrainSurfaceImage getSurfaceImage(IRaptorLayer elevationLayer);

    Iterable<IRaptorLayer> getVisibleElevationLayers();

    boolean isElevationLayerVisible(IRaptorLayer elevationLayer);

    RenderableElevationLayer getTerrainLayer();

    IRaptorLayer getRaptorTerrainLayer();

    IRaptorLayer getRaptorElevationBoundsLayer();

    RenderableLayer getElevationBoundsLayer();

    Iterable<TerrainSurfaceImage> getSurfaceImages();

    Iterable<SurfaceMultiSector> getSurfaceShapes();

    Double getElevationLayerAvailability(IRaptorLayer elevationLayer);

    double getSurfaceOpacity();

    IRectangle getSelectedGeofence();

    TileCollection getTileCollection(IRaptorLayer elevationLayer);

    Position getPosition();

    ResolutionRenderable getResolutionShape();

    boolean areImageCalculationsEnabled();

    boolean areTileBoundariesVisible();

    boolean areSurfaceImagesAvailable();

    Visualization getVisualizationMode();

    Iterable<Double> getResolutions();

    SurfaceMultiSector getNegativeSpaceShape();

    Color getColorForResolution(double resolution);

    void flushTiles();
}
