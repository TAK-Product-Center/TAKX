package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.RaptorLocation;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common base for polygonal surface structures, both surface and extruded.
 *
 * @since 3.0
 */
@Entity
public abstract class APolygonalStructure<L extends LatLon> extends PersistableSurfaceStructure
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Each vertex (location) is encoded as 2 or 3 double values: latitude and longitude, and elevation for Positions
    private static final int MAX_COORDS_PER_VERTEX = 3;

    // Complete list of vertices encoded into a byte array
    @Column(length = Length.LONG32)
    protected byte[] vertices;

    // representation of the bounding sector of this surface structure ordered minLat, maxLat, minLon, maxLon
    private double[] sector;

    // number of vertices for this surface structure
    private int numOfVertices;

    private int numberOfBlocks;

    // The number of lat/lon vertices per location block. This will be decoded as an int[] array,
    // of length numberOfBlocks where each int is the number of vertices that are contained within that block.
    @Column(length = Length.LONG32)
    private byte[] blockCoordCount;

    @Transient
    private List<List<L>> blockLocations;

    @Transient
    protected List<L> locations;

    // For hibernate
    protected APolygonalStructure()
    {
    }

    protected APolygonalStructure(@Nonnull Sector sector, @Nonnull List<List<L>> blockLocations)
    {
        this.sector = sector.asDegreesArray();

        internBlocks(blockLocations);

        final LatLon ll = locations.isEmpty() ? sector.getCentroid() : locations.get(0);

        setLocation(new RaptorLocation(ll.getLatitude().getDegrees(), ll.getLongitude().getDegrees()));
    }

    /**
     * @return The total number of vertices for this structure
     */
    public int getNumOfVertices()
    {
        return numOfVertices;
    }

    public Sector getSector()
    {
        return Sector.fromDegrees(sector);
    }

    @Override
    public Iterable<L> getVertices()
    {
        return Collections.unmodifiableList(locations);
    }

    /**
     * @return The block locations for this structure
     */
    public List<List<L>> getBlockLocations()
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return blockLocations;
    }

    @PostLoad
    protected void onLoad()
    {
        try
        {
            decodeVertexLocations();

            // To simplify DB upgrades, handle the case where the numberOfBlocks is 0
            if (numberOfBlocks == 0)
            {
                // Just install the "locations" as a single block
                numberOfBlocks = 1;
                blockLocations = Collections.singletonList(locations);
            } else
            {
                decodeBlocks();
            }
        } catch (IOException e)
        {
            logger.error("Failed to decode vertices", e);
        }
    }

    /**
     * Store the given block locations and encode them into {@code numberOfBlocks} and {@code blockCoordCount}.
     *
     * @param blockLocations Block locations to intern
     */
    private void internBlocks(List<List<L>> blockLocations)
    {
        this.blockLocations = blockLocations;
        numberOfBlocks = blockLocations.size();

        // Create a byte[] array out of series of int values of our block lengths
        final ByteArrayOutputStream countsByteStream = new ByteArrayOutputStream((numberOfBlocks * Integer.BYTES));

        try (DataOutputStream countsDataStream = new DataOutputStream(countsByteStream))
        {
            for (List<? extends LatLon> blockLocation : blockLocations)
            {
                countsDataStream.writeInt(blockLocation.size());
            }
            blockCoordCount = countsByteStream.toByteArray();
        } catch (IOException e)
        {
            logger.error("Failed to intern block locations", e);
        }

        // And now flatten all the blocks into a single list and intern that

        final List<L> flatLocations = blockLocations.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        internLocations(flatLocations);
    }

    /**
     * Store the given locations and then encode them into the {@code vertices} byte array.
     *
     * @param flatLocations List of locations to intern
     */
    private void internLocations(List<L> flatLocations)
    {
        locations = flatLocations;

        numOfVertices = locations.size();

        // Pre-size the output stream to handle our data.  We know that we hold LatLon or Position objects,
        // so we will just use the larger of the two.  If this ever needs to be made more general, then we can
        // add a method to provide the number of coordinates per vertex.
        final int sizeInBytes = numOfVertices * MAX_COORDS_PER_VERTEX * Double.BYTES;

        try (
             ByteArrayOutputStream baos = new ByteArrayOutputStream(sizeInBytes);
             DataOutputStream dos = new DataOutputStream(baos))
        {
            for (L ll : locations) writeVertex(ll, dos);

            vertices = baos.toByteArray();
        } catch (IOException e)
        {
            logger.error("Failed to encode location vertices", e);
        }
    }

    /**
     * Decode the byte array containing our location data.
     */
    private void decodeVertexLocations() throws IOException
    {
        locations = new ArrayList<>(numOfVertices);

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(vertices)))
        {
            for (int i = 0; i < numOfVertices; i++)
            {
                locations.add(readVertex(dis));
            }
        }
    }

    /**
     * Decode the persisted form of the block locations and restore it as a list of lists of locations,
     */
    private void decodeBlocks()
    {
        blockLocations = new ArrayList<>(numberOfBlocks);

        if (numberOfBlocks == 0) return; // No data to decode

        // stream to read in the int[] array containing the number of vertices for each block
        try (DataInputStream blockDIS = new DataInputStream(new ByteArrayInputStream(blockCoordCount)))
        {
            int index = 0;

            for (int i = 0; i < numberOfBlocks; i++)
            {
                final int vertexCount = blockDIS.readInt();
                final List<L> locations = new ArrayList<>(vertexCount);

                for (int j = 0; j < vertexCount; j++)
                {
                    locations.add(this.locations.get(index++));
                }

                blockLocations.add(locations); // Add this block
            }
        } catch (IOException e)
        {
            logger.error("Failed to decode structure vertex blocks", e);
        }
    }

    /**
     * Write the given location into the given into the given output stream.
     *
     * @param location Location to write
     * @param dos      Output stream to which to write
     */
    protected abstract void writeVertex(L location, DataOutputStream dos) throws IOException;

    /**
     * Read data from the given input stream and produce a vertex location.
     *
     * @param dis Input stream from which to read
     * @return Constructed location
     */
    protected abstract L readVertex(DataInputStream dis) throws IOException;
}
