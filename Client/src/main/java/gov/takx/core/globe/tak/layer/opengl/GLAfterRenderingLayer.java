package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.jogamp.opengl.GLAutoDrawable;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.layer.AfterRenderingLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Handle post-rendering WW events.
 *
 * @since 3.5
 */
public class GLAfterRenderingLayer extends GLAbstractWorldWindLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
            if (object.second instanceof AfterRenderingLayer)
            {
                return new GLAfterRenderingLayer(object.first, object.second);
            }
            return null;
        }
    };

    GLAfterRenderingLayer(MapRenderer surface, Layer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_UI);
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_UI))
        {
            sc.setValue(ApplicationSceneController.RENDER_PASS, renderPass);
            doAfterRendering(view, wwd);
        }
    }

    private void doAfterRendering(GLMapView view, TakWorldWindow wwd)
    {
        GLAutoDrawable drawable = (IWorldWindowGL) view.getRenderSurface();

        try
        {
            SceneController sc = wwd.getSceneController();

            if (sc == null)
            {
                String msg = "Scene controller is null at repaint";
                logger.error(msg);
                throw new IllegalStateException(msg);
            }

            Double frameTime = sc.getFrameTime();
            wwd.setValue("gov.nasa.worldwind.perfstat.FrameTime", frameTime);

            Double frameRate = sc.getFramesPerSecond();
            wwd.setValue("gov.nasa.worldwind.perfstat.FrameRate", frameRate);

            Iterable<Throwable> renderingExceptions = sc.getRenderingExceptions();
            if (renderingExceptions != null)
            {
                for (Throwable t : renderingExceptions)
                {
                    if (t != null)
                    {
                        wwd.callRenderingExceptionListeners(t);
                    }
                }
            }

            wwd.callRenderingListeners(new RenderingEvent(drawable, "gov.nasa.worldwind.RenderingEvent.AfterBufferSwap"));
            Position positionAtStart = (Position) sc.getValue(GLWorldWindRenderLayer.POSITION_AT_START);
            PickedObject selectionAtStart = (PickedObject) sc.getValue(GLWorldWindRenderLayer.SELECTION_AT_START);
            PickedObjectList boxSelectionAtStart = (PickedObjectList) sc.getValue(GLWorldWindRenderLayer.BOX_SELECTION_AT_START);
            Position positionAtEnd = wwd.getCurrentPosition();
            if (positionAtStart != null || positionAtEnd != null)
            {
                if (positionAtStart != null && positionAtEnd != null)
                {
                    if (!positionAtStart.equals(positionAtEnd))
                    {
                        wwd.callPositionListeners(new PositionEvent(drawable, sc.getPickPoint(), positionAtStart, positionAtEnd));
                    }
                } else
                {
                    wwd.callPositionListeners(new PositionEvent(drawable, sc.getPickPoint(), positionAtStart, positionAtEnd));
                }
            }

            PickedObject selectionAtEnd = wwd.getCurrentSelection();
            if (selectionAtStart != null || selectionAtEnd != null)
            {
                wwd.callSelectListeners(new SelectEvent(drawable, "gov.nasa.worldwind.SelectEvent.Rollover", sc.getPickPoint(), sc.getPickedObjectList()));
            }

            PickedObjectList boxSelectionAtEnd = wwd.getCurrentBoxSelection();
            if (boxSelectionAtStart != null || boxSelectionAtEnd != null)
            {
                wwd.callSelectListeners(new SelectEvent(drawable, "gov.nasa.worldwind.SelectEvent.BoxRollover", sc.getPickRectangle(), sc.getObjectsInPickRectangle()));
            }
        } catch (Exception exception)
        {
            logger.error("Exception while attempting to handle after-rendering events", exception);
        }
    }
}
