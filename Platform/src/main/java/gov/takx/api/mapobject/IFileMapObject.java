package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.commons.file.FilePath;

/**
 */
@NoPluginImplementations
public interface IFileMapObject extends IMapObject, IPreviewProvider
{
    String FILE_TYPE = "GeoFile";

    /**
     * Get the namespace that this file belongs to. Each virtual file system (for a device plugin, for example) has
     * it's own namespace.
     *
     * @return The file system namespace.
     * @since 3.0 (actually earlier, but interfaces got rearranged)
     */
    String getFileSystemNamespace();

    /**
     * @return The VFS that contains the physical file
     * @since 3.0
     */
    IVirtualFileSystem getVfs();

    /**
     * @return Underlying VFS file
     * @since 3.0
     */
    IVFSFile getVfsFile();

    /**
     * @return VFS file's file path, if a VFS file has been associated with the file map object, {@code null} otherwise
     * @see IVFSFile#getFilePath()
     * @since 3.0
     */
    FilePath getFilePath();

    /**
     * Set the location of the file. If the file contains geo-reference data, this location will be overridden. This
     * method changes only the metadata that TAKX maintains about the file. It will not alter the file contents.
     *
     * @param location The new location, or {@code null} to set no location.
     */
    void setLocation(ILocation location);
}
