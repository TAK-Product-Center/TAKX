package gov.takx.core.structures;

import de.micromata.opengis.kml.v_2_2_0.Polygon;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.util.CompoundVecBuffer;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.PersistablePolygon;

import java.util.List;
import java.util.function.Function;

/**
 * Concrete implementation of {@link ISurfaceStructure}.  This implementation uses the WW {@link SurfacePolygon} as
 * the WW representation of the structure.
 *
 */
public class SSPolygon extends ASurfaceStructureHolder<SurfacePolygon, PersistablePolygon>
{
    /**
     * Factory method to build from a given persistable.
     *
     * @param persistable Persistable entity holding the state from which to build
     * @since 3.0
     */
    static ISurfaceStructure createFromPersistable(IPersistableSurfaceStructure persistable)
    {
        return new SSPolygon((PersistablePolygon) persistable);
    }

    /**
     * Factory to create an SSPolygon from a KML polygon.
     *
     * @param kmlPolygon kml polygon geometry
     * @since 3.0
     */
    public static SSPolygon fromKml(Polygon kmlPolygon)
    {
        final List<List<LatLon>> blockLocations = kmlToBlockLocations(kmlPolygon, false);

        final Sector sector = Sector.boundingSector(blockLocations.get(0)); // 0 is outer boundary
        final PersistablePolygon persistable = new PersistablePolygon(sector, blockLocations);

        return new SSPolygon(persistable);
    }

    /**
     * Construct from a CompoundVecBuffer.
     *
     * @param sector            bounding sector of all points
     * @param compoundVecBuffer buffer containing all vertex information
     */
    public SSPolygon(Sector sector, CompoundVecBuffer compoundVecBuffer)
    {
        this(new PersistablePolygon(sector, vecBufferToBlockLocations(compoundVecBuffer, Function.identity())));
    }

    /**
     * Construct from persistable state.
     *
     * @param persistable Persistable holding our configuration
     * @since 3.0
     */
    private SSPolygon(PersistablePolygon persistable)
    {
        super(persistable.getSector(), persistable);
    }

    @Override
    protected SurfacePolygon makeRenderable()
    {
        final List<List<LatLon>> blockLocations = persistable.getBlockLocations();
        final int numberOfBlocks = blockLocations.size();

        final PolygonRenderable renderable = new PolygonRenderable();

        if (numberOfBlocks > 0)
        {
            renderable.setOuterBoundary(blockLocations.get(0)); // The first block is our outer boundary

            // Remaining blocks are inner boundaries
            for (int i = 1; i < numberOfBlocks; i++)
            {
                renderable.addInnerBoundary(blockLocations.get(i));
            }
        }

        return renderable;
    }

    @Override
    protected void applyAttributesToRenderable(ShapeAttributes attributes)
    {
        renderable.setAttributes(attributes);
    }

    /**
     * Our renderable.
     *
     * @since 3.0
     */
    private class PolygonRenderable extends SurfacePolygon implements ISurfaceStructureRenderable
    {
        @Override
        public ISurfaceStructure getSurfaceStructure()
        {
            return SSPolygon.this;
        }
    }
}
