package gov.takx.api.filesystem;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.IFileProgress;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface to a virtual file system. Each map object has a private virtual file system into which it can place files.
 * These files will be packaged with the project.
 * <p/>
 * <h3>Creating a File Map Object</h3>
 * <p/>
 * Use {@link #createFileMapObject} to create a file map object. The new file will appear in the explorer,
 * and (if given a location) on the globe. This method will return a {@link IFileMapObject}, which
 * you can use to manipulate the file and set its location.
 * <p/>
 * IFileMapObject are tracked by TAKX and should only be deleted using {@link #deleteFile(IVFSFile)}.
 * <p/>
 * For example, to create a file called "myfile.txt" as a file map object:
 * <p/>
 * <pre>
 *   IFileMapObject file = fileSystem.createFileMapObject(new FilePath("myfile.txt"));
 *   file.setLocation(new RaptorLocation(myLatitude, myLongitude));
 *
 *   FileWriter f = new FileWriter(file.getFile());
 *   f.write("Hello world");
 *   f.close();
 * </pre>
 * <p/>
 * <h3>Creating a private file</h3>
 * <p/>
 * If you want to create a private file in your Virtual File System you can get a reference to the VFS root
 * with {@link #getRootDir()} will return the root directory as a {@code java.io.File} object. A plugin is free
 * to store and manipulate files in this directory. Any files saved here will be packaged with the TAKX project.
 * <p/>
 * For example, to create a file called "myfile.txt" as a private file:
 * <p/>
 * <pre>
 *   File myfile = new java.io.File(fileSystem.getRootDir(), "myfile.txt");
 *   FileWriter f = new FileWriter(myfile);
 *   f.write("Hello world");
 *   myfile.close();
 * </pre>
 * <p/>
 * Files created in this way are not tracked by TAKX, and are not visible on the globe.
 *
 * @see IVFSFile
 * @see IFileMapObject
 * @since 1.3
 */
@SuppressWarnings("unused")
public interface IVirtualFileSystem
{
    /**
     * Get the namespace for this VFS. This can be used to determine the underlying root.
     *
     * @return the namespace
     */
    String getNamespace();

    /**
     * @return VFS Manager for this VFS
     * @since 3.0
     */
    IVirtualFileSystemManager getVfsManager();

    /**
     * Get the root directory of the virtual file system for the plugin instance. You can use this directory to manually
     * store and manage files that you do not want to be turned into map objects. Files that you save into this
     * directory will be packaged with the project.
     *
     * @return Root directory of the virtual file system at the plugin instance level.
     */
    File getRootDir();

    /**
     * Get the root directory of the virtual file system at the family level.  This level is used to 'share' files
     * among plugin types that belong to the same family namespace even when they are of a disparate plugin type,
     * e.g. device, connection, import-export, etc.
     *
     * @return Root directory of the virtual file system at the family level.
     */
    File getFamilyRootDir();

    /**
     * Get the root directory where template files are installed from the plugin by TAKX at the plugin level.
     * This directory can be used to modify template files such as config files.
     *
     * @return Root directory of the template file system at the plugin instance level.
     * @since 1.5
     */
    File getTemplateRootDir();

    /**
     * Get the root directory where template files are installed from the plugin by TAKX at the family level.
     * This directory can be used to modify family level template files such as config files.
     *
     * @return Root directory of the template file system at the family level.
     * @since 1.5
     */
    File getTemplateFamilyRootDir();

    /**
     * Add an existing file to the virtual file system.
     *
     * @param path Path to the file.
     * @return A file.
     * @throws java.io.IOException If the file cannot be added.
     * @since 1.15
     */
    IVFSFile addFile(@Nonnull FilePath path) throws IOException;

    /**
     * Create a new file in the virtual file system.
     *
     * @param path Path to the file.
     * @return A new file.
     * @throws java.io.IOException If the file cannot be created.
     * @since 1.5
     */
    IVFSFile createFile(@Nonnull FilePath path) throws IOException;

    /**
     * Delete a file from the virtual file system.
     *
     * @param file File to delete.
     * @return true if the file on disk is deleted successfully, or false if the file cannot be deleted. The file will
     * be removed from VFS in-memory tracking even if it cannot be deleted on disk.
     */
    boolean deleteFile(@Nonnull IVFSFile file);

    /**
     * Rename a file within the virtual file system.
     *
     * @param file    File to rename.
     * @param newName New name.
     * @return true if the file is renamed successfully, or false if the file cannot be renamed.
     */
    boolean renameFile(@Nonnull IVFSFile file, @Nonnull FilePath newName);

    /**
     * Does a file exist at a given path?
     *
     * @param file Path to test.
     * @return True if a file exists at {@code file}.
     */
    boolean fileExists(FilePath file);

    /**
     * @return List of files in the virtual file system.
     */
    List<? extends IVFSFile> listFiles();

    /**
     * Get a list of file map objects in the virtual file system.
     *
     * @return List of file map objects.
     */
    List<IFileMapObject> listFileMapObjects();

    /**
     * Get a reference to a file in the virtual file system.
     *
     * @param file Path to the file.
     * @return File object, or {@code null} if no file is found.
     */
    @Nullable
    IVFSFile getFile(@Nonnull FilePath file);

    /**
     * Get a reference to a file map object in the virtual file system.
     *
     * @param file Path to the file.
     * @return File map object, or {@code null} if no file map object is found.
     */
    @Nullable
    IFileMapObject getFileMapObject(@Nonnull FilePath file);

    /**
     * Attempt to determine the location of a geocoded file (for example, a geocoded JPEG photo).
     *
     * @param file File to decode.
     * @return Location of file, or {@code null} if the location could not be determined.
     */
    @Nullable
    ILocation geoDecode(@Nonnull File file);

    /**
     * Create a file map object at the specified path in the virtual file system. This method will copy file
     * {@code source} into the Virtual File System, and create a map object.
     *
     * @param target The location in the virtual file system for the new file.
     * @param source File to save.
     * @return Object that represents the new file.
     * @throws IOException If the file cannot be saved to the virtual file system.
     */
    IFileMapObject createFileMapObject(@Nonnull FilePath target, File source) throws IOException;

    /**
     * Create a file map object at the specified path in the virtual file system, and set the file location. This
     * method will copy file file to the VFS, create a map object, and an icon on the globe if provided.
     *
     * @param target   The location in the virtual file system for the new file.
     * @param source   File to save.
     * @param location Location of the file.
     * @return Object that represents the new file.
     * @throws IOException If the file cannot be saved to the virtual file system.
     */
    IFileMapObject createFileMapObject(@Nonnull FilePath target, File source, @Nullable ILocation location) throws IOException;

    /**
     * Create a file map object at the specified path in the virtual file system, with a progress callback. This
     * method will create a map object. The {@code progress} object can be used to receive updates on the
     * progress of the file copy. This can be used to implement a visual progress indicator.
     *
     * @param target   The location in the virtual file system for the new file.
     * @param source   File to save.
     * @param progress Object to receive progress callbacks, in order display progress to the user as a
     *                 large file is imported.
     * @return Object that represents the new file.
     * @throws IOException If the file cannot be saved to the virtual file system.
     */
    IFileMapObject createFileMapObject(@Nonnull FilePath target, File source, IFileProgress progress) throws IOException;

    /**
     * Create a file map object at the specified path in the virtual file system and set its location, with a
     * progress callback. This method will copy file file to the VFS, create a map object. The {@code progress} object
     * can be used to receive updates on the progress of the file copy. This can be used to implement a visual
     * progress indicator.
     *
     * @param target   The location in the virtual file system for the new file.
     * @param source   File to save.
     * @param location Location of the file.
     * @param progress Object to receive progress callbacks, in order display progress to the user as a
     *                 large file is imported.
     * @return Object that represents the new file.
     * @throws IOException If the file cannot be saved to the virtual file system.
     */
    IFileMapObject createFileMapObject(@Nonnull FilePath target, File source, @Nullable ILocation location,
                                       @Nullable IFileProgress progress)
            throws IOException;

    /**
     * Copy a file out of the virtual file system to another location on the disk.
     *
     * @param sourceFile Path in the virtual file system to the file to copy.
     * @param targetFile Location to copy to.
     * @throws IOException If file cannot be copied.
     */
    void exportFile(@Nonnull FilePath sourceFile, @Nonnull File targetFile) throws IOException;

    /**
     * Copy a file out of the virtual file system to another location on the disk, with a progress callback. The
     * progress callback can be used to implement a progress indicator.
     *
     * @param sourceFile Path in the virtual file system to the file to copy.
     * @param targetFile Location to copy to.
     * @param progress   Progress monitor to receive callbacks.
     * @throws IOException If file cannot be copied.
     */
    void exportFile(@Nonnull FilePath sourceFile, @Nonnull File targetFile,
                    @Nullable IFileProgress progress)
            throws IOException;

    /**
     * Copy a file out of the virtual file system to another location on the disk.
     *
     * @param sourceFile File to copy.
     * @param targetFile Location to copy to.
     * @throws IOException If file cannot be copied.
     */
    void exportFile(@Nonnull IVFSFile sourceFile, @Nonnull File targetFile) throws IOException;

    /**
     * Copy a file out of the virtual file system to another location on the disk, with a progress callback. The
     * progress callback can be used to implement a progress indicator.
     *
     * @param sourceFile Path in the virtual file system to the file to copy.
     * @param targetFile Location to copy to.
     * @param progress   Progress monitor to receive callbacks.
     * @throws IOException If file cannot be copied.
     */
    void exportFile(@Nonnull IVFSFile sourceFile, @Nonnull File targetFile, IFileProgress progress) throws IOException;

    /**
     * Add a watcher that will be notified when files are added or deleted from the virtual file system.
     *
     * @param watcher New watcher.
     * @see #removeFileWatcher(IVirtualFileSystemWatcher)
     */
    void addFileWatcher(@Nonnull IVirtualFileSystemWatcher watcher);

    /**
     * Remove a file system watcher.
     *
     * @param watcher Watcher to remove.
     * @see #addFileWatcher(IVirtualFileSystemWatcher)
     */
    void removeFileWatcher(@Nonnull IVirtualFileSystemWatcher watcher);
}
