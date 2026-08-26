package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.layer.pick.PickContext;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.PickedObjectManager;
import gov.takx.core.globe.tak.layer.PickingLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;

/**
 * A layer that runs the picking operation.
 *
 * @since 5.2
 */
public class GLPickingLayer extends GLAbstractWorldWindLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Service provider for {@link GLPickingLayer}.
     *
     * @since 5.2
     */
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
            if (object.second instanceof PickingLayer pickingLayer)
            {
                return new GLPickingLayer(object.first, pickingLayer);
            }
            return null;
        }
    };

    private final PickingLayer pickingLayer;
    private final PickedObjectManager pickedObjectManager;

    private GLPickingLayer(MapRenderer surface, PickingLayer pickingLayer)
    {
        super(surface, pickingLayer, GLMapView.RENDER_PASS_SPRITES);
        this.pickingLayer = pickingLayer;
        this.pickedObjectManager = pickingLayer.getPickedObjectManager();
    }

    /**
     * Creates the {@link PickContext} and sends to {@link gov.takx.core.globe.tak.PickedObjectManager} to run
     * the picking logic.
     *
     * @param view the mapview object
     * @param wwd the worldwind window
     * @param sc the scene controller
     * @param renderPass the bit mask that defines the render pass
     */
    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            DrawContext dc = sc.getDrawContext();
            Point pickPoint = dc.getPickPoint();
            Rectangle pickRectangle = dc.getPickRectangle();
            if (pickRectangle != null)
            {
                pickPoint = pickRectangle.getLocation();
            } else if (pickPoint != null)
            {
                pickRectangle = new Rectangle(pickPoint.x, pickPoint.y, 1, 1);
            } else
            {
                return;
            }

            // right now we have to grab terrain pick from the draw context and get position from there
            gov.nasa.worldwind.pick.PickedObject terrainPick = dc.getPickedObjects().getTerrainObject();
            Position pickedPos = terrainPick != null ? terrainPick.getPosition() : dc.getView().computePositionFromScreenPoint(pickPoint.x, pickPoint.y);
            GeoPoint pickLocation = pickedPos != null ? new GeoPoint(pickedPos.getLatitude().getDegrees(), pickedPos.getLongitude().getDegrees(), pickedPos.elevation) : null;

            // Create context and pass to PickedObjectManager
            PickContext context = new PickContext(globe, pickRectangle, pickLocation);
            pickedObjectManager.pick(context);
        }
    }
}
