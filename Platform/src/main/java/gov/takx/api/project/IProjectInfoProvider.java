package gov.takx.api.project;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.UUID;

/**
 * A provider of information about the currently open project.  An instance of this interface is available in
 * all CDI containers (core and plugins) and can be directly injected.
 *
 * @since 2.0.0
 */
@InjectableService
@NoPluginImplementations
public interface IProjectInfoProvider
{
    /**
     * @return The name of the currently open project
     */
    String getProjectName();

    /**
     * @return The root directory path for the currently open project
     */
    String getProjectDirectory();

    /**
     * @return The UUID of the project, assigned at project creation
     * @since 2.0.0
     */
    UUID getProjectUUID();
}
