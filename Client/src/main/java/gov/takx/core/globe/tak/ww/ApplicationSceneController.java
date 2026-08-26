package gov.takx.core.globe.tak.ww;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.AbstractSceneController;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.DrawContextImpl;
import gov.nasa.worldwind.render.GeographicSurfaceTileRenderer;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.render.SurfaceTile;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.IMapRendererEnums.InverseMode;
import gov.tak.api.engine.map.IMapRendererEnums.InverseResult;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.tak.GlobeAnnotationMapObjectManager;
import gov.takx.core.globe.tak.GlobeMapObjectFeatureManager;
import gov.takx.core.globe.tak.OffscreenFrameBufferHelper;
import gov.takx.core.globe.tak.PickUtils;
import gov.takx.core.globe.tak.PickedObjectManager;
import gov.takx.core.globe.tak.RenderableAnnotationManager;
import gov.takx.core.globe.tak.RenderableFeatureManager;
import gov.takx.core.globe.tak.RenderableManager;
import gov.takx.core.globe.tak.ScreenAnnotationManager;
import gov.takx.core.globe.tak.SurfaceImageManager;
import gov.takx.core.globe.tak.WidgetLayerManager;
import jakarta.inject.Inject;
import jogamp.nativewindow.SurfaceScaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/**
 * Implementation of {@link SceneController} for TAK globe. This class must differentiate between sprite and surface
 * passes and behave accordingly.
 * <p/>
 * The sprite pass is for rendering 3D objects directly to the screen. The sprite pass renders once for every call to
 * OpenGL's display(). The SceneController will intercept rendering of WW surface objects during the <i>sprite</i> pass
 * and defer them to the <i>surface</i> pass. WW's {@link gov.nasa.worldwind.View} will reflect the state of the scene
 * during the sprite pass.
 * <p/>
 * The surface pass is for rendering to a 2D tile that will be draped over the terrain. The surface pass may run
 * multiple times during OpenGL's display() call. The tiles for the tile set might all render in one call to display(),
 * or that might be spread out over multiple calls to display() depending on how long it takes to render the tiles. When
 * the surface render pump changes, we take ownership of all deferred surface objects gathered during the <i>sprite</i>
 * pass. Those objects are then rendered to each tile over the course of all surface passes for the current render pump.
 * <p/>
 * Care must be taken when calling to any TAK rendering during the {@link #doRepaint(DrawContext)} call. If TAK changes the current program,
 * it must clear it before WW's fixed function pipeline calls will work.
 *
 * @since 3.4
 */
@PseudoBean
public class ApplicationSceneController extends AbstractSceneController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The {@link gov.takx.api.globe.IApplicationGlobe} associated with this scene controller.
     * @since 3.5
     */
    public static final String GLOBE_DELEGATE = "gov.takx.core.globe.tak.GLOBE_DELEGATE";

    /**
     * The current TAK kernel render pass.
     * @since 3.5
     */
    public static final String RENDER_PASS = "gov.takx.core.globe.tak.RENDER_PASS";

    @Inject
    RenderableFeatureManager renderableFeatureManager;

    @Inject
    ScreenAnnotationManager screenAnnotationManager;

    @Inject
    RenderableAnnotationManager renderableAnnotationManager;

    @Inject
    SurfaceImageManager surfaceImageManager;

    @Inject
    PickedObjectManager pickedObjectManager;
    @Inject
    GlobeMapObjectFeatureManager globeMapObjectFeatureManager;
    @Inject
    WidgetLayerManager widgetLayerManager;
    @Inject
    GlobeAnnotationMapObjectManager globeAnnotationMapObjectManager;
    @Inject
    RenderableManager renderableManager;

    private enum Pass
    {
        SURFACE, SPRITE
    }

    private Pass pass;

    private GLMapView glMapView;
    private final ArrayList<OrderedRenderable> surfaceRenderables = new ArrayList<>();
    private final PickedObjectList surfacePickedObjects = new PickedObjectList();
    private final PickedObjectList surfaceObjectsInPickRect = new PickedObjectList();
    private int pickRenderPump;
    private Point lastPickPoint;
    private final OffscreenFrameBufferHelper ofb = new OffscreenFrameBufferHelper();
    boolean stopDrawing = false;

    public ApplicationSceneController()
    {
        dc = new TakDrawContext();
    }

    /**
     * This will instruct the controller to cease drawing of the scene. This facilitates executing a final shutdown task
     * on the GL thread without triggering rendering.
     */
    public void stopAllDrawing()
    {
        stopDrawing = true;
    }

    /**
     * Call {@link #doRepaint(DrawContext)} to just repaint the surface pass without reinitializing the drawing context.
     * @param dc the draw context
     * @since 3.5
     */
    public void repaintSurfacePass(DrawContext dc)
    {
        doRepaint(dc);
    }

    @Override
    protected void doRepaint(DrawContext dc)
    {
        if (stopDrawing) return;

        IApplicationGlobe applicationGlobe = (IApplicationGlobe) dc.getValue(GLOBE_DELEGATE);
        if (applicationGlobe == null) return;
        glMapView = applicationGlobe.getGLMapView();

        int renderPass = AVListImpl.getIntegerValue(this, RENDER_PASS, GLMapView.RENDER_PASS_SPRITES);
        pass = MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SURFACE) ? Pass.SURFACE : Pass.SPRITE;

        // WorldWind expects the default color to be set
        dc.restoreDefaultCurrentColor();

        if (pass == Pass.SPRITE)
        {
            applyView(dc);
            createPickFrustum(dc);
            createTerrain(dc);
        }

        ofb.useOffscreenBuffer(() -> {
            preRender(dc);
            clearFrame(dc);
            pick(dc);
        }, dc.getDrawableWidth(), dc.getDrawableHeight());

        draw(dc);
    }

    @Override
    public void dispose()
    {
        ofb.release();
        if (getGpuResourceCache() != null) getGpuResourceCache().clear();
        super.dispose();
    }

    @Override
    public void reinitialize()
    {
        ofb.release();
        if (getGpuResourceCache() != null) getGpuResourceCache().clear();
        super.reinitialize();
    }

    @Override
    protected void preRender(DrawContext dc)
    {
        if (pass == Pass.SURFACE)
        {
            dc.setPreRenderMode(true);
            preRenderOrderedSurfaceRenderables(dc);
            dc.setPreRenderMode(false);
            return;
        }
        super.preRender(dc);
    }

    @Override
    protected void preRenderOrderedSurfaceRenderables(DrawContext dc)
    {
        if (pass == Pass.SPRITE)
        {
            // Store surface renderables for pre-/rendering during the Surface pass
            surfaceRenderables.clear();
            final var orderedSurfaceRenderables = dc.getOrderedSurfaceRenderables();
            surfaceRenderables.addAll(orderedSurfaceRenderables);
            orderedSurfaceRenderables.clear();
            return;
        }

        if (!surfaceRenderables.isEmpty())
        {
            dc.setOrderedRenderingMode(true);

            for (OrderedRenderable renderable : surfaceRenderables)
            {
                try
                {
                    if (renderable instanceof PreRenderable preRenderable) preRenderable.preRender(dc);
                } catch (Exception e)
                {
                    logger.warn("Exception encountered while pre-rendering renderable {}.", renderable, e);
                }
            }

            dc.setOrderedRenderingMode(false);
        }
    }

    @Override
    protected void pick(DrawContext dc)
    {
        if (pass == Pass.SURFACE)
        {
            // If we're rendering a new set of surface tiles, then clear our pick lists
            if (pickRenderPump != glMapView.currentPass.renderPump || dc.getPickPoint() != lastPickPoint)
            {
                surfacePickedObjects.clear();
                surfaceObjectsInPickRect.clear();
                pickRenderPump = glMapView.currentPass.renderPump;
                lastPickPoint = dc.getPickPoint();
            }
            // Clear picked objects
            dc.getPickedObjects().clear();
            dc.enablePickingMode();
            // Pick surface renderables to current surface tile
            pickOrderedSurfaceRenderables(dc);
            dc.disablePickingMode();
            // Capture results
            surfacePickedObjects.addAll(dc.getPickedObjects());
            surfaceObjectsInPickRect.addAll(dc.getObjectsInPickRectangle());
            return;
        }

        if (dc.getPickPoint() != lastPickPoint)
        {
            SurfaceRendererControl control = glMapView.getControl(SurfaceRendererControl.class);
            if (control != null) control.markDirty();
            lastPickPoint = dc.getPickPoint();
        }
        super.pick(dc);
    }

    @Override
    protected void pickOrderedSurfaceRenderables(DrawContext dc)
    {
        if (pass == Pass.SPRITE) return;

        // This method replicates the behavior of AbstractSurfaceObject.pick()
        dc.setOrderedRenderingMode(true);
        PickSupport pickSupport = new PickSupport();
        GL2 gl = dc.getGL().getGL2();
        Color pickColor = PickUtils.PICK_COLOR;
        for (OrderedRenderable renderable : surfaceRenderables)
        {
            if (PickUtils.isNotPickable(renderable)) continue;
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            pickSupport.clearPickList();
            pickSupport.beginPicking(dc);
            try
            {
                pickSupport.addPickableObject(PickUtils.createPickedObject(dc, renderable, pickColor));
                gl.glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());
                renderable.render(dc);
            } finally
            {
                Point point = PickUtils.getTranslatedPoint(dc, glMapView, getPickPoint());
                pickSupport.endPicking(dc);
                pickSupport.resolvePick(dc, point, null);
            }
        }

        // Call upon SurfaceImageManager to pick the SurfaceImages/GLTileMesh.
        surfaceImageManager.pick(dc, glMapView, getPickPoint());

        dc.setOrderedRenderingMode(false);
    }

    @Override
    protected void pickTerrain(DrawContext dc)
    {
        // Replace call to super and WW terrain picking with pick of current lla
        Point pickPoint = dc.getPickPoint();
        if (pickPoint != null)
        {
            GeoPoint geoPoint = GeoPoint.createMutable();
            InverseResult inverse = glMapView.inverse(new PointD(pickPoint.x, pickPoint.y), geoPoint, InverseMode.RayCast, 0, DisplayOrigin.UpperLeft);
            if (inverse != InverseResult.None)
            {
                Position position = Position.fromDegrees(geoPoint.getLatitude(), geoPoint.getLongitude(), geoPoint.getAltitude());
                dc.addPickedObject(new PickedObject(dc.getUniquePickColor().getRGB(), position, position, true));
            }
        }
    }

    @Override
    protected void doNonTerrainPick(DrawContext dc)
    {
        // Add surface picked objects.
        dc.addPickedObjects(surfacePickedObjects);
        surfaceObjectsInPickRect.forEach(dc::addObjectInPickRectangle);
        // Pick WW objects
        super.doNonTerrainPick(dc);

        // Push TAKX picks back into draw context until we extract selection logic
        for (gov.tak.core.map.layer.PickedObject pickedObject : pickedObjectManager.getPickedObjectList())
        {
            gov.nasa.worldwind.pick.PickedObject wwObject = PickUtils.createPickedObject(dc, pickedObject.userObject(), Color.BLACK);
            dc.addPickedObject(wwObject);
        }

        // Pick screen annotations e.g. terrain contour lines TODO TAKX-2695 Migrate this picking logic once terrain contour lines are re-enabled
        screenAnnotationManager.pick(dc, glMapView, getPickPoint());
    }

    @Override
    protected void resolveTopPick(DrawContext dc)
    {
        super.resolveTopPick(dc);
        if (pass == Pass.SPRITE)
        {
            // Because the surface picks happen out of band over multiple passes, we can get multiple objects that are isOnTop.
            // To try to restore some order to the list of objects, let's sort them so terrain objects get put at the end.
            dc.getPickedObjects().sort((o1, o2) -> o1.isTerrain() == o2.isTerrain() ? 0 : o1.isTerrain() ? 1 : -1);
        }
    }

    @Override
    protected void draw(DrawContext dc)
    {
        if (pass == Pass.SURFACE)
        {
            drawOrderedSurfaceRenderables(dc);
            return;
        }
        super.draw(dc);
    }

    @Override
    protected void drawOrderedSurfaceRenderables(DrawContext dc)
    {
        if (pass == Pass.SPRITE) return;

        dc.setOrderedRenderingMode(true);

        for (OrderedRenderable renderable : surfaceRenderables)
        {
            try
            {
                renderable.render(dc);
            } catch (Exception e)
            {
                logger.warn("Exception encountered while repainting renderable {}.", renderable, e);
            }
        }

        var globe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
        if (globe != null) surfaceImageManager.render(glMapView, globe);

        dc.setOrderedRenderingMode(false);
    }

    private class TakDrawContext extends DrawContextImpl
    {
        public TakDrawContext()
        {
            geographicSurfaceTileRenderer = new GeographicSurfaceTileRenderer()
            {
                // Anything that makes it here is intended to be rendered to WW surface tiles which we do not render when
                // using TAK engine. This is the last opportunity for us to pull out any objects for rendering to TAK
                // surface tiles. Don't call the super, since that will end up doing rendering work that we don't display.
                @Override
                public void renderTiles(DrawContext dc, Iterable<? extends SurfaceTile> tiles)
                {
                    tiles.forEach(t -> {
                        if (t instanceof SurfaceImage surfaceImage)
                        {
                            IApplicationGlobe globe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
                            if (globe == null) return;
                            surfaceImage.setValue(AVKey.LAYER, dc.getCurrentLayer());
                            surfaceImageManager.offerRenderable(surfaceImage, globe);
                        }
                    });
                }
            };
        }

        @Override
        public void addOrderedRenderable(OrderedRenderable orderedRenderable)
        {
            if (renderableManager.offerRenderable(orderedRenderable, this)) return;
            super.addOrderedRenderable(orderedRenderable);
        }

        @Override
        public void addOrderedRenderable(OrderedRenderable orderedRenderable, boolean isBehind)
        {
            if (renderableManager.offerRenderable(orderedRenderable, this)) return;
            super.addOrderedRenderable(orderedRenderable, isBehind);
        }

        @Override
        public void addOrderedSurfaceRenderable(OrderedRenderable orderedRenderable)
        {
            if (renderableManager.offerRenderable(orderedRenderable, this)) return;
            super.addOrderedSurfaceRenderable(orderedRenderable);
        }

        @Override
        public int[] awtPointToGLpoint(Point pt)
        {
            if (glMapView != null && glMapView.getRenderSurface() instanceof ScalableSurface scalableSurface)
            {
                if (pt == null) return null;
                int[] awtPt = {pt.x, pt.y};
                return SurfaceScaleUtils.scale(awtPt, awtPt, scalableSurface.getCurrentSurfaceScale(new float[2]));
            }
            return super.awtPointToGLpoint(pt);
        }
    }
}
