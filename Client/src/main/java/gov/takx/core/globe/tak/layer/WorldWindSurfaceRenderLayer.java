package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.core.globe.tak.layer.opengl.GLWorldWindSurfaceRenderLayer;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLWorldWindSurfaceRenderLayer}.
 *
 * @since 3.4
 */
public class WorldWindSurfaceRenderLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLWorldWindSurfaceRenderLayer.SPI);
    }

    public WorldWindSurfaceRenderLayer()
    {
        super("WorldWind Surface Render Layer");
    }
}
