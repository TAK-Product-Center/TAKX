package gov.takx.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static gov.takx.bootstrap.FlavorConstants.MF_ATTR_TAKX_FLAVOR;

/**
 * Utils for handling flavor logic.
 *
 * @since 5.5
 */
public final class FlavorUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Inspect the main attributes of the given jar file's manifest and determine if it is marked with a flavor.
     *
     * @param jarFile Jar file to inspect
     * @return the flavor string or {@code null} if not present
     * @throws IOException If the manifest could not be read
     */
    public static String getFlavor(JarFile jarFile) throws IOException
    {
        final String jarFileName = jarFile.getName();
        final Manifest manifest = jarFile.getManifest();
        if (manifest == null)
        {
            logger.debug("[{}] has no MANIFEST, so assuming no flavor", jarFileName);
            return null;
        }
        return manifest.getMainAttributes().getValue(MF_ATTR_TAKX_FLAVOR);
    }

    /**
     * Determines if the flavor string is {@link FlavorConstants#CIV} or effectively CIV ({@code null}, {@code ""}, {@code "civ"}).
     *
     * @param flavor the flavor string
     * @return {@code true} if the flavor string is effectively {@link FlavorConstants#CIV}
     */
    public static boolean isCiv(String flavor)
    {
        return flavor == null || flavor.isEmpty() || FlavorConstants.CIV.equalsIgnoreCase(flavor);
    }

    private FlavorUtils()
    {
    }
}
