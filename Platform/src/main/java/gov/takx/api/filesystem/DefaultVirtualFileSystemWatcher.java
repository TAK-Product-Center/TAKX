package gov.takx.api.filesystem;

/**
 * Empty implementation of a file system watcher.
 *
 */
public class DefaultVirtualFileSystemWatcher implements IVirtualFileSystemWatcher
{
    /**
     * Does nothing.
     */
    @Override
    public void onFileAdded(IVFSFile file)
    {
    }

    /**
     * Does nothing.
     */
    @Override
    public void onFileRemoved(IVFSFile file)
    {
    }
}
