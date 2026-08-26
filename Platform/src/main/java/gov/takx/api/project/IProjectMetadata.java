package gov.takx.api.project;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.commons.utils.PlatformVersion;

import java.util.List;
import java.util.UUID;

/**
 */
@NoPluginImplementations
public interface IProjectMetadata
{
    /**
     * @return The UUID of the project, assigned at project creation
     * @since 2.0.0
     */
    UUID getProjectUUID();

    /**
     * @return Returns name of the project.
     */
    String getProjectName();

    /**
     * Returns a collection of Metadata indicating the map objects included in the project.
     *
     * @return A collection of Map Object metadata
     */
    List<IProjectMapObjectMetadata> getMapObjects();

    /**
     * Returns the version of the platform when this project was last saved.
     *
     * @return The project's platform version.
     */
    PlatformVersion getPlatformVersion();

    /**
     * Returns a boolean indicating if the file is a package or project metadata file.
     *
     * @return {@code true} if this file is a package file, {@code false} if it is a *.projx file.
     */
    boolean isPackage();

    /**
     * Returns the project's description.
     *
     * @return The project's description.
     */
    String getDescription();

    /**
     * Returns the project's end date.
     *
     * @return The project's end date.
     */
    long getEndDate();

    /**
     * Returns the project's location description.
     *
     * @return The project's location.
     */
    String getLocation();

    /**
     * Returns any notes attached to the project.
     *
     * @return The project's notes.
     */
    String getNotes();

    /**
     * Returns a String indicating the size of the project.
     *
     * @return The size of the project.
     */
    String getSize();

    /**
     * Returns the project's start date.
     *
     * @return The project's start date.
     */
    long getStartDate();

    /**
     * Returns the path to the VFS included in this project.
     *
     * @return The path to the project's VFS.
     */
    String getVfsSize();

    /**
     * Returns a boolean indicating if the project was in VFS Offline mode when it was last saved.
     *
     * @return {@code true} ff the project was in VFS Offline mode when it was last saved, {@code false}
     * otherwise.
     */
    boolean getWmsOfflineMode();

    /**
     * @return The creation date of the project
     */
    long getCreationDate();

    /**
     * @return The title of the project
     */
    String getTitle();

    String getDBType();

    /**
     * @return Database version
     * @since 3.0
     */
    String getDBVersion();
}
