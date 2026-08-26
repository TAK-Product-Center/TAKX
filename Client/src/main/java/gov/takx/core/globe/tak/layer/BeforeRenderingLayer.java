package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.core.globe.tak.layer.opengl.GLBeforeRenderingLayer;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLBeforeRenderingLayer}.
 * 
 * @since 3.5
 */
public class BeforeRenderingLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLBeforeRenderingLayer.SPI);
    }

    public BeforeRenderingLayer()
    {
        super("Before Rendering Layer");
    }
}
