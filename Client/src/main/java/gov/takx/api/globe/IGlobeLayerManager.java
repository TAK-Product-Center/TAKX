package gov.takx.api.globe;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.core.globe.layers.IGlobeLayer;

import java.util.List;

/**
 */
public interface IGlobeLayerManager extends IRaptorShutdownListener
{
    IGlobeLayer getGlobeLayer(IRaptorLayer raptorLayer);

    List<IGlobeLayer> getGlobeLayers();

    /**
     * @return The number of globes on which the given layer is being used
     * @since 3.4
     */
    int getLayerUseCount(IRaptorLayer raptorLayer);
}
