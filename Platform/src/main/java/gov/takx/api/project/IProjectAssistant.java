package gov.takx.api.project;

import gov.takx.api.annotation.NoPluginImplementations;

import java.io.File;
import java.io.IOException;

/**
 * Provides easy access to a project's (or packaged project's) meta data.
 *
 * @since 1.16
 */
@NoPluginImplementations
public interface IProjectAssistant
{
    /**
     * Extract metadata from a project's *.projx (or a packaged project's *.pkgx) file and return the results in
     * the form of an {@link IProjectMetadata} object.
     *
     * @param projectFile The *.projx (or *.pkgx) file to inspect.
     * @return An object containing the project's metadata.
     * @throws IOException Thrown if there is an exception reading the project's metadata.
     */
    IProjectMetadata parseProjectFile(File projectFile) throws IOException;
}
