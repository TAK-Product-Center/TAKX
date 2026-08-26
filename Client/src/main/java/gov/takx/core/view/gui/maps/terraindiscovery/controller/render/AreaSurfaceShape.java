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
import gov.nasa.worldwind.util.GLUTessellatorSupport;
import gov.nasa.worldwind.util.SurfaceTileDrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */
public class AreaSurfaceShape extends AbstractSurfaceShape implements ITerrainRenderable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Area area;
    private Sector boundingSector;

    public AreaSurfaceShape()
    {
        this(Sector.FULL_SPHERE);
    }

    public AreaSurfaceShape(Sector boundingSector)
    {
        area = new Area();
        setBoundingSector(boundingSector);
        this.setAttributes(new BasicShapeAttributes());
        getAttributes().setDrawOutline(false);
        getAttributes().setDrawInterior(true);
        setOpacity(0.8d);
        setColor(Color.BLUE);
    }

    public void setBoundingSector(Sector boundingSector)
    {
        if (boundingSector != null)
        {
            this.boundingSector = Sector.boundingSector(boundingSector);
        }
    }

    public void setColor(Color color)
    {
        getAttributes().setInteriorMaterial(new Material(color));
    }

    public void setOpacity(double opacity)
    {
        getAttributes().setInteriorOpacity(opacity);
    }

    public void add(double lat1, double lon1, double lat2, double lon2)
    {
        add(Sector.boundingSector(toLatLon(lat1, lon1), toLatLon(lat2, lon2)));
    }

    public void add(Sector sector)
    {
        addArea(sectorToArea(sector));
    }

    public void add(Shape shape)
    {
        if (shape != null)
        {
            addArea(new Area(shape));
        }
    }

    public void add(AreaSurfaceShape areaSurfaceShape)
    {
        if (areaSurfaceShape != null)
        {
            addArea(areaSurfaceShape.getArea());
        }
    }

    public void addArea(Area otherArea)
    {
        if (otherArea != null)
        {
            this.area.add(otherArea);
        }
    }

    public void remove(double lat1, double lon1, double lat2, double lon2)
    {
        remove(Sector.boundingSector(toLatLon(lat1, lon1), toLatLon(lat2, lon2)));
    }

    public void remove(Shape shape)
    {
        if (shape != null)
        {
            removeArea(new Area(shape));
        }
    }

    public void remove(Sector sector)
    {
        removeArea(sectorToArea(sector));
    }

    public void remove(AreaSurfaceShape areaSurfaceShape)
    {
        if (areaSurfaceShape != null)
        {
            removeArea(areaSurfaceShape.area);
        }
    }

    public void removeArea(Area otherArea)
    {
        if (otherArea != null)
        {
            this.area.subtract(otherArea);
        }
    }

    public boolean isEmpty()
    {
        return area.isEmpty();
    }

    public Area getArea()
    {
        return new Area(area);
    }

    public void clear()
    {
        area.reset();
    }

    @Override
    public Position getReferencePosition()
    {
        return new Position(boundingSector.getCentroid(), 0);
    }

    @Override
    public void pick(DrawContext dc, Point pickPoint)
    {
        // This shape is not pickable.
    }

    @Override
    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (boundingSector == null)
        {
            return Collections.emptyList();
        }

        LatLon[] locations = new LatLon[5];
        System.arraycopy(boundingSector.getCorners(), 0, locations, 0, 4);
        locations[4] = locations[0];
        return Arrays.asList(locations);
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
                GLU.gluTessProperty(this.tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_POSITIVE);
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
        final List<List<LatLon>> baseGeometry = createGeometry(null, null);
        if (baseGeometry == null || baseGeometry.isEmpty()) return baseGeometry;

        final List<List<LatLon>> geom = new ArrayList<>(baseGeometry.size());

        for (List<LatLon> latLons : baseGeometry)
        {
            final ArrayList<LatLon> drawLocations = new ArrayList<>();
            this.generateIntermediateLocations(latLons, edgeIntervalsPerDegree, false, drawLocations);

            geom.add(drawLocations);
        }

        return geom;
    }

    @Override
    protected List<List<LatLon>> createGeometry(Globe globe, SurfaceTileDrawContext sdc)
    {
        List<List<LatLon>> geom = new ArrayList<>();
        if (area.isEmpty())
        {
            return geom;
        }

        PathIterator pathIterator = area.getPathIterator(null);

        double[] segment = new double[6];
        double[] start = new double[segment.length];

        List<LatLon> currentPath = new ArrayList<>();

        while (!pathIterator.isDone())
        {
            int type = pathIterator.currentSegment(segment);
            switch (type)
            {
                case PathIterator.SEG_MOVETO:
                    System.arraycopy(segment, 0, start, 0, segment.length);
                    break;
                case PathIterator.SEG_LINETO:
                    currentPath.add(toLatLon(segment));
                    break;
                case PathIterator.SEG_CLOSE:
                    currentPath.add(toLatLon(start));
                    geom.add(currentPath);
                    currentPath = new ArrayList<>();
                    break;
                default:
                    break;
            }
            pathIterator.next();
        }

        // todo: do I need this?
        if (!currentPath.isEmpty())
        {
            currentPath.add(toLatLon(start));
            geom.add(currentPath);

            if (logger.isDebugEnabled())
            {
                logger.debug("Adding non-empty path: {}", currentPath);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("New geometry: {}", geom);
        }
        return geom;
    }

    private Area sectorToArea(Sector sector)
    {
        Rectangle2D rect = sectorToRectangle(sector);
        if (rect != null)
        {
            return new Area(rect);
        }
        return null;
    }

    private Rectangle2D sectorToRectangle(Sector sector)
    {
        sector = boundingSector.intersection(sector);
        if (sector != null)
        {
            return new Rectangle2D.Double(
                    sector.getMinLatitude().degrees,
                    sector.getMinLongitude().degrees,
                    sector.getDeltaLatDegrees(),
                    sector.getDeltaLonDegrees());
        }
        return null;
    }

    private LatLon toLatLon(double[] segment)
    {
        return toLatLon(segment[0], segment[1]);
    }

    private LatLon toLatLon(double lat, double lon)
    {
        return LatLon.fromDegrees(lat, lon);
    }
}
