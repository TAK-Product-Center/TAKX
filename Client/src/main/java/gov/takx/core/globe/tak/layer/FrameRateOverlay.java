package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.layer.builder.GenericLayerBuilder;
import gov.takx.core.globe.tak.layer.opengl.GLFrameRateOverlay;
import gov.takx.platform.preferences.PreferenceEntry;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLFrameRateOverlay}.
 * 
 * @since 3.4
 */
public class FrameRateOverlay extends AbstractLayer
{
    private static final String LAYER_NAME = "Frame Rate";

    /**
     * Layer configuration for adding this layer.
     * @since 5.4
     */
    public static final LayerConfiguration layerConfiguration = GenericLayerBuilder.builder()
            .setName(LAYER_NAME)
            .setDisplayName(LAYER_NAME)
            .setVisible(PreferenceEntry.DEV_MODE_CONTINUOUS_RENDERING_ENABLED.getBooleanValue())
            .setLayerGroup(LayerGroup.UNLISTED_OVERLAY)
            .setLayer(new FrameRateOverlay())
            .build();

    static
    {
        GLLayerFactory.register(GLFrameRateOverlay.SPI);
    }

    public FrameRateOverlay()
    {
        super(LAYER_NAME);
    }
}
