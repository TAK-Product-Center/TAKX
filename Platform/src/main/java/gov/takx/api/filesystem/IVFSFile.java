package gov.takx.api.filesystem;

import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.commons.file.FilePath;

import javax.swing.Icon;

import java.io.File;

/**
 * A file in the TAKX Virtual File System. IVFSFiles are tracked by TAKX.
 * <p/>
 * This interface mostly matches the interface of java.io.File, so plugin code can manipulate objects with the familiar
 * File interface. If you need to get a reference to the actual java.io.File object, use {@link #getFile()}. Note that
 * methods such as {@code delete}, {@code move}, and {@code rename} are absent from this interface. While nothing
 * prevents you from getting a reference to the java.io.File that backs the IVFSFile and deleting or moving it,
 * you should do this through IVirtualFileSystem. Deleting the file directly will cause problems.
 * <p/>
 *
 * @see IVirtualFileSystem
 * @since 1.3
 */
public interface IVFSFile
{
    /**
     * @return The FileMapObject associated with this VFS file, if any. May be {@code null}.
     */
    IFileMapObject getFileMapObject();

    /**
     * Get the java.io.File object that backs this object. You may read and write from the file, but do not delete
     * or move it. These operations should be done through IVirtualFileSystem.
     *
     * @return The File object
     * @see IVirtualFileSystem
     */
    File getFile();

    /**
     * Get the namespace that this file belongs to. Each virtual file system (for a device plugin, for example) has
     * it's own namespace.
     *
     * @return The file system namespace.
     * @since 1.5
     */
    String getFileSystemNamespace();

    /**
     * Get the file size, in bytes.
     *
     * @return file size, measured in bytes.
     */
    long length();

    /**
     * Tests whether the application can execute the file denoted by
     * this abstract pathname.
     */
    boolean canExecute();

    /**
     * Tests whether the application can read the file denoted by this abstract pathname.
     */
    boolean canRead();

    /**
     * Tests whether the application can modify the file denoted by
     * this abstract pathname.
     */
    boolean canWrite();

    /**
     * Compares two abstract path names lexicographically.
     */
    int compareTo(File pathname);

    /**
     * Compares two VFS files. The files are considered equal if their location in the Virtual File System is the same.
     *
     * @param file File to compare with.
     * @return True if the files have lexicographically equal same file paths. The comparison is case sensitive.
     */
    int compareTo(IVFSFile file);

    /**
     * Tests this abstract pathname for equality with the given
     * object.
     */
    boolean equals(Object obj);

    /**
     * Tests whether the file or directory denoted by this abstract
     * pathname exists.
     */
    boolean exists();

    /**
     * Returns the name of the file or directory denoted by this
     * abstract pathname.
     */
    String getFileName();

    /**
     * Returns the abstract pathname of this abstract pathname's
     * parent, or {@code null} if this pathname does not name a parent
     * directory.
     *
     * @return A path relative to the VFS root of the file's parent.
     */
    FilePath getParentFilePath();

    /**
     * Converts this abstract pathname into a pathname string.
     */
    String getPath();

    /**
     * Converts the abstract pathname into a fully qualified (relative) FilePath
     *
     * @return FilePath containing the fully qualified VFS path (relative to the VFS root)
     */
    FilePath getFilePath();

    /**
     * Get the absolute path to the file on disk. This path is not relative to the Virtual File System root, and
     * is not portable. <b>Plugins should generally not use this method. Do not assume that the file is ready for use.
     * The file on disk may represent a remote file that needs to be downloaded before use.</b> Use {@link #getFile()}
     * to safely obtain a reference to the underlying file.
     *
     * @return Absolute path to the file on disk.
     * @see #getFile()
     */
    String getAbsolutePath();

    /**
     * Tests whether the file denoted by this abstract pathname is a
     * directory.
     */
    boolean isDirectory();

    /**
     * Tests whether the file denoted by this abstract pathname is a
     * normal file.
     */
    boolean isFile();

    /**
     * Tests whether the file named by this abstract pathname is a
     * hidden file.
     */
    boolean isHidden();

    /**
     * Returns the time that the file denoted by this abstract
     * pathname was last modified.
     */
    long lastModified();

    /**
     * Returns the date that the file was created.
     *
     * @return File creation date.
     */
    long getCreationDate();

    /**
     * A convenience method to set the owner's execute permission for
     * this abstract pathname.
     *
     * @param executable
     * @return {@code true} if and only if the operation succeeded.
     */
    boolean setExecutable(boolean executable);

    /**
     * Sets the owner's or everybody's execute permission for this
     * abstract pathname.
     */
    boolean setExecutable(boolean executable, boolean ownerOnly);

    /**
     * Sets the last-modified time of the file or directory named by
     * this abstract pathname.
     */
    boolean setLastModified(long time);

    /**
     * Sets the owner's or everybody's read permission for this
     * abstract pathname.
     */
    boolean setReadable(boolean readable);

    /**
     * Sets the owner's or everybody's read permission for this
     * abstract pathname.
     */
    boolean setReadable(boolean readable, boolean ownerOnly);

    /**
     * Marks the file or directory named by this abstract pathname so
     * that only read operations are allowed.
     */
    boolean setReadOnly();

    /**
     * A convenience method to set the owner's write permission for
     * this abstract pathname.
     */
    boolean setWritable(boolean writable);

    /**
     * Sets the owner's or everybody's write permission for this
     * abstract pathname.
     */
    boolean setWritable(boolean writable, boolean ownerOnly);

    /**
     * Get the operating system's icon for this type of file. The behavior of this method is depends on the
     * operating system.
     *
     * @return An icon for this type of file.
     */
    Icon getSystemIcon();
}
