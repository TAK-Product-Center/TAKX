package gov.takx.core.globe.tak.layer.opengl;

import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.opengl.GLMapView;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;

/**
 * Abstract class that provides a {@link TakWorldWindow} and a {@link ApplicationSceneController} to GLLayers.
 * @since 3.5
 */
abstract class GLAbstractWorldWindLayer extends GLAbstractLayer2
{
    GLAbstractWorldWindLayer(MapRenderer surface, Layer subject, int renderPassMask)
    {
        super(surface, subject, renderPassMask);
    }

    @Override
    protected final void drawImpl(GLMapView view, int renderPass)
    {
        TakWorldWindow wwd = (TakWorldWindow) view.getRenderSurface();
        ApplicationSceneController sc = (ApplicationSceneController) wwd.getSceneController();
        IApplicationGlobe globe = (IApplicationGlobe) sc.getDrawContext().getValue(ApplicationSceneController.GLOBE_DELEGATE);
        if (globe != null) drawImpl(view, globe, wwd, sc, renderPass);
    }

    protected abstract void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass);
}
