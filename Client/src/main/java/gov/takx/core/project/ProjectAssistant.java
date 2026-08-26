package gov.takx.core.project;

import gov.takx.api.project.IProjectAssistant;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.commons.file.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static gov.takx.api.project.ProjectConstants.PACKAGE_EXTENSION;
import static gov.takx.api.project.ProjectConstants.PROJECT_EXTENSION;

/**
 * Provides access to project metadata.
 *
 * @since 1.16
 */
public class ProjectAssistant implements IProjectAssistant
{
    @Override
    public IProjectMetadata parseProjectFile(File projectOrPackageFile) throws IOException
    {
        return getProjectMetadata(projectOrPackageFile);
    }

    /**
     * Parse the metadata from the projx file for a given source (which may be a directory, a projx file, or a pkgx
     * file).  If it is a pkgx file, then the projx file will be extracted from the package zip file.
     *
     * @param source Source of project.  It can be the directory containing the project, the .projx file, or a .pkgx file.
     */
    private IProjectMetadata getProjectMetadata(File source) throws IOException
    {
        final File originalSource = source;

        boolean isPackage = false;
        final String projectName;
        final String projxContent;

        // If a project directory is passed in instead of the .projx file itself, look for the .projx file
        // in the directory.
        if (source.isDirectory())
        {
            source = getProjectFileFromProjectDirectory(source);

            if (source == null) throw new IOException("No .projx file found in directory: " + originalSource);
        }

        final String extension = FileUtils.getExtension(source);

        if (PROJECT_EXTENSION.equalsIgnoreCase(extension))
        {
            projxContent = Files.readString(source.toPath());
            projectName = FilenameUtils.getBaseName(source.getPath());
        } else if (PACKAGE_EXTENSION.equalsIgnoreCase(extension))
        {
            // This is a package, so read the content from the zip entry.

            isPackage = true;

            final String projxPathInZip = FileUtils.findFirstFileWithGivenExtension(source, PROJECT_EXTENSION);
            projectName = FilenameUtils.getBaseName(projxPathInZip);
            projxContent = new String(ZipUtil.unpackEntry(source, projxPathInZip));
        } else
        {
            throw new IOException("Don't know how to handle: " + originalSource);
        }

        return new ProjectFileParser().parseProjectFile(projectName, projxContent, isPackage);
    }

    /**
     * Obtain the .projx file from the project directory.
     *
     * @param projectDirectory The project directory.
     * @return The project's configuration file, may be {@code null} if not found
     */
    private File getProjectFileFromProjectDirectory(File projectDirectory)
    {
        final File[] files = projectDirectory.listFiles();

        if (files != null)
        {
            for (File f : files)
            {
                if (f.isFile() && f.getName().endsWith(PROJECT_EXTENSION))
                {
                    return f;
                }
            }
        }

        return null;
    }
}
