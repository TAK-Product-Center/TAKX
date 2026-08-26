package gov.takx.core.media.freja;

import gov.takx.core.media.MediaFrame;
import gov.takx.core.media.decode.DecodingException;
import gov.takx.core.media.decode.VideoDecoder;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

/**
 * Custom decoder for yellowkit video files
 * <p>
 * Note that normally custom code like this would belong in a plugin. Standard video formats multiplex any metadata
 * in a separate data stream. Freja instead embeds it in the EXIF field of each JPEG frame within the motion JPEG video.
 * This decoder simply takes the Freja metadata and splits it into a separate data stream, so it will look like any
 * other video stream with metadata.
 *
 * @since 2.8
 */
public class GeoTriggerDecoder extends VideoDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String FORMAT_IDENTIFIER = "FRJA";

    private static final byte[] COMMENT_MARKER = {(byte) 0xFF, (byte) 0xFE};

    /**
     * The start marker must be within MAX_BYTES_TO_SEARCH bytes of the start of the frame
     **/
    private static final int MAX_BYTES_TO_SEARCH = 30;

    /**
     * This is the size necessary to ensure that the metadata, if present, is contained in the frames bytes
     **/
    public static final int MAX_BYTES_FROM_START = MAX_BYTES_TO_SEARCH + 46;

    /**
     * Max number of bytes, from the beginning of the file to determine whether the file has Geo trigger metadata
     */
    private static final int MAX_BYTES_TO_INSPECT = 0x2024;

    /**
     * byte patterns and the offsets of where to look for them in a Freja encoded AVI file
     */
    private static final byte[] JUNK_PATTERN = {0x4a, 0x55, 0x4e, 0x4b};
    private static final int JUNK_OFFSET = 0x1f8;
    private static final byte[] FREJA_VERSION_PATTERN = {0x07, 0x00, 0x00, 0x00};
    private static final int FREJA_VERSION_OFFSET = 0x200;
    private static final byte[] DEVICE_TYPE_PATTERN = {0x6d, 0x6f, 0x76, 0x69};
    private static final int DEVICE_OFFSET_V1 = 0xffc;
    private static final int DEVICE_OFFSET_V2 = 0x1ffc;
    private static final byte[] METADATA_PATTERN = {(byte) 0xFF, (byte) 0xFE};
    private static final int METADATA_OFFSET_V1 = 0x101c;
    private static final int METADATA_OFFSET_V2 = 0x201c;

    public GeoTriggerDecoder(AVStream stream)
    {
        super(stream);
    }

    @Override
    public Optional<MediaFrame> decodeFrame(AVPacket packet, AVFrame frame) throws DecodingException
    {
        Optional<MediaFrame> mediaFrame = super.decodeFrame(packet, frame);

        // Set the metadata as necessary
        mediaFrame.ifPresent(media -> {
            Optional<MediaFrame.DataFrame> metadataFrame = getMetadata(packet);
            metadataFrame.ifPresent(metadata -> media.setDataFrame(metadata));
        });

        return mediaFrame;
    }

    private Optional<MediaFrame.DataFrame> getMetadata(AVPacket packet)
    {
        try
        {
            // Only copy the max number of bytes we need out of native memory
            int metadataSize = Math.min(packet.size(), MAX_BYTES_FROM_START);
            byte[] roughMetadataBytes = new byte[metadataSize];
            packet.data().get(roughMetadataBytes);

            // Attempt to find the bytes corresponding to the metadata
            byte[] metadataBytes = copyMetadataBytes(roughMetadataBytes);
            return Optional.of(new MediaFrame.DataFrame(FORMAT_IDENTIFIER, metadataBytes));
        } catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    /**
     * Copy out the bytes that just correspond to the Freja GPS metadata, as stored in the EXIF data of a JPEG video frame.
     *
     * @param data raw bytes of the video frame
     * @return a byte[] containing just the metadata
     * @throws IllegalArgumentException if the metadata can not be found
     */
    public static byte[] copyMetadataBytes(byte[] data)
    {
        int startIndex = searchForStart(data);

        // Couldn't find the data comment
        if (startIndex < 0)
        {
            throw new IllegalArgumentException("Couldn't find start index");
        }

        // Not enough space for the header
        if (startIndex + 2 >= data.length)
        {
            throw new IllegalArgumentException("Not enough space for the header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data, startIndex, 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        short length = buffer.getShort();

        if (startIndex + length > data.length)
        {
            throw new IllegalArgumentException("Not enough space for metadata");
        }

        return Arrays.copyOfRange(data, startIndex + 2, startIndex + length);
    }

    private static int searchForStart(byte[] data)
    {
        for (int i = 0; i < data.length - 1 && i < MAX_BYTES_TO_SEARCH; ++i)
        {
            if (data[i] == COMMENT_MARKER[0] && data[i + 1] == COMMENT_MARKER[1])
            {
                return i + 2;
            }
        }
        return -1;
    }

    /**
     * Test whether a file was encoded by a Freja device and is expected to have GPS metadata stored in the EXIF comment
     * section of each jpeg frame.
     *
     * @param fileName to test
     * @return true if the file probably has GPS data.
     */
    public static boolean hasGeoTriggerMetadata(String fileName)
    {
        File file = new File(fileName);
        byte[] data = new byte[MAX_BYTES_TO_INSPECT];
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file)))
        {
            dis.readFully(data, 0, data.length);

            if (matchesPattern(data, JUNK_OFFSET, JUNK_PATTERN) && matchesPattern(data, FREJA_VERSION_OFFSET, FREJA_VERSION_PATTERN))
            {
                if (matchesPattern(data, DEVICE_OFFSET_V1, DEVICE_TYPE_PATTERN) && matchesPattern(data, METADATA_OFFSET_V1, METADATA_PATTERN))
                {
                    // Is a Freja 1 device
                    return true;
                }

                if (matchesPattern(data, DEVICE_OFFSET_V2, DEVICE_TYPE_PATTERN) && matchesPattern(data, METADATA_OFFSET_V2, METADATA_PATTERN))
                {
                    // Is a Freja 2 device
                    return true;
                }
            }
        } catch (Exception e)
        {
            return false;
        }
        return false;
    }

    private static boolean matchesPattern(byte[] data, int offset, byte[] pattern)
    {
        if (data.length < offset + pattern.length)
        {
            return false;
        }

        byte[] dataAtOffset = Arrays.copyOfRange(data, offset, offset + pattern.length);
        return Arrays.equals(dataAtOffset, pattern);
    }
}
