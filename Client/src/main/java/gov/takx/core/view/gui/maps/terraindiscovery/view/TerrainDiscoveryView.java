package gov.takx.core.view.gui.maps.terraindiscovery.view;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;

import java.util.List;

/**
 */
public class TerrainDiscoveryView implements ITerrainDiscoveryView
{
    private final TerrainDiscoveryPanel terrainDiscoveryPanel;
    private final TerrainDiscoveryDialog terrainDiscoveryDialog;

    public TerrainDiscoveryView(ITerrainDiscoveryController controller)
    {
        terrainDiscoveryPanel = new TerrainDiscoveryPanel(controller);
        terrainDiscoveryDialog = new TerrainDiscoveryDialog(
                controller,
                terrainDiscoveryPanel,
                new TerrainDiscoveryPositionListener(controller));
        terrainDiscoveryPanel.setDialog(terrainDiscoveryDialog);
    }

    @Override
    @InvokeOnEDT
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            // Attempt to re-attach the geofence shape listener.  If it is already attached, this method call should
            // detach then attach - meaning it shouldn't add the same listener twice.
            terrainDiscoveryPanel.reattachShapeListener();
            terrainDiscoveryPanel.refreshToModel();
        }

        terrainDiscoveryDialog.setVisible(visible);

        if (visible)
        {
            terrainDiscoveryDialog.toFront();
        }
    }

    @Override
    @InvokeOnEDT
    public void setPreparingEnabled(boolean enable)
    {
        terrainDiscoveryPanel.setCalculationPreparationEnabled(enable);
    }

    @Override
    @InvokeOnEDT
    public void setElevationLayers(List<IRaptorLayer> elevationLayers)
    {
        terrainDiscoveryPanel.setElevationLayers(elevationLayers);
    }

    @Override
    @InvokeOnEDT
    public void setElevationLayerCoverage(IRaptorLayer elevationLayer, String coverage)
    {
        terrainDiscoveryPanel.setElevationLayerCoverage(elevationLayer, coverage);
    }

    @Override
    @InvokeOnEDT
    public void setElevationLayerResolution(IRaptorLayer elevationLayer, String resolution)
    {
        terrainDiscoveryPanel.setElevationLayerResolution(elevationLayer, resolution);
    }

    @Override
    @InvokeOnEDT
    public void setElevationAtMouseCursor(IRaptorLayer elevationLayer, String elevation)
    {
        terrainDiscoveryPanel.setElevationAtMouseCursor(elevationLayer, elevation);
    }

    @Override
    @InvokeOnEDT
    public void setProgressStarted()
    {
        terrainDiscoveryPanel.setProgressStarted();
    }

    @Override
    @InvokeOnEDT
    public void setProgressUpdated(double progress)
    {
        terrainDiscoveryPanel.setProgressUpdated(progress);
    }

    @Override
    @InvokeOnEDT
    public void setProgressText(String text)
    {
        terrainDiscoveryPanel.setProgressText(text);
    }

    @Override
    @InvokeOnEDT
    public void setProgressStopped()
    {
        terrainDiscoveryPanel.setProgressStopped();
    }

    @Override
    @InvokeOnEDT
    public void clearTemp()
    {
        terrainDiscoveryPanel.clearTemp();
    }

    @Override
    @InvokeOnEDT
    public void clearAll()
    {
        terrainDiscoveryDialog.setVisible(false);
        terrainDiscoveryPanel.clearAll();
    }

    @Override
    @InvokeOnEDT
    public void updateElevationLayerOrder(List<IRaptorLayer> elevationLayers)
    {
        terrainDiscoveryPanel.updateElevationModelOrder(elevationLayers);
    }

    @Override
    @InvokeOnEDT
    public void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible)
    {
        terrainDiscoveryPanel.setElevationLayerVisible(elevationLayer, visible);
    }
}
