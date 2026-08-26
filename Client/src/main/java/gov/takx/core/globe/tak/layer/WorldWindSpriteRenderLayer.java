package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.core.globe.tak.layer.opengl.GLWorldWindRenderLayer;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLWorldWindRenderLayer}.
 *
 * @since 3.4
 */
public class WorldWindSpriteRenderLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLWorldWindRenderLayer.SPI);
    }

    public WorldWindSpriteRenderLayer()
    {
        super("WorldWind Sprite Render Layer");
    }
}
