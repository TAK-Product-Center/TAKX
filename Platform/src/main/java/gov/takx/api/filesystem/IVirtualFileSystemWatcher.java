package gov.takx.api.filesystem;

import gov.takx.commons.file.FilePath;

/**
 * Interface for watching a Virtual File System for changes. The watcher will receive notification as files are added
 * or deleted from the VFS.
 * <p/>
 * The watcher only receives notifications about files that are added via {@link IVirtualFileSystem#addFile(FilePath)}
 * of {@link IVirtualFileSystem#createFile(FilePath)}, or deleted using {@link IVirtualFileSystem#deleteFile(IVFSFile)}.
 *
 * @see IVirtualFileSystem
 * @since 1.3
 */
public interface IVirtualFileSystemWatcher
{
    /**
     * Invoked when a file is created in the Virtual File System.
     *
     * @param file The new file.
     */
    void onFileAdded(IVFSFile file);

    /**
     * Invoked when a file is removed from the Virtual File System.
     *
     * @param file File that was removed.
     */
    void onFileRemoved(IVFSFile file);
}
