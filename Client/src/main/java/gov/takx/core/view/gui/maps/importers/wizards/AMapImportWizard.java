package gov.takx.core.view.gui.maps.importers.wizards;

import com.google.common.collect.ImmutableSet;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An importer for Imagery and Elevation files. Note that this is only for discrete files. We cannot, for example
 * drop a DTED directory on the globe. Also note that this importer will detect the type of layer that needs to be
 * created by looking at the contents of the file. I don't believe that it is possible to have mixed layers in a
 * file, but if this is not the case then we will need to make some adjustments. For example, if a NITF file can
 * have both an imagery layer and an elevation layer inside it.
 */
public abstract class AMapImportWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Set of file extensions that we know are NOT imagery or elevation data.
     */
    private static final Set<String> EXTENSION_BLACK_LIST = ImmutableSet.of(
            "bsf",
            "csv",
            "fakefile", // Used in tests
            "gns",
            "nmf",
            "pbl",
            "pcap",
            "pcapdump",
            "rns",
            "txt");

    // Predicate to test if a file might be a map data source file.
    private final Predicate<File> isMapFile;

    /**
     * Constructor.
     *
     * @param fileFilter     File filter to apply
     * @since 2.30
     */
    protected AMapImportWizard(Predicate<File> fileFilter)
    {
        isMapFile = file -> !EXTENSION_BLACK_LIST.contains(FileUtils.getExtension(file)) && fileFilter.test(file);
    }

    @Override
    public boolean supportsMultipleFiles()
    {
        return true;
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        try
        {
            // GDAL is used to test for imagery and elevation data sources, and it is noisy when parsing content.
            org.gdal.gdal.gdal.PushErrorHandler();
            return file.isDirectory() ? anyFileMatches(file, isMapFile) : isMapFile.test(file);
        } finally
        {
            org.gdal.gdal.gdal.PopErrorHandler();
        }
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        /*
         * At this point the importer runs the operation during the showImportWizardDialog. I think it
         * would be a good idea to capture the information in the dialog and save it in the properties
         * so that the file can be dropped and imported without showing the user a dialog at all. The only
         * big drawback there is that if the file has multiple layers we would need to present the dialog
         * for layer combination and name.
         *
         * Of course, I would also like to get the progress bar into the standard progress that goes on the
         * status panel with control given back to the user.
         *
         * And we still can drop a directory on the globe which means that things like DTED are not droppable.
         */
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    /**
     * Traverse a file hierarchy looking for any file that matches the given predicate.
     *
     * @param rootFile  Root of files to scan
     * @param predicate Predicate to match desired file
     * @return {@code true} if at least 1 file matches the predicate
     * @since 3.0
     */
    private boolean anyFileMatches(File rootFile, Predicate<File> predicate)
    {
        try (Stream<Path> paths = Files.walk(rootFile.toPath()))
        {
            return paths.filter(Files::isRegularFile).map(Path::toFile).anyMatch(predicate);
        } catch (IOException e)
        {
            logger.error("Failed to scan input file(s) from: {}", rootFile, e);
            return false;
        }
    }
}
