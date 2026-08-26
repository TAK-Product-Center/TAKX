package gov.tak.platform.datapackage;

import gov.tak.platform.datapackage.content.PackageManifest;
import gov.takx.api.annotation.UseClientClassLoader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;

/**
 * Utility class for reading manifest and content from a Data Package zip file.
 *
 * @since 3.0
 */
public class PackageReader extends ZipReader
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PACKAGE_MANIFEST_ELEM = "<MissionPackageManifest ";

    // Allow the manifest to be in two locations (for better compatibility with older packages)
    private static final List<String> MANIFEST_LOCATIONS = List.of("MANIFEST/manifest.xml", "manifest.xml");

    private static final Predicate<ZipEntry> PREFERENCES_MATCHER = zipEntry -> zipEntry.getName().toLowerCase().endsWith(".pref");

    private JAXBContext manifestJaxbContext;

    /**
     * Construct on the given package file.
     */
    public PackageReader(@Nonnull File packageFile) throws IOException
    {
        super(packageFile);
    }

    /**
     * @return {@code true} if the file contains a preferences file (a file with suffix {@code .pref})
     * @since 3.2
     */
    public boolean hasPreferences()
    {
        return findEntry(PREFERENCES_MATCHER) != null;
    }

    /**
     * @return The first preferences file (a file with suffix {@code .pref}) in the zip file
     * @since 3.2
     */
    @Nullable
    public PreferencesReader.Preferences getPreferences() throws IOException
    {
        final String entry = findEntry(PREFERENCES_MATCHER);
        return entry != null ? new PreferencesReader().parsePreferences(readEntryAsString(entry)) : null;
    }

    /**
     * @return {@code true} if the file contains a manifest
     */
    public boolean hasPackageManifest()
    {
        return readManifest() != null;
    }

    /**
     * @return The parsed package manifest
     * @throws IOException If it can't be read or parsed properly
     */
    @Nonnull
    public PackageManifest getManifest() throws IOException
    {
        try
        {
            final String manifestXml = readManifest();

            if (manifestXml == null) throw new IOException("No manifest found in: " + zipFilePath);

            return (PackageManifest) getJaxbContext().createUnmarshaller().unmarshal(new StringReader(manifestXml));
        } catch (JAXBException e)
        {
            final String message = "Failed to parse package manifest from " + zipFilePath;
            logger.error(message, e);
            throw new IOException(message, e);
        }
    }

    /**
     * Get/create JAXBContext.  Need to do this in a method, so we can use the {@link UseClientClassLoader} annotation.
     * @since 3.1
     */
    @UseClientClassLoader
    private JAXBContext getJaxbContext() throws IOException
    {
        if (manifestJaxbContext != null) return manifestJaxbContext;
        try
        {
            return manifestJaxbContext = JAXBContext.newInstance(PackageManifest.class);
        } catch (JAXBException e)
        {
            final String message = "Failed to initialize JAXBContext for " + PackageManifest.class;
            logger.error(message, e);
            throw new IOException(message, e);
        }
    }

    /**
     * @return String content of the manifest (from any of our configured locations), but only if it has the proper
     * opening tag.
     */
    @Nullable
    private String readManifest()
    {
        return MANIFEST_LOCATIONS.stream()
                .map(entry -> readEntryAsString(entry, true))
                .filter(manifestXml -> manifestXml.contains(PACKAGE_MANIFEST_ELEM))
                .findFirst()
                .orElse(null);
    }
}
