package gov.takx.core.view.gui.maps.terraindiscovery.controller.render;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AbstractSurfaceShape;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.GLUTessellatorSupport;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import gov.takx.api.annotation.InvokeOnEDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A WW Renderable SurfaceShape which takes an arbitrary number of Sectors and renders them as a single shape,
 * including groups of Sectors which do not intersect.  Uses the GLU winding rule GLU.GLU_TESS_WINDING_POSITIVE so
 * that intersecting/overlapping Sectors do not create unwanted holes in the shape.
 *
 */
public class SurfaceMultiSector extends AbstractSurfaceShape implements ITerrainRenderable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Sector boundingSector;
    private Set<Sector> sectorSet;
    private int windingRule;

    public SurfaceMultiSector(Sector boundingSector)
    {
        this(boundingSector, false);
    }

    public SurfaceMultiSector(Sector boundingSector, boolean inverted)
    {
        sectorSet = new HashSet<>();
        setBoundingSector(boundingSector);
        setInverted(inverted);

        ShapeAttributes shapeAttributes = new BasicShapeAttributes();
        shapeAttributes.setDrawOutline(false);
        setAttributes(shapeAttributes);
    }

    public void setInteriorVisible(boolean visible)
    {
        getAttributes().setDrawInterior(visible);
    }

    public void setOutlineVisible(boolean visible)
    {
        getAttributes().setDrawOutline(visible);
    }

    @InvokeOnEDT
    public void setBoundingSector(Sector boundingSector)
    {
        this.boundingSector = boundingSector;
    }

    @InvokeOnEDT
    public void addSector(Sector sector)
    {
        if (sector == null)
        {
            return;
        }

        sector = this.boundingSector.intersection(sector);

        // Small optimization - Sector implements equals(), so checking whether the set already contains an
        // equivalent sector reduces duplicate geometry from being created.
        if (!sectorSet.contains(sector))
        {
            sectorSet.add(sector);
        } else if (logger.isTraceEnabled())
        {
            logger.trace("Duplicate sector ignored: {}", sector);
        }

        geometryCache.clear();
    }

    public Iterable<Sector> getSectors()
    {
        return new HashSet<>(sectorSet);
    }

    public void setInverted(boolean inverted)
    {
        windingRule = inverted ? GLU.GLU_TESS_WINDING_NEGATIVE : GLU.GLU_TESS_WINDING_POSITIVE;

        // Force creation of new geometry by clearing the geometry cache.
        geometryCache.clear();
    }

    public void setSurfaceColor(Color surfaceColor)
    {
        getAttributes().setInteriorMaterial(new Material(surfaceColor));
        getAttributes().setOutlineMaterial(new Material(shiftHue(shiftBrightness(surfaceColor, 0.25d), 0.07d)));
    }

    public void setOpacity(double opacity)
    {
        getAttributes().setInteriorOpacity(opacity);
        getAttributes().setOutlineOpacity(Math.min(opacity + .1d, 1));
    }

    @Override
    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        return getSectorLocations(boundingSector);
    }

    @Override
    public Position getReferencePosition()
    {
        if (boundingSector == null)
        {
            return Position.ZERO;
        }
        return new Position(boundingSector.getCentroid(), 0);
    }

    @Override
    public void pick(DrawContext dc, Point pickPoint)
    {
        // This shape is not pickable.
    }

    @Override
    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        // This shape is not pickable/movable.
    }

    @Override
    protected void doMoveTo(Globe globe, Position oldReferencePosition, Position newReferencePosition)
    {
        // This shape is not pickable/movable.
    }

    @Override
    protected Integer doTessellateInterior(DrawContext dc)
    {
        final GLUtessellatorCallback cb = GLUTessellatorSupport.createOGLDrawPrimitivesCallback((GL2) dc.getGL());

        // Create a tessellator with the  winding rule: GLU_TESS_WINDING_POSITIVE. This is necessary in order to
        // prevent sector intersections from creating holes in the surface shape.
        final GLUTessellatorSupport glts = new GLUTessellatorSupport()
        {
            @Override
            public void beginTessellation(GLUtessellatorCallback callback, Vec4 normal)
            {
                super.beginTessellation(callback, normal);
                GLU.gluTessProperty(this.tess, GLU.GLU_TESS_WINDING_RULE, windingRule);
            }
        };

        glts.beginTessellation(cb, new Vec4(0, 0, 1));
        try
        {
            return this.tessellateInteriorVertices(glts.getGLUtessellator());
        } finally
        {
            // Free any heap memory used for tessellation immediately. If tessellation has consumed all available heap
            // memory, we must free memory used by tessellation immediately or subsequent operations such as message
            // logging will fail.
            glts.endTessellation();
        }
    }

    @Override
    protected List<List<LatLon>> createGeometry(Globe globe, double edgeIntervalsPerDegree)
    {
        Iterable<? extends LatLon> originalLocations = this.getLocations(globe);
        if (originalLocations == null)
        {
            return null;
        }

        final ArrayList<List<LatLon>> geom = new ArrayList<>();

        for (Sector s : sectorSet)
        {
            final List<LatLon> sectorLocations = getSectorLocations(s);

            final ArrayList<LatLon> drawLocations = new ArrayList<>();
            this.generateIntermediateLocations(sectorLocations, edgeIntervalsPerDegree, false, drawLocations);

            geom.add(drawLocations);
        }

        return geom;
    }

    @Override
    protected List<List<LatLon>> createGeometry(Globe globe, SurfaceTileDrawContext sdc)
    {
        ArrayList<List<LatLon>> geom = new ArrayList<>();
        if (windingRule == GLU.GLU_TESS_WINDING_NEGATIVE && boundingSector != null)
        {
            List<LatLon> mainSectorLocations = getSectorLocations(boundingSector);
            Collections.reverse(mainSectorLocations);
            geom.add(mainSectorLocations);
        }

        for (Sector s : sectorSet)
        {
            geom.add(getSectorLocations(s));
        }
        return geom;
    }

    private List<LatLon> getSectorLocations(Sector s)
    {
        if (s == null)
        {
            return Collections.emptyList();
        }

        LatLon[] locations = new LatLon[5];
        System.arraycopy(s.getCorners(), 0, locations, 0, 4);
        locations[4] = locations[0];
        return Arrays.asList(locations);
    }

    private static Color shiftHue(Color color, double shift)
    {
        if (shift <= 0 || shift >= 1)
        {
            return new Color(color.getRGB());
        }

        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float hue = hsb[0];
        hue += shift;
        if (hue > 1)
        {
            hue -= 1;
        }
        return new Color(Color.HSBtoRGB(hue, hsb[1], hsb[2]));
    }

    private static Color shiftBrightness(Color color, double shift)
    {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float brightness = hsb[2];
        brightness += shift;
        if (brightness > 1)
        {
            brightness = 1;
        } else if (brightness < 0)
        {
            brightness = 0;
        }
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], brightness));
    }
}
