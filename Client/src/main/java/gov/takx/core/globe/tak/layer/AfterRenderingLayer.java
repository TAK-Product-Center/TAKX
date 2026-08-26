package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.core.globe.tak.layer.opengl.GLAfterRenderingLayer;

/**
 * {@link com.atakmap.map.layer.Layer} implementation for {@link GLAfterRenderingLayer}.
 * 
 * @since 3.5
 */
public class AfterRenderingLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLAfterRenderingLayer.SPI);
    }

    public AfterRenderingLayer()
    {
        super("After Rendering Layer");
    }
}
