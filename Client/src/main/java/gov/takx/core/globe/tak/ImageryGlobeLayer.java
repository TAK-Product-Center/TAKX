package gov.takx.core.globe.tak;

import com.atakmap.map.layer.Layer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.layers.IGlobeLayer;
import jakarta.inject.Inject;

/**
 * Custom {@link IGlobeLayer} for rendering TAK imagery layers.
 *
 * @since 5.1
 */
@PseudoBean
public class ImageryGlobeLayer extends GlobeLayer
{
    @Inject
    private ApplicationGlobeManager globeManager;

    public ImageryGlobeLayer(IRaptorLayer raptorLayer, Layer layer)
    {
        super(raptorLayer, layer);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        // Encourage TAK kernel to redraw surface
        globeManager.repaintAllGlobes();
        super.setEnabled(enabled);
    }
}
