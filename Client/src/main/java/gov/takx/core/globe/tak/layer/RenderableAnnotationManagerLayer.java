package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.RenderableAnnotationManager;
import gov.takx.core.globe.tak.layer.opengl.GLRenderableAnnotationManagerLayer;

/**
 * A layer that represents and controls the {@link RenderableAnnotationManager}.
 *
 * @since 5.1
 */
@PseudoBean
public class RenderableAnnotationManagerLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLRenderableAnnotationManagerLayer.SPI);
    }

    private final RenderableAnnotationManager renderableAnnotationManager;

    public RenderableAnnotationManagerLayer(RenderableAnnotationManager renderableAnnotationManager)
    {
        super("Renderable Annotation Manager Layer");
        this.renderableAnnotationManager = renderableAnnotationManager;
    }

    /**
    * Get the renderable annotation manager.
    * @return the renderable annotation manager
    */
    public RenderableAnnotationManager getRenderableAnnotationManager()
    {
        return renderableAnnotationManager;
    }
}
