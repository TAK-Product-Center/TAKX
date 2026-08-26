package gov.takx.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Extract our version and build data from the bootstrap jar manifest.
 *
 * @since 2.0.0
 */
public final class Version
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String IMPLEMENTATION_VERSION_KEY = "Implementation-Version";
    private static final String BUILD_NUMBER_KEY = "Build-Number";
    private static final String BUILD_DATE_KEY = "Build-Date";
    private static final String UNKNOWN = "(unknown)";

    private static String VERSION_STRING = UNKNOWN;
    private static String FULL_VERSION_STRING = UNKNOWN;
    private static String PRE_RELEASE_TAG = "";
    private static String BUILD_NUMBER = UNKNOWN;
    private static String BUILD_DATE = UNKNOWN;

    static
    {
        boolean gotVersion = false;

        try
        {
            // Get our version data from the bootstrap jar manifest
            final Manifest manifest = getManifestFromCodeSource();

            gotVersion = manifest != null && extractVersionFromManifest(manifest);
        } catch (IOException | URISyntaxException e)
        {
            logger.error("Failed to read manifest", e);
        }

        // Fallback to manually locating the bootstrap jar file and read the manifest from there.
        // This path is needed to support unit tests which directly reference the build/classes and not
        // a jar.
        if (!gotVersion && !getVersionFromLoadJar())
        {
            logger.error("Failed to determine our program version!");
        }
    }

    /**
     * Read the manifest from the jar file indicated in our code source.  Handle code sources of both 'jar' and 'file'
     * protocols.
     *
     * @return Manifest, or {@code null} if our code source isn't a 'jar' or 'file' protocol URL
     * @since 2.18
     */
    private static Manifest getManifestFromCodeSource() throws IOException, URISyntaxException
    {
        // The approach is to get our "code source" location, which should directly identify the jar
        // from which this class was loaded.

        final URL location = Version.class.getProtectionDomain().getCodeSource().getLocation();
        final String protocol = location.getProtocol();

        logger.debug("bootstrap jar location: {}", location);

        switch (protocol)
        {
            case "jar":
                return ((JarURLConnection) location.openConnection()).getManifest();

            case "file":
                // If the file we're give is a directory, it means we are running from withing IJ, so return
                // null and let processing default to trying to read the bootstrap jar in bin/load directly.
                if (new File(location.getPath()).isDirectory())
                {
                    logger.debug("location is directory, default to load jar");
                    return null;
                }

                // It seems that in some build cases (like when the jar is in the gradle cache and not
                // in the local maven repo, we get a 'file' protocol URL instead of 'jar', so deal with that.
                //
                // Just stick a 'jar:' in front and that gives us 'jar:file:....' which should work fine
                final URL jarUrl = new URL("jar:" + location.toExternalForm() + "!/");
                return ((JarURLConnection) jarUrl.openConnection()).getManifest();

            default:
                logger.error("Unknown URL protocol for: {}", location);
        }
        return null;
    }

    /**
     * @return the version string
     */
    public static String getVersionString()
    {
        return VERSION_STRING;
    }

    /**
     * @return the FULL version string, this will include any pre-release component, like -SNAPSHOT or -RC1
     */
    public static String getFullVersionString()
    {
        return FULL_VERSION_STRING;
    }

    /**
     * @return The pre-release tag for the build, like 'SNAPSHOT' or 'RC1'
     */
    public static String getPreReleaseTag()
    {
        return PRE_RELEASE_TAG;
    }

    /**
     * @return The build number
     */
    public static String getBuildNumber()
    {
        return BUILD_NUMBER;
    }

    /**
     * @return The short form of the build number
     */
    public static String getShortBuildNumber()
    {
        return BUILD_NUMBER.substring(0, 7);
    }

    /**
     * @return The build date
     */
    public static String getBuildDate()
    {
        return BUILD_DATE;
    }

    public static void main(String[] args)
    {
        System.out.printf("Full version '%s'\nversion '%s'\nPre-release '%s'\nbuild '%s'\nbuild short '%s'\ndate '%s'", // NOPMD
                getFullVersionString(), getVersionString(), getPreReleaseTag(), getBuildNumber(), getShortBuildNumber(), getBuildDate());
    }

    /**
     * Try to locate the bootstrap jar file and extract the version data from its manifest.
     *
     * @return {@code true} if we found the version data
     */
    private static boolean getVersionFromLoadJar()
    {
        File loadDir = new File("load");

        final File[] files = loadDir.listFiles((dir, name) -> name.contains("bootstrap"));

        if (files == null || files.length != 1)
        {
            return false;
        }

        try (JarFile jarFile = new JarFile(files[0]))
        {
            return extractVersionFromManifest(jarFile.getManifest());
        } catch (IOException e)
        {
            logger.error("Failed to open bootstrap jar file", e);
        }
        return false;
    }

    /**
     * Extract the version data from the given manifest.
     *
     * @param manifest Manifest to process
     * @return {@code true} if the attributes were extracted
     */
    private static boolean extractVersionFromManifest(Manifest manifest)
    {
        final var mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null)
        {
            FULL_VERSION_STRING = mainAttributes.getValue(IMPLEMENTATION_VERSION_KEY);

            final int dash = FULL_VERSION_STRING.indexOf('-');
            if (dash > 0)
            {
                VERSION_STRING = FULL_VERSION_STRING.substring(0, dash);
                PRE_RELEASE_TAG = FULL_VERSION_STRING.substring(dash + 1);
            } else
            {
                VERSION_STRING = FULL_VERSION_STRING;
            }

            BUILD_NUMBER = mainAttributes.getValue(BUILD_NUMBER_KEY);
            BUILD_DATE = mainAttributes.getValue(BUILD_DATE_KEY);
            return true;
        }
        return false;
    }

    private Version()
    {
    }
}
