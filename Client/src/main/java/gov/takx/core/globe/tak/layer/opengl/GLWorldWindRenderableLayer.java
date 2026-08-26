package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.render.Renderable;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.RenderableManager;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import gov.takx.platform.layer.WorldWindRenderableLayer;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Collection;

/**
 * Provide a collection of {@link Renderable} from a {@link WorldWindRenderableLayer} to the {@link RenderableManager}.
 *
 * @since 5.2
 */
@PseudoBean
public class GLWorldWindRenderableLayer extends GLAbstractWorldWindLayer
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
            if (object.second instanceof WorldWindRenderableLayer layer)
            {
                return new GLWorldWindRenderableLayer(object.first, layer);
            }
            return null;
        }
    };

    @Inject
    private RenderableManager renderableManager;

    private final WorldWindRenderableLayer worldWindRenderableLayer;

    protected GLWorldWindRenderableLayer(MapRenderer surface, WorldWindRenderableLayer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
        worldWindRenderableLayer = subject;
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            Collection<Renderable> renderables = worldWindRenderableLayer.getRenderables();

            // Apply pick enabled
            boolean pickEnabled = worldWindRenderableLayer.isPickEnabled();
            renderables.stream()
                    .filter(AVList.class::isInstance)
                    .map(AVList.class::cast)
                    .forEach(r -> r.setValue(AVKey.PICK_ENABLED, pickEnabled));

            for (Renderable renderable : renderables)
            {
                renderableManager.offerRenderable(renderable, globe);
            }
        }
    }
}
