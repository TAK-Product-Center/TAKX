package gov.takx.api.filesystem;

/**
 * The file system manager oversees virtual file systems created for users and plugins, apps. This interface
 * adds functionality specific to the TAKX client to the more general {@link IVirtualFileSystemService}.
 *
 */
public interface IVirtualFileSystemManager extends IVirtualFileSystemService
{
    /**
     * @return VFSScope managed by this VFS Manager
     * @since 3.0
     */
    VFSScope getVfsScope();
}
