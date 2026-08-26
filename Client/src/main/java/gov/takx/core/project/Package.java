package gov.takx.core.project;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.FileStore;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.project.ProjectConstants;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.view.gui.projectadmin.PackageExtractionLocationDialog;

import javax.swing.JFrame;

import java.io.File;

/**
 * Handles the extraction of a TAKX project from a zipped package and the determining of a new project location and name.
 * Instantiated class provides info about the resulting extracted project.
 *
 * @since 3.4 (renamed from RaptorPackage)
 */
public class Package
{
    /**
     * The location that the user chose to store the new project.
     */
    private final String parentDirectory;

    /**
     * The directory to which the file was extracted. This will be the project name, made unique via
     * {@link FileUtils#getNextNameIteration(java.io.File, String)}.
     */
    private final String projectDirectory;

    /**
     * The path to the projx file in the {@link #projectDirectory}.
     */
    private final String projectFile;

    /**
     * The project name, as specified by the projx file.
     */
    private final String projectName;

    /**
     * Constructor. Prompts the user to select an extraction location and project name, extracts the package and sets
     * the new project location and name values.
     *
     * @param packagePath    The path the the package file.
     * @param extractionPath The location where the package should be extracted.
     */
    public Package(String packagePath, String extractionPath)
    {
        final File zipFile = new File(packagePath);

        // Set the new target project location.
        parentDirectory = extractionPath;
        File projDir = new File(parentDirectory);

        // Pull the project name from the name of the project preferences file (projx) within the zip file.
        String projxFilePath = FileUtils.findFirstFileWithGivenExtension(zipFile, ProjectConstants.PROJECT_EXTENSION);

        // Get the name of the projx file.
        projectName = FileUtils.extractFileNameWithoutExtensionFromPath(projxFilePath);

        // Check to see if the name already exists. If so, make it unique.
        String projectDirectoryName = FileUtils.getNextNameIteration(projDir, projectName);

        // Build the new full path to the project.
        projectDirectory = parentDirectory + File.separator + projectDirectoryName;

        // Build the new full path to the projx file. projxFilePath is a path, so get the file name.
        projectFile = projectDirectory + File.separator + new File(projxFilePath).getName();

        // Extract the zip archive into the new project directory
        FileUtils.extractTopLevelDirFromZip(zipFile, projDir, projectName, projectDirectoryName);

        // expand imagery files if it is a packaged project

        // TODO: refactor away these references to WW
        final FileStore dataFileStore = WorldWind.getDataFileStore();
        final File dataFileCache = dataFileStore.getWriteLocation();
        final File parentFile = dataFileCache.getParentFile();

        FileUtils.extractTopLevelDirFromZip(zipFile, parentFile, dataFileCache.getName());

        for (File f : dataFileStore.getLocations())
        {
            if (dataFileStore.isInstallLocation(f.getAbsolutePath()))
            {
                FileUtils.extractTopLevelDirFromZip(zipFile, parentFile, f.getName());
                break;
            }
        }
    }

    /**
     * Returns the directory into which the project was extracted.
     *
     * @return The directory into which the project was extracted.
     */
    public String getExtractedProjectParentDirectory()
    {
        return parentDirectory;
    }

    /**
     * Returns the full path to the project directory.
     *
     * @return The full path to the project directory.
     */
    public String getProjectPath()
    {
        return projectDirectory;
    }

    /**
     * The the full path to the extracted projx file.
     *
     * @return The full path to the extracted projx file.
     */
    public String getProjectFile()
    {
        return projectFile;
    }

    /**
     * The name of the project as extracted from the projx file.
     *
     * @return The name of the project.
     */
    public String getProjectName()
    {
        return projectName;
    }

    /**
     * Presents a file chooser dialog to the user to allow the to select an extraction location.
     *
     * @param defaultPath The default project location for the program installation.
     * @return The location chosen by the user.
     */
    @InvokeAndWaitOnEDT
    public static String choosePackageExtractionPath(final String defaultPath)
    {
        try
        {
            return new PackageExtractionLocationDialog(JFrame.getFrames()[0], defaultPath).showDialog();
        } catch (Exception e)
        {
            return defaultPath;
        }
    }
}
