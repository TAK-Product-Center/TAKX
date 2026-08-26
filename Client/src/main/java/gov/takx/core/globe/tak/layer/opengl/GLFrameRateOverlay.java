package gov.takx.core.globe.tak.layer.opengl;

import android.graphics.Typeface;
import android.util.Pair;
import com.atakmap.android.maps.MapTextFormat;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLText;
import gov.takx.core.globe.tak.layer.FrameRateOverlay;

/**
 * Render the current FPS to the screen.
 *
 * @since 3.4
 */
public class GLFrameRateOverlay extends GLAbstractLayer2
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
            if (!(object.second instanceof FrameRateOverlay)) return null;
            return new GLFrameRateOverlay(object.first, object.second);
        }
    };

    private static final String FPS_FORMAT = "%03.2f FPS";
    private static final float PADDING = 8f;

    GLText textRenderer;

    long count;
    long timeCall;
    double currentFramerate;
    long lastReport;

    GLFrameRateOverlay(MapRenderer surface, Layer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_UI);
    }

    @Override
    protected void drawImpl(GLMapView view, int renderPass)
    {
        if (!MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_UI)) return;

        if (textRenderer == null) textRenderer = GLText.getInstance(new MapTextFormat(Typeface.DEFAULT_BOLD, true, 24));

        if (count == 0)
        {
            timeCall = view.animationLastTick;
        } else if (count > 1_000)
        {
            currentFramerate = (1_000_000.0 / (view.animationLastTick - timeCall));
            timeCall = view.animationLastTick;
            count = 0;
            lastReport = timeCall;
        } else if ((view.animationLastTick - lastReport) > 1000)
        {
            currentFramerate = ((count * 1000.0d) / (view.animationLastTick - timeCall));
            lastReport = view.animationLastTick;

            if ((view.animationLastTick - timeCall) > 5000)
            {
                timeCall = view.animationLastTick;
                count = 0;
                lastReport = timeCall;
            }
        }
        count++;

        final String fpsString = String.format(FPS_FORMAT, currentFramerate);
        final float tx = view.currentPass.right - PADDING - textRenderer.getStringWidth(fpsString);
        final float ty = view.currentPass.top - PADDING - textRenderer.getCharHeight();

        GLES20FixedPipeline.glPushMatrix();
        GLES20FixedPipeline.glTranslatef(tx, ty, 0f);
        textRenderer.draw(fpsString, 1f, 1f, 1f, 1f);
        GLES20FixedPipeline.glPopMatrix();
    }

    @Override
    public void release()
    {
        super.release();

        textRenderer = null;
    }
}
