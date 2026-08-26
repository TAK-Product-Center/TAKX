package gov.takx.dataservices.filesystem;

import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVFSWatchDispatcher;
import gov.takx.api.filesystem.IVirtualFileSystemWatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Utility class for managing Virtual File System watchers. Each watcher is watching a certain namespace
 * in the VFS. The IVFSWatchDispatcher manages the set of watchers, and provides methods for firing events.
 * <p>
 * Proprietary information subject to terms of a Non-disclosure agreement
 */
public class VFSWatchDispatcher implements IVFSWatchDispatcher
{
    private final Map<String, List<IVirtualFileSystemWatcher>> watcherMap = new ConcurrentHashMap<>();

    @Override
    public void addWatcher(String namespace, IVirtualFileSystemWatcher watcher)
    {
        if (namespace == null) throw new IllegalArgumentException("Parameter namespace may not be null");
        if (watcher == null) throw new IllegalArgumentException("Parameter watcher may not be null");

        List<IVirtualFileSystemWatcher> watchers = watcherMap.get(namespace);
        if (watchers == null)
        {
            watchers = new CopyOnWriteArrayList<>();
            watcherMap.put(namespace, watchers);
        }

        watchers.add(watcher);
    }

    @Override
    public void removeWatcher(String namespace, IVirtualFileSystemWatcher watcher)
    {
        if (namespace == null) throw new IllegalArgumentException("Parameter namespace may not be null");
        if (watcher == null) throw new IllegalArgumentException("Parameter watcher may not be null");

        List<IVirtualFileSystemWatcher> watchers = watcherMap.get(namespace);
        if (watchers != null)
        {
            watchers.remove(watcher);
        }
    }

    @Override
    public void fireFileAdded(String namespace, IVFSFile file)
    {
        fireEvent(namespace, file, (watcher, vfsFile) -> watcher.onFileAdded(file));
    }

    @Override
    public void fireFileRemoved(String namespace, IVFSFile file)
    {
        fireEvent(namespace, file, (watcher, vfsFile) -> watcher.onFileRemoved(file));
    }

    private void fireEvent(String namespace, IVFSFile file, BiConsumer<IVirtualFileSystemWatcher, IVFSFile> callback)
    {
        List<IVirtualFileSystemWatcher> watchers = watcherMap.get(namespace);
        if (watchers != null)
        {
            for (IVirtualFileSystemWatcher watcher : watchers)
            {
                callback.accept(watcher, file);
            }
        }
    }
}
