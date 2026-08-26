package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.RenderableFeatureManager;
import gov.takx.core.globe.tak.layer.opengl.GLRenderableFeatureLayer;
import jakarta.inject.Inject;

/**
 * A layer that represents and controls the {@link RenderableFeatureManager}.
 *
 * @since 3.5
 */
@PseudoBean
public class RenderableFeatureLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLRenderableFeatureLayer.SPI);
    }

    @Inject
    private RenderableFeatureManager renderableFeatureManager;

    public RenderableFeatureLayer()
    {
        super("Renderable Feature Manager Layer");
    }

    /**
     * Get the renderable feature layer manager.
     * @return the renderable feature layer manager
     */
    public RenderableFeatureManager getRenderableFeatureManager()
    {
        return renderableFeatureManager;
    }
}
