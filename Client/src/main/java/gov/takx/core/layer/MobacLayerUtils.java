package gov.takx.core.layer;

import gov.takx.core.globe.layers.worldwind.LayerFactory;
import jakarta.annotation.Nonnull;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * MOBAC layer utility functions.
 *
 * @since 5.8
 */
public final class MobacLayerUtils
{
    // XML element that identifies a custom map source
    private static final String MAP_SOURCE_ELEMENT = "<" + LayerFactory.CUSTOM_MAP_SOURCE + ">";

    /**
     * Test if a file appears to be a map source XML config file.
     *
     * @param file File to test
     * @return {@code true} if the file contains the custom map source XML element
     */
    public static boolean isMapSourceXml(@Nonnull File file)
    {
        if (!"xml".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) return false; // Must be XML

        try (InputStream inputStream = new FileInputStream(file))
        {
            return isMapSourceXml(inputStream);
        } catch (IOException e)
        {
            return false;
        }
    }

    public static boolean isMapSourceXml(@Nonnull String string)
    {
        return string.contains(MAP_SOURCE_ELEMENT);
    }

    /**
     * Test if an input stream appears to contain a map source XML config file.
     *
     * @param inputStream input stream to test
     * @return {@code true} if the input stream contains the custom map source XML element
     */
    public static boolean isMapSourceXml(@Nonnull InputStream inputStream)
    {
        // We only need the first block of the file to determine if it is one of our config files.
        // If it's valid, the element will always be within the first 512 characters.
        try
        {
            final byte[] bytes = inputStream.readNBytes(512);
            return new String(bytes).contains(MAP_SOURCE_ELEMENT);
        } catch (IOException e)
        {
            return false;
        }
    }
}
