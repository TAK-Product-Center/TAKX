package gov.takx.api.coordinatesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for encoding/decoding locations as byte arrays.
 *
 * @see ILocation
 * @see RaptorLocation
 */
public class EncodingUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Used for encoding a single point to avoid allocating a new list on each invocation
    private static final List<ILocation> singlePointList = new ArrayList<>();

    /**
     * Construct the encoded (byte array) form of the points (location list).
     *
     * @param points List of points to encode
     * @return byte array of encoded data, empty array on any error
     */
    public static byte[] encodePoints(List<? extends ILocation> points)
    {
        // For N points the size of the final byte array will be: N*25 + 4
        // 25 = 3*8 + 1  (from 3 doubles and 1 boolean)
        // So, preallocate the array to avoid any resizing during encoding.  Use 26 as the factor just to add in
        // a little fudge, since being off by 1 causes the underlying BAOS to double the size of its allocated array.
        ByteArrayOutputStream baos = new ByteArrayOutputStream((points.size() * 26) + 4);
        DataOutputStream dos = new DataOutputStream(baos);

        try
        {
            dos.writeInt(points.size());

            for (ILocation vertex : points)
            {
                dos.writeDouble(vertex.getLatitude());
                dos.writeDouble(vertex.getLongitude());
                dos.writeDouble(vertex.getElevation());
                dos.writeBoolean(vertex.hasElevation());
            }

            dos.close();

            return baos.toByteArray();
        } catch (IOException e)
        {
            logger.error("Failed to construct packed points data", e);
        }

        return new byte[0];
    }

    /**
     * Construct the encoded (byte array) form of the point given.  Note this method is synchronized since
     * it operates on a shared member (singlePointList).  It should be faster overall to use synchronization
     * than to construct the list each time this method is called.
     *
     * @param point Single point to encode
     * @return byte array of encoded data, empty array on any error or if input was {@code null}
     */
    public synchronized static byte[] encodePoint(ILocation point)
    {
        singlePointList.clear();
        if (point != null) singlePointList.add(point);
        return encodePoints(singlePointList);
    }

    /**
     * Decode the raw bytes into the list of points (locations).
     *
     * @param data Raw data to decode
     * @return newly allocated list, may be empty if any error occurred during decoding
     */
    public static List<RaptorLocation> decodePoints(byte[] data)
    {
        List<RaptorLocation> points;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        try (DataInputStream dis = new DataInputStream(bais))
        {
            int count = dis.readInt();
            points = new ArrayList<>(count);

            for (int i = 0; i < count; i++)
            {
                double lat = dis.readDouble();
                double lon = dis.readDouble();
                double elevation = dis.readDouble();
                boolean hasElevation = dis.readBoolean();

                if (hasElevation)
                {
                    points.add(new RaptorLocation(lat, lon, elevation));
                } else
                {
                    points.add(new RaptorLocation(lat, lon));
                }
            }
        } catch (IOException e)
        {
            logger.error("Unable to decode vertex data", e);
            points = new ArrayList<>(); // Return empty list on error
        }
        // Nothing we can do

        return points;
    }
}
