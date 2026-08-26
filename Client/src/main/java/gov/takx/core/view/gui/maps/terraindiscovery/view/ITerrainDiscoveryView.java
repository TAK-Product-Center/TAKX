package gov.takx.core.view.gui.maps.terraindiscovery.view;

import gov.takx.api.layer.IRaptorLayer;

import java.util.List;

/**
 */
public interface ITerrainDiscoveryView
{
    void setVisible(boolean visible);

    void setPreparingEnabled(boolean enable);

    void setElevationLayers(List<IRaptorLayer> elevationLayers);

    void setElevationLayerCoverage(IRaptorLayer elevationLayer, String coverage);

    void setElevationLayerResolution(IRaptorLayer elevationLayer, String resolution);

    void setElevationAtMouseCursor(IRaptorLayer elevationAtMouseCursor, String elevation);

    void setProgressStarted();

    void setProgressUpdated(double progress);

    void setProgressText(String text);

    void setProgressStopped();

    void clearAll();

    void clearTemp();

    void updateElevationLayerOrder(List<IRaptorLayer> elevationLayers);

    void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible);
}
