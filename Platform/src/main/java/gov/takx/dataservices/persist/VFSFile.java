package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Representation of a file in the Virtual File System.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Internal
@PseudoBean
public class VFSFile implements IVFSFile
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    private final VFSScope vfsScope;
    private final String namespace;
    private String relativePath;

    private File file;
    protected IFileMapObject fileMapObject;

    private transient boolean interned = false; // Only try to locate our FileMapObject once

    public VFSFile(IVirtualFileSystemManager vfsManager, String namespace, String relativePath)
    {
        this.namespace = namespace;
        vfsScope = vfsManager.getVfsScope();

        final File rootDir = vfsManager.getRoot(namespace);

        setFile(new File(rootDir, relativePath), relativePath);
    }

    public void setFile(File file, String relativePath)
    {
        logger.debug("Setting file to {}", file);
        this.file = file;
        this.relativePath = relativePath;
    }

    @Override
    public File getFile()
    {
        return file;
    }

    @Override
    public IFileMapObject getFileMapObject()
    {
        if (fileMapObject == null && !interned)
        {
            // In order to find the MO, we need to find the correct VFS
            final IVirtualFileSystem vfs = projectVFSManager.getVirtualFileSystem(namespace);

            fileMapObject = vfs.getFileMapObject(new FilePath(relativePath));
            interned = true;
        }
        return fileMapObject;
    }

    @Override
    public boolean canExecute()
    {
        return file.canExecute();
    }

    @Override
    public boolean canRead()
    {
        return file.canRead();
    }

    @Override
    public boolean canWrite()
    {
        return file.canWrite();
    }

    @Override
    public int compareTo(File pathname)
    {
        return file.compareTo(pathname);
    }

    @Override
    public int compareTo(IVFSFile file)
    {
        return getPath().compareTo(file.getPath());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VFSFile vfsFile = (VFSFile) o;

        if (vfsScope != vfsFile.vfsScope) return false;
        if (!relativePath.equals(vfsFile.relativePath)) return false;
        return namespace.equals(vfsFile.namespace);
    }

    @Override
    public int hashCode()
    {
        int result = vfsScope.hashCode();
        result = 31 * result + relativePath.hashCode();
        result = 31 * result + namespace.hashCode();
        return result;
    }

    @Override
    public boolean exists()
    {
        return file.exists();
    }

    @Override
    public String getFileName()
    {
        return file.getName();
    }

    @Override
    public FilePath getParentFilePath()
    {
        return getFilePath().getParent();
    }

    @Override
    public String getPath()
    {
        return relativePath;
    }

    @Override
    public FilePath getFilePath()
    {
        return new FilePath(relativePath);
    }

    @Override
    public String getAbsolutePath()
    {
        return file.getAbsolutePath();
    }

    @Override
    public boolean isDirectory()
    {
        return file.isDirectory();
    }

    @Override
    public boolean isFile()
    {
        return file.isFile();
    }

    @Override
    public boolean isHidden()
    {
        return file.isHidden();
    }

    @Override
    public long lastModified()
    {
        return file.lastModified();
    }

    @Override
    public boolean setExecutable(boolean executable)
    {
        return file.setExecutable(executable);
    }

    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly)
    {
        return file.setExecutable(executable, ownerOnly);
    }

    @Override
    public boolean setLastModified(long time)
    {
        return file.setLastModified(time);
    }

    @Override
    public boolean setReadable(boolean readable)
    {
        return file.setReadable(readable);
    }

    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly)
    {
        return file.setReadable(readable, ownerOnly);
    }

    @Override
    public boolean setReadOnly()
    {
        return file.setReadOnly();
    }

    @Override
    public boolean setWritable(boolean writable)
    {
        return file.setWritable(writable);
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly)
    {
        return file.setWritable(writable, ownerOnly);
    }

    @Override
    public String toString()
    {
        return relativePath;
    }

    @Override
    public String getFileSystemNamespace()
    {
        return namespace;
    }

    @Override
    public long length()
    {
        return file.length();
    }

    @Override
    public Icon getSystemIcon()
    {
        return FileUtils.getSystemIcon(file);
    }

    @Override
    public long getCreationDate()
    {
        return FileUtils.getFileCreationTime(file);
    }
}
