package gov.takx.core.globe.tak;

import com.atakmap.map.layer.opengl.GLLayerFactory;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.tak.layer.opengl.GLWorldWindRenderableLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * A helper class to manage WW renderables in the TAK ecosystem.
 * @since 5.1
 */
@ApplicationScoped
public class RenderableManager
{
    static
    {
        GLLayerFactory.register(GLWorldWindRenderableLayer.SPI);
    }

    private final RenderableAnnotationManager renderableAnnotationManager;
    private final SurfaceImageManager surfaceImageManager;
    private final RenderableFeatureManager renderableFeatureManager;
    private final ScreenAnnotationManager screenAnnotationManager;

    @Inject
    RenderableManager(RenderableAnnotationManager renderableAnnotationManager, SurfaceImageManager surfaceImageManager, RenderableFeatureManager renderableFeatureManager, ScreenAnnotationManager screenAnnotationManager)
    {
        this.renderableAnnotationManager = renderableAnnotationManager;
        this.surfaceImageManager = surfaceImageManager;
        this.renderableFeatureManager = renderableFeatureManager;
        this.screenAnnotationManager = screenAnnotationManager;
    }

    /**
     * Offer a WorldWind {@link Renderable} to be rendered by TAK. If TAK is able to render the object it will return true and no further rendering is necessary.
     * @param renderable the ordered renderable
     * @param dc the draw context
     * @return true if TAK was able to render the object
     * @deprecated Use {@link #offerRenderable(Renderable, IApplicationGlobe)}
     */
    @Deprecated(since = "5.4")
    @DeprecatedApi(since = "5.4")
    public boolean offerRenderable(Renderable renderable, DrawContext dc)
    {
        IApplicationGlobe globe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
        return globe != null && offerRenderable(renderable, globe);
    }

    /**
     * Offer a WorldWind {@link Renderable} to be rendered by TAK. If TAK is able to render the object it will return true and no further rendering is necessary.
     * @param renderable the ordered renderable
     * @param globe the globe
     * @return true if TAK was able to render the object
     * @since 5.4
     */
    public boolean offerRenderable(Renderable renderable, @Nonnull IApplicationGlobe globe)
    {
        if (renderable instanceof OrderedRenderable orderedRenderable) renderable = OrderedRenderableUtils.extractRenderable(orderedRenderable);
        // Set the renderable's current layer for referencing by TAK
        Layer currentLayer = globe.getWorldWindow().getSceneController().getDrawContext().getCurrentLayer();
        if (renderable instanceof AVList avList) avList.setValue(AVKey.LAYER, currentLayer);
        return (renderableAnnotationManager.offerRenderable(renderable, globe)) ||
                (surfaceImageManager.offerRenderable(renderable, globe)) ||
                (renderableFeatureManager.offerRenderable(renderable, globe)) ||
                (screenAnnotationManager.offerRenderable(renderable, globe));
    }
}
