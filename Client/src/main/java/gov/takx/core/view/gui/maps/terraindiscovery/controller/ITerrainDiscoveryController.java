package gov.takx.core.view.gui.maps.terraindiscovery.controller;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.shapes.IRectangle;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.ITerrainRenderable;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.SurfaceMultiSector;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.TerrainSurfaceImage;
import gov.takx.core.view.gui.maps.terraindiscovery.model.ITerrainDiscoveryModel;
import gov.takx.core.view.gui.maps.terraindiscovery.view.ITerrainDiscoveryView;

import java.awt.Color;
import java.beans.PropertyChangeListener;

/**
 */
public interface ITerrainDiscoveryController extends PropertyChangeListener
{
    ITerrainDiscoveryModel getModel();

    ITerrainDiscoveryView getView();

    void show();

    /**
     * Initialize controller for calculation and visualization.
     */
    void initialize();

    /**
     * Clear any temporary data in the controller.  Typically called when hiding the Terrain Discovery dialog.
     */
    void clearTemp();

    /**
     * Clear all data in the controller. Typically called when a project closes.
     */
    void clearAll();

    void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible);

    void setElevationLayerColor(IRaptorLayer elevationLayer, Color color);

    void setSurfaceShape(IRaptorLayer elevationLayer, SurfaceMultiSector shape);

    void setSurfaceImage(IRaptorLayer elevationLayer, TerrainSurfaceImage image);

    void setElevationLayerAvailability(IRaptorLayer elevationLayer, Double availability);

    void clearVisualizations();

    void createAnalyticsLayers();

    void removeAnalyticsLayers();

    void setOpacity(double opacity);

    void calculate();

    void cancelCalculation();

    void calculationComplete();

    void setSelectedGeofence(IRectangle geofence);

    void setProgressStarted();

    void setProgress(double progress);

    void setProgressText(String text);

    void setProgressStopped();

    void setPosition(Position position);

    Sector getBoundingSector();

    void updateElevationLayerCoverage(IRaptorLayer elevationLayer);

    void setElevationLayerToTop(IRaptorLayer elevationLayer);

    void setTileBoundsVisible(boolean visible);

    Iterable<ITerrainRenderable> getTerrainRenderables();

    void setUseCoverageVisualization();

    void setUseResolutionVisualization();

    void setUseTerrainImageVisualization();

    Color getColorForResolution(double resolution);

    void updateTileCollectionsEnabled();

    boolean isProjectClosing();
}
