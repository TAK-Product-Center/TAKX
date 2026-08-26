package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.animation.Animator;
import gov.takx.core.globe.tak.animation.IAnimationService;
import gov.takx.core.globe.tak.layer.AnimationLayer;

/**
 * Process animations served by the {@link IAnimationService}.
 *
 * @since 3.4.1
 */
public class GLAnimationLayer extends GLAbstractLayer2
{
    public final static GLLayerSpi2 SPI = new GLLayerSpi2()
    {
        @Override
        public int getPriority()
        {
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> object)
        {
            if (object.second instanceof AnimationLayer animationLayer)
            {
                return new GLAnimationLayer(object.first, animationLayer);
            }
            return null;
        }
    };
    private final AnimationLayer animationLayer;

    private Animator activeAnimator;

    protected GLAnimationLayer(MapRenderer surface, AnimationLayer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
        animationLayer = subject;
    }

    @Override
    protected void drawImpl(GLMapView view, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            gov.tak.api.engine.map.MapRenderer<?> mapRenderer = LegacyAdapters.adapt(view);
            IAnimationService animationService = animationLayer.getAnimationService();
            Animator nextAnimator = animationService.peekAnimation(mapRenderer);
            if (activeAnimator != nextAnimator)
            {
                if (activeAnimator != null) activeAnimator.stop();
                activeAnimator = nextAnimator;
                if (activeAnimator != null) activeAnimator.start();
            }
            if (activeAnimator != null)
            {
                if (activeAnimator.hasNext())
                {
                    activeAnimator.next();
                } else
                {
                    activeAnimator.stop();
                    activeAnimator = null;
                    animationService.pollAnimation(mapRenderer);
                }
            }
        }
    }
}
