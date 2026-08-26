package gov.takx.core.globe.tak.layer;

import com.atakmap.map.layer.AbstractLayer;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.animation.IAnimationService;
import gov.takx.core.globe.tak.layer.opengl.GLAnimationLayer;
import jakarta.inject.Inject;

/**
 * A layer that represents and controls the {@link IAnimationService}.
 *
 * @since 3.4.1
 */
@PseudoBean
public class AnimationLayer extends AbstractLayer
{
    static
    {
        GLLayerFactory.register(GLAnimationLayer.SPI);
    }

    @Inject
    private IAnimationService animationService;

    public AnimationLayer()
    {
        super("Animation Layer");
    }

    /**
     * Get the animation service.
     * @return the animation service
     * @since 3.5
     */
    public IAnimationService getAnimationService()
    {
        return animationService;
    }
}
