package gov.takx.api.project;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Descriptor data for a TAKX Project: name, creation time, etc.
 *
 */
@NoPluginImplementations
public interface IRaptorProjectDescriptor
{
    /**
     * @return The name of the project
     */
    String getProjectName();

    /**
     * @return The full path to the project file for this project
     */
    String getProjectFilePath();

    /**
     * @return The directory containing this project
     */
    String getProjectDirectory();

    /**
     * Time of creation of TAKX Project
     *
     * @return time of project creation
     */
    long getProjectCreationTime();
}
