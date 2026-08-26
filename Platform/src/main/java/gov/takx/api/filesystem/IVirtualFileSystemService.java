package gov.takx.api.filesystem;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * The file system service creates virtual file system objects for users, plugins, apps, etc. It provides
 * factory methods for creating file systems.
 *
 */
@NoPluginImplementations
public interface IVirtualFileSystemService
{
    /**
     * Delete a file system for a device plugin.
     *
     * @param mapObject The map object that wants a file system. May not be {@code null}.
     * @since 1.14
     */
    void deleteVirtualFileSystemForDevice(@Nonnull IMapObject mapObject);

    /**
     * Create a file system for a device plugin. This virtual file system will be part of the project, and unique for
     * this map object family/type/unit_id.
     *
     * @param mapObject The map object that wants a file system. May not be {@code null}.
     * @return A virtual file system for the map object.
     */
    IVirtualFileSystem getVirtualFileSystemForDevice(@Nonnull IMapObject mapObject);

    /**
     * Create a file system for a plugin. This virtual file system will be part of the project, and unique for
     * this map object family/type/unit_id.
     *
     * @param pluginFamily The family of device that wants a file system. May not be {@code null}.
     * @param pluginType   The type of device that wants a file system. May not be {@code null}.
     * @param unitId       The unit ID of the device that wants a file system. May not be {@code null}.
     * @return A file system object.
     */
    IVirtualFileSystem getVirtualFileSystemForDevice(@Nonnull String pluginFamily, @Nonnull String pluginType, @Nonnull String unitId);

    /**
     * Create a file system for an app. This virtual file system will be part of the project, and unique to this app.
     *
     * @param family Family name.  Must not be {@code null}.
     * @param type   Type name.  Must not be {@code null}.
     * @return A file system object.
     */
    IVirtualFileSystem getVirtualFileSystemForApp(@Nonnull String family, @Nonnull String type);

    /**
     * Create a file system for a connection plugin. This virtual file system will be part of the project, and unique
     * for this connection family/name.
     *
     * @param family Family name.  Must not be {@code null}.
     * @param type   Type name.  Must not be {@code null}.
     * @return A file system object.
     */
    IVirtualFileSystem getVirtualFileSystemForConnection(@Nonnull String family, @Nonnull String type);

    /**
     * Create a file system for an Import/Export plugin. This virtual file system will be part of the project, and unique
     * for this Import/Export plugin family/name.
     *
     * @param family Family name.  Must not be {@code null}.
     * @param type   Type name.  Must not be {@code null}.
     * @return A file system object.
     */
    IVirtualFileSystem getVirtualFileSystemForImportExport(@Nonnull String family, @Nonnull String type);

    /**
     * Create a file system shared among a family. This virtual file system will be part of the project, and unique
     * for this family.
     *
     * @param family Family name.  Must not be {@code null}.
     * @return A file system object.
     * @since 1.14
     */
    IVirtualFileSystem getVirtualFileSystemForFamily(@Nonnull String family);

    /**
     * Get the file system for attached files.
     *
     * @return A file system rooted at the location to place attached files
     * @since 3.4
     */
    IVirtualFileSystem getVirtualFileSystemForAttachments();

    /**
     * Get the file system for a namespace. The file system will be created if it did not already exist.
     *
     * @param namespace Namespace of this file system
     * @return File system for namespace
     * @since 3.4
     */
    IVirtualFileSystem getVirtualFileSystem(@Nonnull String namespace);

    /**
     * Get the file system for a namespace and subdirectory.
     *
     * @param namespace    Namespace of this file system.
     * @param subdirectory Subdirectory to access under the file system root, may be empty
     * @return File system for namespace. The file system will be created if it did not already exist.
     */
    IVirtualFileSystem getVirtualFileSystem(@Nonnull String namespace, @Nonnull String subdirectory);

    /**
     * @return The root of the VFS for this VFS' scope.
     * @since 3.4
     */
    File getRootDirectory();

    /**
     * Get the root directory for a namespace.
     *
     * @param namespace Namespace to find root for.
     * @return Return the root directory for the namespace.
     */
    File getRoot(@Nonnull String namespace);

    /**
     * Get the root directory of the VFS namespace that holds a file.
     *
     * @param file File to find root for.
     * @return Return the root directory for the file's namespace.
     */
    File getRoot(@Nonnull IVFSFile file);

    IVFSWatchDispatcher getWatchDispatcher();
}
