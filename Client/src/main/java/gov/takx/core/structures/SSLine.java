package gov.takx.core.structures;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolylines;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.CompoundVecBuffer;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.VecBufferSequence;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.ASurfacePolygonalStructure;
import gov.takx.dataservices.persist.PersistableLine;

import java.nio.DoubleBuffer;
import java.util.List;
import java.util.function.Function;

/**
 * Concrete implementation of {@link ISurfaceStructure}.  This implementation uses the WW {@link gov.nasa.worldwind.render.SurfacePolylines} as
 * the WW representation of the structure.
 * <p/>
 * Renderable line surface structure implementation. We extend SurfacePolylines in order to be able to renderable
 * a large amount of lines quickly. This is dependent on CompoundVecBuffers. For most of our functionality,
 * we just delegate to the underlying SurfacePolylines.
 *
 */
public class SSLine extends ASurfaceStructureHolder<SurfacePolylines, PersistableLine>
{
    // Each vertex (location) is encoded as 2 double values: latitude and longitude
    private static final int COORDS_PER_VERTEX = 2;

    /**
     * Factory method to build from a given persistable.
     *
     * @param persistable Persistable entity holding the state from which to build
     * @since 3.0
     */
    static ISurfaceStructure createFromPersistable(IPersistableSurfaceStructure persistable)
    {
        return new SSLine((PersistableLine) persistable);
    }

    /**
     * SSPolygon constructor.
     *
     * @param sector            bounding sector of all points
     * @param compoundVecBuffer buffer containing all vertex information
     */
    public SSLine(Sector sector, CompoundVecBuffer compoundVecBuffer)
    {
        this(new PersistableLine(sector, vecBufferToBlockLocations(compoundVecBuffer, Function.identity())));
    }

    /**
     * Construct from persistable state.
     *
     * @param persistable Persistable holding our configuration
     * @since 3.0
     */
    private SSLine(PersistableLine persistable)
    {
        super(persistable.getSector(), persistable);
    }

    @Override
    protected LineRenderable makeRenderable()
    {
        return new LineRenderable(persistable.getSector(), createVecBufferSequence(persistable));
    }

    @Override
    protected void applyAttributesToRenderable(ShapeAttributes attributes)
    {
        renderable.setAttributes(attributes);
    }

    /**
     * Convert the block locations in the given polygonal into a vec buffer sequence.
     *
     * @param polygon Polygonal to process
     * @return vec buffer sequence with one sub-buffer per block of locations in the polygonal
     */
    protected static VecBufferSequence createVecBufferSequence(ASurfacePolygonalStructure polygon)
    {
        final int numOfVertices = polygon.getNumOfVertices();

        final VecBufferSequence vertexSequence = new VecBufferSequence(makeVecBuffer(numOfVertices), numOfVertices);

        for (List<LatLon> locations : polygon.getBlockLocations())
        {
            // Construct a VecBuffer holding our this set of locations
            final VecBuffer locationBuffer = makeVecBuffer(locations.size());
            locationBuffer.putLocations(0, locations);

            // And append it to the buffer sequence
            vertexSequence.append(locationBuffer);
        }

        return vertexSequence;
    }

    /**
     * Construct a {@link VecBuffer} with sufficient size to hold the coordinates of the given number of vertices.
     * Each vertex/location is encoded as 2 doubles, latitude and longitude.
     *
     * @param numOfVertices Number of vertices to use for sizing the returned buffer
     * @return new VecBuffer of an appropriate size to hold {@code numOfVertices} vertices (locations)
     */
    private static VecBuffer makeVecBuffer(int numOfVertices)
    {
        // Construct the backing buffer for the double values
        final int size = COORDS_PER_VERTEX * numOfVertices;
        final DoubleBuffer doubleBuffer = Buffers.newDirectDoubleBuffer(size);

        // And the VecBuffer on top of that
        return new VecBuffer(COORDS_PER_VERTEX, new BufferWrapper.DoubleBufferWrapper(doubleBuffer));
    }

    /**
     * Our renderable.
     *
     * @since 3.0
     */
    private class LineRenderable extends SurfacePolylines implements ISurfaceStructureRenderable
    {
        LineRenderable(Sector sector, CompoundVecBuffer buffer)
        {
            super(sector, buffer);
        }

        @Override
        public ISurfaceStructure getSurfaceStructure()
        {
            return SSLine.this;
        }
    }
}
