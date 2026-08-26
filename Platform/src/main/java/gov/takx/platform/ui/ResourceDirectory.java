package gov.takx.platform.ui;

import gov.takx.api.annotation.Internal;

/**
 * Enum to represent the different directories that can be used to retrieve resources for the user interface.
 * <p>
 * This is an internal support class.  Our UI resources and their organization is not part of the published API,
 * and can be changed at will.
 *
 * @since 3.0
 */
@Internal
public enum ResourceDirectory
{
    PLATFORM_ICONS("icons/", false),
    RESOURCES("resources/", true),
    TAK_ASSETS_ICONS("resources/tak-assets/icons/", true),
    TAK_ASSETS_PNG("resources/tak-assets/png/", true),
    ;

    private final String directoryPath;
    private final boolean external;

    /**
     * Constructor.
     *
     * @param directoryPath The directory path
     * @param external Indicates if the resource directory is for an external file rather than a resource in the jar
     */
    ResourceDirectory(String directoryPath, boolean external)
    {
        this.directoryPath = directoryPath;
        this.external = external;
    }

    /**
     * @return The directory path. For external files, it should be relative to the installer directory. For jar
     * resources, it should be relative to the root of the platform jar.
     */
    public String getDirectoryPath()
    {
        return directoryPath;
    }

    /**
     * @return True if the resource directory is for an external file; false if it is for a resource in the platform jar.
     * @since 3.3
     */
    public boolean isExternal()
    {
        return external;
    }
}
