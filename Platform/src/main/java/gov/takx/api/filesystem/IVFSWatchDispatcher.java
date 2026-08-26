package gov.takx.api.filesystem;

/**
 * Utility class for managing Virtual File System watchers. Each watcher is watching a certain namespace
 * in the VFS. The IVFSWatchDispatcher manages the set of watchers, and provides methods for firing events.
 * <p/>
 * Proprietary information subject to terms of a Non-disclosure agreement
 *
 * @since 1.5
 */
public interface IVFSWatchDispatcher
{
    /**
     * Add a VFS watcher to the set of watchers that this object manages.
     *
     * @param namespace The namespace being watched.
     * @param watcher   The watcher object. A watcher can watch more than one namespace.
     */
    void addWatcher(String namespace, IVirtualFileSystemWatcher watcher);

    /**
     * Remove a watcher.
     *
     * @param namespace The namespace being watched.
     * @param watcher   The watcher to remove.
     */
    void removeWatcher(String namespace, IVirtualFileSystemWatcher watcher);

    /**
     * Fire a file added event.
     *
     * @param namespace The namespace to which the file was added.
     * @param file      The new file.
     */
    void fireFileAdded(String namespace, IVFSFile file);

    /**
     * Fire a file removed event.
     *
     * @param namespace The namespace of the file that was removed.
     * @param file      The file that was removed.
     */
    void fireFileRemoved(String namespace, IVFSFile file);
}
