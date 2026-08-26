package gov.takx.api.kml;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Contract for performing kml layer operations.
 *
 * @since 1.24
 */
@InjectableService
@NoPluginImplementations
public interface IKmlLayerService
{
    /**
     * Load a KML/KMZ file and add it to the KML Overlays.  Note, this is a blocking operation.
     *
     * @param kmlFile File to import
     * @throws IOException if file does not exist on the filesystem
     * @since 1.24
     */
    void loadKmlFile(@Nonnull File kmlFile) throws IOException;

    /**
     * Load the KML/KMZ content of the given input stream and add it to the KML Overlays.
     * Note, this is a blocking operation.
     *
     * @param source Input stream to process
     * @param name Name to use for creating local file names from the stream
     * @since 3.1
     */
    void loadKml(@Nonnull InputStream source, @Nonnull String name);
}
