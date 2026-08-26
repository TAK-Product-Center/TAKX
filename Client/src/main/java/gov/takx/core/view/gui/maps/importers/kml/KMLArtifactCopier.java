package gov.takx.core.view.gui.maps.importers.kml;

import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the copying of artifacts that are relatively referenced via the {@code <Link>} tag by a given KML file.
 * Artifacts generally include other KML files and icon files such as PNGs.
 *
 * @since 2.29.1
 */
public class KMLArtifactCopier
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Copies artifacts referenced by the source KML file to the target KML directory, preserving
     * directory hierarchy based on the source KML file.
     * <p>
     * For example, if source.kml contains a reference to
     * more_kml/data.kml, then the target directory will contain both source.kml and more_kml/data.kml after copying
     * has completed.
     * <p>
     * Note that this method also handles the copying of artifacts referenced by other referenced artifacts, i.e. the
     * copying is done recursively.
     *
     * @param sourceKmlPath       The absolute path of the source KML file to copy to the target directory. Artifacts
     *                            referenced by this file will also be copied to the target directory, along with any
     *                            artifacts referenced by <i>those</i> artifacts, and so on and so forth.
     * @param targetDirectoryPath The absolute path of the target directory to which the source KML file and all referenced
     *                            artifacts will be copied.
     * @return The absolute path of the new copy of the source KML file. This is effectively the path given by
     * [TARGET_DIRECTORY]/[SOURCE_KML_FILE_NAME]
     */
    public Path copyKmlArtifacts(@Nonnull Path sourceKmlPath, @Nonnull Path targetDirectoryPath)
    {
        Path targetKmlFile = createTargetFilePath(sourceKmlPath.getFileName().toString(), targetDirectoryPath);

        logger.debug("Copying KML file {} to directory {}", sourceKmlPath, targetDirectoryPath);

        copySourceFileToTarget(sourceKmlPath, targetKmlFile);

        final String parent = sourceKmlPath.getParent().toString();
        final Map<String, String> linksToReplace = new HashMap<>();
        final List<String> kmlLinksInSource = KMLLinkExtractor.getKMLLinks(sourceKmlPath.toFile());

        for (String link : kmlLinksInSource)
        {
            try
            {
                copyReferencedFileToTarget(parent, link, linksToReplace, targetDirectoryPath);
            } catch (IOException e)
            {
                logger.error("Error occurred while attempting to copy relatively-referenced KML files to target directory.", e);
            }
        }

        if (!linksToReplace.isEmpty())
        {
            replaceRelativePathsInFile(targetKmlFile.toFile(), linksToReplace, kmlLinksInSource);
        }

        return targetKmlFile;
    }

    /**
     * Copies the contents of an InputStream to the target KML directory.
     *
     * @param source Input stream to process
     * @param name Name of the file to create
     * @param targetDirectoryPath The absolute path of the target directory to write to
     * @return The absolute path of the file created
     * @since 3.1
     */
    Path copyKmlArtifact(InputStream source, String name, Path targetDirectoryPath)
    {
        final Path targetKmlFile = createTargetFilePath(name, targetDirectoryPath);

        logger.debug("Copying InputStream to {}", targetDirectoryPath);

        try
        {
            Files.copy(source, targetKmlFile);
        } catch (IOException e)
        {
            logger.error("Failed to write KML/KMZ stream to {}", targetKmlFile, e);
        }

        return targetKmlFile;
    }

    /**
     * Creates a target file path from a source file name and target directory. If a file by the same name already
     * exists in the target directory, then "(n)" will be appended to the source file name where "n" is the first
     * integer > 1 which creates a unique name.
     * <p>
     * Note that this method does not actually <i>create</i> the target file; it simply creates a path which can then be
     * used to instantiate the target file, e.g. through a copy operation.
     *
     * @param sourceFileName      The name of the source file from which the target file name will be derived.
     * @param targetDirectoryPath The absolute path of the target directory from which the target file path will be created.
     * @return The newly created target file path.
     */
    private Path createTargetFilePath(String sourceFileName, Path targetDirectoryPath)
    {
        final String targetFileName = FileUtils.getNextNameIteration(targetDirectoryPath.toFile(), sourceFileName);

        return targetDirectoryPath.resolve(targetFileName);
    }

    /**
     * Copies the source file to the target file, catching and logging any {@link IOException}s that may occur in the
     * process.
     *
     * @param sourcePath The absolute path of the source file to copy from.
     * @param targetPath The absolute path of the target file to copy to.
     */
    private void copySourceFileToTarget(Path sourcePath, Path targetPath)
    {
        try
        {
            FileUtils.copy(sourcePath.toFile(), targetPath.toFile());
        } catch (IOException e)
        {
            logger.error("Error occurred while attempting to copy {} to {}", sourcePath, targetPath, e);
        }
    }

    /**
     * Copy the file referenced in the given KML link into the target directory. If the link is a relative path, then copy it to
     * the same relative path in the target directory so as to preserve the directory hierarchy.
     * <p>
     * Note that this method may be called recursively in order to copy files that are referenced by referenced files.
     *
     * @param baseTargetDirectory Absolute path string of the base directory of the source KML file.
     * @param kmlLink             The link path taken from a given <Link> tag in a KML file.
     * @param linksToReplace      A map representing relative links to artifacts, where the key is the link to
     *                            replace and the value is the link to replace it with. This is added to in the case
     *                            that a KML file contains a link to an artifact that gets indexed, e.g. source.kml
     *                            references data.kml, but the data.kml that it references gets indexed to data(2).kml and
     *                            thus source.kml must be updated to reflect the new index.
     * @param targetDirectoryPath The target directory to which artifacts should be copied. For the source KML path that
     *                            gets passed into {@link #copyKmlArtifacts(Path, Path)}, this will be the same as the
     *                            baseTargetDirectory.
     * @throws IOException If an error occurs while copying the referenced files.
     */
    private void copyReferencedFileToTarget(String baseTargetDirectory, String kmlLink, Map<String, String> linksToReplace, Path targetDirectoryPath) throws IOException
    {
        // If it's an http link, then skip it
        if (StringUtils.startsWithIgnoreCase(kmlLink, "http:") || StringUtils.startsWithIgnoreCase(kmlLink, "https:"))
        {
            logger.debug("Ignore HTTP link: {}", kmlLink);
            return;
        }

        final File referencedFile = new File(kmlLink);

        if (referencedFile.isAbsolute())
        {
            logger.warn("KML link uses absolute path, resource not copied to VFS: {}", kmlLink);
            return;
        }

        // The kml link is a relative path.  Ensure that it doesn't "break out" of the current directory
        // by using ".." constructs.  The File() constructor always normalizes the path, so convert backslashes
        // to forward slashes to avoid issues with split (it's a regex, not a simple string) so we can
        // just split on the file separator and check for ".." entries.

        for (String part : referencedFile.toString().replace(File.separatorChar, '/').split("/"))
        {
            if ("..".equals(part))
            {
                logger.error("KML link contains '..' - resource not copied to VFS: {}", kmlLink);
                return;
            }
        }

        final String refParent = referencedFile.getParent();
        if (refParent != null)
        {
            // Construct the relative directory components so the reference in the KML file will remain
            // valid upon reload.

            final File parentDir = new File(targetDirectoryPath.toFile(), refParent);
            if (!parentDir.exists() && !parentDir.mkdirs())
            {
                logger.error("Failed to create KML resource directory: {}", parentDir);
            }
        }

        final File sourceLink = new File(baseTargetDirectory, kmlLink);
        File target = new File(targetDirectoryPath.toFile(), kmlLink);
        boolean needsReplacement = false;

        // It's common for numerous links in the same KML file to reference the same resource, so avoid
        // copying the same file repeatedly.  In some files this was on the order of 1000 copies of the same file.

        if (target.exists())
        {
            if (org.apache.commons.io.FileUtils.contentEquals(target, sourceLink))
            {
                logger.debug("File already present: {}", target);
                return;
            }

            File targetParent = target.getParentFile();

            target = new File(targetParent, FileUtils.getNextNameIteration(targetParent, target.getName()));
            needsReplacement = true;

            while (target.exists())
            {
                if (org.apache.commons.io.FileUtils.contentEquals(target, sourceLink))
                {
                    break;
                }

                target = new File(FileUtils.getNextNameIteration(target.getParentFile(), target.getName()));
            }
        }

        if (needsReplacement)
        {
            String replacementPath = refParent + File.separator + target.getName();

            logger.debug("Replacing relative path {} with {}", kmlLink, replacementPath);

            linksToReplace.put(kmlLink, replacementPath);
        }

        FileUtils.copy(sourceLink, target);

        final Map<String, String> referencedFileLinksToReplace = new HashMap<>();
        final List<String> kmlLinks = KMLLinkExtractor.getKMLLinks(target);

        for (String currentLink : kmlLinks)
        {
            copyReferencedFileToTarget(baseTargetDirectory, refParent + "/" + currentLink, referencedFileLinksToReplace, targetDirectoryPath);
        }

        if (!referencedFileLinksToReplace.isEmpty())
        {
            replaceRelativePathsInFile(target, referencedFileLinksToReplace, kmlLinks);
        }
    }

    /**
     * Replaces the relative paths found in a KML file with their new paths in the case that any files are renamed (e.g.
     * data.kml becomes data(2).kml because there already existed a data.kml in the target directory).
     *
     * @param kmlFile                The KML file in which the relative paths will be replaced.
     * @param relativePathsToReplace The map of relative paths that should be replaced, where the key is the path to replace
     *                               and the value is the path to replace it with.
     * @param knownRelativePaths     The relative paths that are known to exist in the give KML file.
     */
    private void replaceRelativePathsInFile(File kmlFile, Map<String, String> relativePathsToReplace, List<String> knownRelativePaths)
    {
        try
        {
            String fileContent = Files.readString(kmlFile.toPath());

            for (String currentLinkInFile : knownRelativePaths)
            {
                if (relativePathsToReplace.containsKey(currentLinkInFile))
                {
                    fileContent = fileContent.replace(currentLinkInFile, relativePathsToReplace.get(currentLinkInFile));
                }
            }

            Files.writeString(kmlFile.toPath(), fileContent);
        } catch (IOException e)
        {
            logger.debug("Unable to replace relative paths, files relatively referenced may not be properly referenced now.", e);
        }
    }
}
