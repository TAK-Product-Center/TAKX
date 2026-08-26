package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.OGLStackHandler;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import gov.nasa.worldwind.util.Tile;
import gov.tak.api.engine.math.IMatrix.MatrixOrder;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.layer.WorldWindSurfaceRenderLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;

/**
 * Render the WW surface pump.
 *
 * @since 3.4
 */
public class GLWorldWindSurfaceRenderLayer extends GLAbstractWorldWindLayer
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
            if (object.second instanceof WorldWindSurfaceRenderLayer)
            {
                return new GLWorldWindSurfaceRenderLayer(object.first, object.second);
            }
            return null;
        }
    };

    GLWorldWindSurfaceRenderLayer(MapRenderer surface, Layer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SURFACE);
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SURFACE))
        {
            sc.setValue(ApplicationSceneController.RENDER_PASS, renderPass);
            drawSurface(view, wwd);
        }
    }

    private void drawSurface(GLMapView view, TakWorldWindow wwd)
    {
        ApplicationSceneController sc = (ApplicationSceneController) wwd.getSceneController();
        DrawContext dc = sc.getDrawContext();
        if (dc.getGLContext() != GLContext.getCurrent()) return; // The first surface pass will happen before dc is set up in the first sprite pass

        double[] fwdProjection = new double[16];
        view.currentPass.scene.forward.get(fwdProjection, MatrixOrder.COLUMN_MAJOR);
        Matrix fwdMatrix = Matrix.fromArray(fwdProjection, 0, false);

        Sector sector = Sector.fromDegrees(view.currentPass.southBound, view.currentPass.northBound,
                view.currentPass.westBound, view.currentPass.eastBound);
        AVList params = new AVListImpl();
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(sector.getDeltaLat(), sector.getDeltaLon()));
        params.setValue(AVKey.SECTOR, sector);
        params.setValue(AVKey.NUM_LEVELS, 1);
        params.setValue(AVKey.TILE_WIDTH, view.currentPass.scene.width);
        params.setValue(AVKey.TILE_HEIGHT, view.currentPass.scene.height);
        params.setValue(AVKey.DATA_CACHE_NAME, "TakElevationModel");
        params.setValue(AVKey.DATASET_NAME, "TakElevationModel");
        params.setValue(AVKey.FORMAT_SUFFIX, ".bil");
        LevelSet levelSet = new LevelSet(params);
        Tile tile = new Tile(sector, levelSet.getFirstLevel());
        TakSurfaceTileDrawContext sdc = new TakSurfaceTileDrawContext(tile, Collections.emptyList(), fwdMatrix);
        dc.setValue(AVKey.SURFACE_TILE_DRAW_CONTEXT, sdc);

        GL2 gl = dc.getGL().getGL2();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushProjection(gl);
        ogsh.pushModelview(gl);
        ogsh.pushAttrib(gl, GL2.GL_POLYGON_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        try
        {
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrtho(0, view.currentPass.scene.width, 0, view.currentPass.scene.height, -1, 1);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glFrontFace(GL.GL_CCW);
            gl.glDisable(GL.GL_DEPTH_TEST);

            sc.repaintSurfacePass(dc);
        } finally
        {
            ogsh.pop(gl);
        }
    }

    private static class TakSurfaceTileDrawContext extends SurfaceTileDrawContext
    {
        public TakSurfaceTileDrawContext(Tile tile, Collection<PickedObject> pickCandidates, Matrix modelview)
        {
            super(tile, pickCandidates);
            this.modelview = modelview;
        }
    }
}
