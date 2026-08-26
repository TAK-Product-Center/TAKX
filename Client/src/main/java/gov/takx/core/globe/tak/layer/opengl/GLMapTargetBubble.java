package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayer3;
import com.atakmap.map.layer.opengl.GLLayerFactory;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.platform.layers.MagnifierLayer;
import gov.tak.platform.layers.opengl.GLCrosshairLayer;
import gov.tak.platform.layers.opengl.GLMagnifierLayer;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.core.globe.tak.layer.MapTargetBubble;
import gov.takx.core.globe.tak.layer.MapTargetBubble.OnCrosshairColorChangedListener;
import gov.takx.core.globe.tak.layer.MapTargetBubble.OnLocationChangedListener;
import gov.takx.core.globe.tak.layer.MapTargetBubble.OnScaleChangedListener;
import jakarta.annotation.Nonnull;

/**
 * Render a magnified bubble view for precise targeting.
 *
 * @since 5.1
 */
public class GLMapTargetBubble implements OnLocationChangedListener, OnScaleChangedListener, OnCrosshairColorChangedListener, GLLayer3, MapControl, MapRenderer2.OnCameraChangedListener2
{
    static
    {
        GLLayerFactory.register(GLCrosshairLayer.SPI);
    }

    public final static GLLayerSpi2 SPI2 = new GLLayerSpi2()
    {
        @Override
        public int getPriority()
        {
            // MapTargetBubble : Layer
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> arg)
        {
            if (!(arg.second instanceof MapTargetBubble mapTargetBubble)) return null;
            return new GLMapTargetBubble(arg.first, mapTargetBubble);
        }
    };

    private final MapTargetBubble subject;
    private final GLMapView renderCtx;
    private final GLMagnifierLayer impl;
    private final GLCrosshairLayer crosshair;

    // GLMapView interface, for backwards API compatibility
    protected float focusx;
    protected float focusy;
    protected int top;

    public GLMapTargetBubble(final MapRenderer surface, MapTargetBubble subject)
    {
        impl = new GLMagnifierLayer(surface, MarshalManager.marshal(subject, MapTargetBubble.class, MagnifierLayer.class));
        crosshair = (GLCrosshairLayer) GLLayerFactory.create4(surface, subject.getCrosshair());
        renderCtx = (GLMapView) surface;
        this.subject = subject;
    }

    public MapRenderer2 getRenderer()
    {
        return impl.getRenderer();
    }

    @Override
    public void onMapTargetBubbleLocationChanged(final MapTargetBubble bubble)
    {
        // handled by `impl`
    }

    @Override
    public void onMapTargetBubbleScaleChanged(MapTargetBubble bubble)
    {
        // handled by `impl`
    }

    @Override
    public void onMapTargetBubbleCrosshairColorChanged(MapTargetBubble bubble)
    {
    }

    @Override
    public void draw(GLMapView view)
    {
        draw(view, -1);
    }

    @Override
    public void draw(GLMapView view, int renderPass)
    {
        if (!MathUtils.hasBits(renderPass, getRenderPass())) return;

        impl.draw(view, renderPass);
        final GLMapView renderer = (GLMapView) getRenderer();
        focusx = renderer.currentPass.focusx;
        focusy = renderer.currentPass.focusy;
        top = renderer.currentPass.top;

        crosshair.draw(view, GLMapView.RENDER_PASS_UI);
    }

    @Override
    public void release()
    {
        impl.release();
        crosshair.release();
    }

    @Override
    public int getRenderPass()
    {
        return GLMapView.RENDER_PASS_UI;
    }

    @Override
    public Layer getSubject()
    {
        return subject;
    }

    @Override
    public void start()
    {
        subject.addOnLocationChangedListener(this);
        subject.addOnScaleChangedListener(this);
        subject.addOnCrosshairColorChangedListener(this);
        renderCtx.registerControl(subject, this);
        renderCtx.addOnCameraChangedListener(this);
        setLocation((MagnifierLayer) impl.getSubject(), renderCtx);

        impl.start();
        crosshair.start();
    }

    @Override
    public void stop()
    {
        crosshair.stop();
        impl.stop();

        renderCtx.removeOnCameraChangedListener(this);
        renderCtx.unregisterControl(subject, this);
        subject.removeOnCrosshairColorChangedListener(this);
        subject.removeOnLocationChangedListener(this);
        subject.removeOnScaleChangedListener(this);
    }

    @Override
    public void onCameraChanged(MapRenderer2 mapRenderer2)
    {
        MagnifierLayer magnifier = (MagnifierLayer) impl.getSubject();
        setLocation(magnifier, renderCtx);
    }

    @Override
    public void onCameraChangeRequested(MapRenderer2 mapRenderer2)
    {
        MagnifierLayer magnifier = (MagnifierLayer) impl.getSubject();
        setLocation(magnifier, renderCtx);
    }

    private static void setLocation(@Nonnull MagnifierLayer magnifier, @Nonnull GLMapView glMapView)
    {
        MapSceneModel scene = glMapView.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        GeoPoint focus = scene.mapProjection.inverse(scene.camera.target, null);
        if (focus != null)
        {
            magnifier.setLocation(focus.getLatitude(), focus.getLongitude());
        }
    }
}
