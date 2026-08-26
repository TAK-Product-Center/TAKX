package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.ScreenAnnotationManager;
import gov.takx.core.globe.tak.layer.opengl.GLScreenAnnotationLayer;
import jakarta.inject.Inject;

/**
 * A layer that represents and controls the {@link ScreenAnnotationManager}.
 *
 * @since 3.5
 */
@PseudoBean
public class ScreenAnnotationLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLScreenAnnotationLayer.SPI);
    }

    @Inject
    private ScreenAnnotationManager screenAnnotationManager;

    public ScreenAnnotationLayer()
    {
        super("Screen Annotation Layer");
    }

    /**
     * Get the screen annotation manager.
     * @return the screen annotation manager
     */
    public ScreenAnnotationManager getScreenAnnotationManager()
    {
        return screenAnnotationManager;
    }
}
