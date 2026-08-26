package gov.takx.core.filesystem;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVFSWatchDispatcher;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.filesystem.VfsConstants;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.filesystem.VFSWatchDispatcher;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The file system manager oversees virtual file system objects created for users, plugins, apps, etc.
 * It provides factory methods for creating file systems.
 * File names on physical file systems are escaped to encode characters such as * and ", which
 * are illegal within a Windows file name.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public abstract class VirtualFileSystemManager implements IVirtualFileSystemManager
{
    // Base path/key for finding a device's VFS
    private static final String DEVICE_FILES_NAMESPACE = VfsConstants.FILES_DIR + "/" + VfsConstants.DEVICE_NAMESPACE + "/";

    protected final ConcurrentHashMap<FilePath, VirtualFileSystem> fileSystems = new ConcurrentHashMap<>();
    protected final VFSWatchDispatcher watchDispatcher = new VFSWatchDispatcher();
    protected final VFSScope scope;

    /**
     * Construct a VFS manager for the given scope.
     */
    protected VirtualFileSystemManager(@Nonnull VFSScope scope)
    {
        this.scope = scope;
    }

    @Override
    public VFSScope getVfsScope()
    {
        return scope;
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystemForDevice(@Nonnull IMapObject mapObject)
    {
        return getVirtualFileSystemForDevice(mapObject.getFamily(), mapObject.getType(), mapObject.getUnitID());
    }

    @Override
    public void deleteVirtualFileSystemForDevice(@Nonnull IMapObject mapObject)
    {
        // We may want to make this method more generic to handle other VFS types besides devices, but to
        // limit the impact of the risk given our current time frame I'm only implementing this for devices.

        String uniqueKey = FileUtils.makeSafeName(KeyUtils.getUniqueKey(mapObject.getFamily(), mapObject.getType(), mapObject.getUnitID()));
        final VirtualFileSystem fs = fileSystems.remove(new FilePath(DEVICE_FILES_NAMESPACE, uniqueKey));

        if (fs != null) FileUtils.deleteDirectory(fs.getRootDir());
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystemForDevice(@Nonnull String pluginFamily, @Nonnull String pluginType, @Nonnull String unitId)
    {
        // make a safe unit id name, in case the plugin developer tries to use illegal Windows filename chars.
        // may want to do this in other places too, but we are most vulnerable in the unit id, I think.
        String uniqueKey = FileUtils.makeSafeName(KeyUtils.getUniqueKey(pluginFamily, pluginType, unitId));
        String templateDir = new FilePath(FileUtils.makeSafeName(pluginFamily), FileUtils.makeSafeName(pluginType)).toString();

        return getVirtualFileSystem(VfsConstants.DEVICE_NAMESPACE, uniqueKey, templateDir, pluginFamily);
    }

    @Override
    public VirtualFileSystem getVirtualFileSystemForApp(@Nonnull String pluginFamily, @Nonnull String pluginType)
    {
        String uniqueKey = FileUtils.makeSafeName(KeyUtils.getUniqueKey(pluginFamily, pluginType));

        return getVirtualFileSystem(VfsConstants.APP_NAMESPACE, uniqueKey, uniqueKey, pluginFamily);
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystemForAttachments()
    {
        return getVirtualFileSystem(VfsConstants.ATTACHMENTS_NAMESPACE);
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystemForFamily(@Nonnull String family)
    {
        return getFamilyFileSystem(family);
    }

    @Override
    public VirtualFileSystem getVirtualFileSystemForConnection(@Nonnull String pluginFamily, @Nonnull String pluginType)
    {
        String uniqueKey = FileUtils.makeSafeName(KeyUtils.getUniqueKey(pluginFamily, pluginType));

        return getVirtualFileSystem(VfsConstants.CONNECTION_NAMESPACE, uniqueKey, uniqueKey, pluginFamily);
    }

    @Override
    public VirtualFileSystem getVirtualFileSystemForImportExport(@Nonnull String pluginFamily, @Nonnull String pluginType)
    {
        String uniqueKey = FileUtils.makeSafeName(KeyUtils.getUniqueKey(pluginFamily, pluginType));
        return getVirtualFileSystem(VfsConstants.IMPORT_EXPORT_NAMESPACE, uniqueKey, uniqueKey, pluginFamily);
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem(@Nonnull String namespace)
    {
        return getVirtualFileSystem(namespace, "");
    }

    @Override
    public VirtualFileSystem getVirtualFileSystem(@Nonnull String namespace, @Nonnull String subdirectory)
    {
        // Could be a namespace that was already created so the FILES_DIR would already be included
        final FilePath path = namespace.startsWith(VfsConstants.FILES_DIR) || namespace.startsWith(VfsConstants.FILES_DIR.substring(1))
                ? new FilePath(namespace)
                : new FilePath(VfsConstants.FILES_DIR, namespace);

        if (!subdirectory.isEmpty()) path.append(subdirectory); // Append subdirectory, if any

        return fileSystems.computeIfAbsent(path,
                k -> new VirtualFileSystem(this, path.toString(), path.toSystemPath(getRootDirectory()), null, watchDispatcher));
    }

    /**
     * Get or construct a VFS for the given owner, namespace, and subdirectory.  If the family is specified, then
     * the family VFS and family template VFS are also initialized.
     *
     * @param namespace    VFS namespace
     * @param subdirectory subdirectory within the VFS
     * @param templateDir  Directory for the template file system (often the same as the subdirectory)
     * @param family       Plugin family, {@code null} if no family VFS should be initialized
     * @return VFS instance
     */
    private VirtualFileSystem getVirtualFileSystem(String namespace, String subdirectory, String templateDir, String family)
    {
        final FilePath path = new FilePath(VfsConstants.FILES_DIR, namespace, subdirectory);
        final String rootDir = path.toSystemPath(getRootDirectory());

        // If we need a family VFS, then get/create it now
        final IVirtualFileSystem familyVfs = family == null ? null : getFamilyFileSystem(family);

        return fileSystems.computeIfAbsent(path,
                k -> makeVirtualFileSystem(namespace, templateDir, family, familyVfs, rootDir, path));
    }

    private VirtualFileSystem makeVirtualFileSystem(String namespace, String templateDir, String family,
                                                    IVirtualFileSystem familyVfs, String rootDir, FilePath path)
    {
        final VirtualFileSystem fs = new VirtualFileSystem(this, path.toString(), rootDir,
                TemplateFileSystem.getFileSystem(namespace, templateDir), watchDispatcher);

        if (familyVfs != null)
        {
            // Install the family VFS root and template FS root into the VFS we are creating
            fs.setFamilyRootDirectory(familyVfs.getRootDir());
            fs.setTemplateFamilyRootDirectory(TemplateFileSystem.getFileSystem(null, family).getRootDir());
        }
        return fs;
    }

    /**
     * Creates a VFS directory for the given family name.  This provides access to a 'shared' family namespace
     * wide VFS folder where plugins of the same family name but of a different plugin type can 'share' documents.
     *
     * @param pluginFamily Plugin family
     * @return IVirtualFileSystem for the named family
     */
    private IVirtualFileSystem getFamilyFileSystem(String pluginFamily)
    {
        FilePath path = new FilePath(VfsConstants.FILES_DIR, FileUtils.makeSafeName(pluginFamily));

        return fileSystems.computeIfAbsent(path,
                k -> new VirtualFileSystem(this, path.toString(), path.toSystemPath(getRootDirectory()),
                        TemplateFileSystem.getFileSystem(null, pluginFamily), watchDispatcher));
    }

    @Override
    public File getRoot(@Nonnull String namespace)
    {
        return new File(new FilePath(namespace).toSystemPath(getRootDirectory()));
    }

    @Override
    public File getRoot(@Nonnull IVFSFile file)
    {
        return getRoot(file.getFileSystemNamespace());
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_VirtualFileSystemManager) StartProjectServices event)
    {
        doStartService();
    }

    /**
     * Default service start handler.
     */
    protected void doStartService()
    {
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_VirtualFileSystemManager) StopProjectServices event)
    {
        doStopService();
    }

    /**
     * Default stop service handler.
     */
    protected void doStopService()
    {
        fileSystems.clear();
    }

    @Override
    public IVFSWatchDispatcher getWatchDispatcher()
    {
        return watchDispatcher;
    }
}
