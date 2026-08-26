package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.Polygon;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.CompoundVecBuffer;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.PersistableExtrudedPolygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Concrete implementation of {@link ISurfaceStructure}.  This implementation uses the WW {@link ExtrudedPolygon} as
 * the WW representation of the structure.
 * <p/>
 * Renderable extruded polygon implementation. For most of our functionality, we just delegate to the underlying extruded polygon.
 *
 */
public class SSExtrudedPolygon extends AExtrudedPolygonalHolder<SSExtrudedPolygon.ExtrudedPolygonRenderable, PersistableExtrudedPolygon>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final int GEOMETRY_REGEN_INTERVAL = 50_000;
    public static final int DEFAULT_BASE_DEPTH = 1_000;

    private double minHeight;
    private double maxHeight;

    /**
     * Factory method to build from a given persistable.
     *
     * @param persistable Persistable entity holding the state from which to build
     * @since 3.0
     */
    static ISurfaceStructure createFromPersistable(IPersistableSurfaceStructure persistable)
    {
        return new SSExtrudedPolygon((PersistableExtrudedPolygon) persistable);
    }

    /**
     * Factory to create an SSPolygon from a KML polygon.
     *
     * @param kmlPolygon kml polygon geometry
     * @since 3.0
     */
    public static SSExtrudedPolygon fromKml(Polygon kmlPolygon)
    {
        final List<List<Position>> blockLocations = kmlToBlockLocations(kmlPolygon, true);

        final Sector sector = Sector.boundingSector(blockLocations.get(0)); // 0 is outer boundary
        final PersistableExtrudedPolygon persistable = new PersistableExtrudedPolygon(sector, blockLocations, null);

        persistable.setIsExtrude(kmlPolygon.isExtrude());
        persistable.setAltitudeMode(getAltitudeModeFromKml(kmlPolygon.getAltitudeMode(), WorldWind.CONSTANT));

        return new SSExtrudedPolygon(persistable);
    }

    /**
     * Construct from a CompoundVecBuffer.
     *
     * @param sector            bounding sector of all points
     * @param compoundVecBuffer buffer containing all vertex information
     * @param height            extrude height
     */
    public SSExtrudedPolygon(Sector sector, CompoundVecBuffer compoundVecBuffer, double height)
    {
        this(new PersistableExtrudedPolygon(sector, vecBufferToBlockLocations(compoundVecBuffer, ll -> new Position(ll, height)), height));
    }

    /**
     * Construct from block locations.
     *
     * @param sector         bounding sector of all points
     * @param blockLocations Block locations of the polygon's boundaries
     * @param height         extrude height
     */
    public SSExtrudedPolygon(Sector sector, List<List<Position>> blockLocations, double height)
    {
        this(new PersistableExtrudedPolygon(sector, blockLocations, height));
    }

    /**
     * Construct from persistable state.
     *
     * @param persistable Persistable holding our configuration
     * @since 3.0
     */
    public SSExtrudedPolygon(PersistableExtrudedPolygon persistable)
    {
        super(persistable.getSector(), persistable);

        calculateHeightRange();
    }

    public String getHeightString()
    {
        // PLUGIN-2661
        //
        // If there is a displayable difference between heights (display format supports 2 digits of precision), then
        // display height as a range.
        //
        // @since 1.43
        return Math.abs(maxHeight - minHeight) >= 0.01
                ? String.format("%.2f m - %.2f m", minHeight, maxHeight)
                : String.format("%.2f m", minHeight);
    }

    /**
     * Calculate the min/max heights in the polygon boundaries.
     *
     * @since 3.0
     */
    private void calculateHeightRange()
    {
        Double min = null;
        Double max = null;

        for (List<? extends LatLon> boundary : persistable.getBlockLocations())
        {
            for (LatLon ll : boundary)
            {
                if (ll instanceof Position)
                {
                    final double height = ((Position) ll).getElevation();

                    min = min == null ? height : Math.min(min, height);
                    max = max == null ? height : Math.max(max, height);
                }
            }
        }

        if (min == null)
        {
            minHeight = maxHeight = persistable.getExtrudeHeight();
        } else
        {
            minHeight = min;
            maxHeight = max;
        }
    }

    /**
     * Expose the getBoundaries method to public to enable drawing this polygon on a 2D graphics context with outer
     * and inner boundaries.
     *
     * @return the list of boundaries for this extruded polygon
     */
    public List<List<? extends LatLon>> getBoundaries()
    {
        return renderable.getBoundaries();
    }

    @Override
    protected ExtrudedPolygonRenderable makeRenderable()
    {
        final List<List<Position>> blockLocations = persistable.getBlockLocations();
        final Double height = persistable.getExtrudeHeight();
        final int numberOfBlocks = blockLocations.size();

        // Construct our renderable
        final ExtrudedPolygonRenderable renderable = new ExtrudedPolygonRenderable();

        if (numberOfBlocks > 0)
        {
            renderable.setOuterBoundary(blockLocations.get(0), height); // The first block is our outer boundary

            // Remaining blocks are inner boundaries
            for (int i = 1; i < numberOfBlocks; i++)
            {
                renderable.addInnerBoundary(blockLocations.get(i));
            }
        }

        renderable.setAltitudeMode(persistable.getAltitudeMode());
        renderable.setEnableSides(persistable.isExtrude());

        if (height != null) renderable.setHeight(height);

        return renderable;
    }

    @Override
    protected void applyAttributesToRenderable(ShapeAttributes attributes)
    {
        renderable.setSideAttributes(attributes);
        renderable.setCapAttributes(attributes);
    }

    @Override
    public String toString()
    {
        return persistable.toString();
    }

    /**
     * Our renderable.
     *
     * @since 3.0
     */
    public class ExtrudedPolygonRenderable extends ExtrudedPolygon implements ISurfaceStructureRenderable
    {
        private ExtrudedPolygonRenderable()
        {
            setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            setGeometryRegenerationInterval(GEOMETRY_REGEN_INTERVAL);
            setBaseDepth(DEFAULT_BASE_DEPTH);
        }

        @Override
        public ISurfaceStructure getSurfaceStructure()
        {
            return SSExtrudedPolygon.this;
        }

        /**
         * Overridden to make access public.
         *
         * @return the list of boundaries for this extruded polygon
         */
        @Override
        public List<List<? extends LatLon>> getBoundaries()
        {
            return super.getBoundaries();
        }

        /**
         * Overriding render with modified version found in AbstractShape.  The only difference is that the rendered shape
         * size is not tested, in order to give the render cycle a slight speedup.  This assumption can be made if a max
         * viewable altitude is used on the shapes layer, where virtually all of the extruded polygons will be much
         * larger than one pixel.
         *
         * @param dc draw context for the current globe view
         */
        @Override
        public void render(DrawContext dc)
        {
            if (!isOuterBoundaryValid())
            {
                return;
            }

            // This render method is called three times during frame generation. It's first called as a Renderable
            // during Renderable picking. It's called again during normal rendering. And it's called a third
            // time as an OrderedRenderable. The first two calls determine whether to add the shape to the ordered renderable
            // list during pick and render. The third call just draws the ordered renderable.

            if (dc == null)
            {
                String msg = "DrawContext is null";
                logger.error("{}", msg);
                throw new IllegalArgumentException(msg);
            }

            // Retrieve the cached data for the current globe. If it doesn't yet exist, create it. Most code subsequently
            // executed depends on currentData being non-null.
            currentData = (AbstractShapeData) shapeDataCache.getEntry(dc.getGlobe());
            if (currentData == null)
            {
                currentData = createCacheEntry(dc);
                shapeDataCache.addEntry(currentData);
            }

            if (dc.getSurfaceGeometry() == null) return;

            if (!isVisible()) return;

            if (isTerrainDependent())
            {
                checkViewDistanceExpiration(dc);
            }

            if (getExtent() != null)
            {
                if (!intersectsFrustum(dc)) return;
            }

            if (dc.isOrderedRenderingMode())
            {
                drawOrderedRenderable(dc);
            } else
            {
                makeOrderedRenderable(dc);
            }
        }
    }
}
