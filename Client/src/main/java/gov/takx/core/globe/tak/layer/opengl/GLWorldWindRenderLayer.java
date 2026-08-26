package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.layer.WorldWindSpriteRenderLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Render the WW pump.
 *
 * @since 3.4
 */
public class GLWorldWindRenderLayer extends GLAbstractWorldWindLayer
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
            if (object.second instanceof WorldWindSpriteRenderLayer)
            {
                return new GLWorldWindRenderLayer(object.first, object.second);
            }
            return null;
        }
    };

    /**
     * The {@link Position} at the start of WW rendering.
     * @since 3.5
     */
    static final String POSITION_AT_START = "gov.takx.core.globe.tak.layer.opengl.PositionAtStart";

    /**
     * The {@link PickedObject} selection at the start of WW rendering.
     * @since 3.5
     */
    static final String SELECTION_AT_START = "gov.takx.core.globe.tak.layer.opengl.SelectionAtStart";

    /**
     * The {@link PickedObjectList} box selection at the start of WW rendering.
     * @since 3.5
     */
    static final String BOX_SELECTION_AT_START = "gov.takx.core.globe.tak.layer.opengl.BoxSelectionAtStart";

    GLWorldWindRenderLayer(MapRenderer surface, Layer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            sc.setValue(ApplicationSceneController.RENDER_PASS, renderPass);
            drawSprites(view, wwd);
        }
    }

    private void drawSprites(GLMapView view, TakWorldWindow wwd)
    {
        try
        {
            SceneController sc = wwd.getSceneController();

            if (sc == null)
            {
                String msg = "Scene controller is null at repaint";
                logger.error(msg);
                throw new IllegalStateException(msg);
            }

            Position positionAtStart = wwd.getCurrentPosition();
            PickedObject selectionAtStart = wwd.getCurrentSelection();
            PickedObjectList boxSelectionAtStart = wwd.getCurrentBoxSelection();

            sc.setValue(POSITION_AT_START, positionAtStart);
            sc.setValue(SELECTION_AT_START, selectionAtStart);
            sc.setValue(BOX_SELECTION_AT_START, boxSelectionAtStart);

            sc.repaint();

        } catch (Exception exception)
        {
            logger.error("Exception while attempting to repaint WorldWindow", exception);
        }
    }
}
