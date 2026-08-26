package gov.takx.api.filesystem;

import gov.takx.commons.file.FilePath;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @since 1.3
 */
public interface ITemplateFileSystem
{
    /**
     * Get the root directory of the virtual file system master template directory. You can use this directory to
     * manually store and manage template files that will be copied into the VFS of a map object upon its instantiation
     * in a project. Files that you save into this directory will be <b>not</b> be packaged with the project because
     * they are considered part of the system, not part of a project.
     *
     * @return Root directory of the virtual file system master template directory.
     */
    File getRootDir();

    /**
     * Get a reference to a file in the virtual file system master template directory.
     *
     * @param file Path to the file.
     * @return File object, or {@code null} if no file is found.
     */
    @Nullable
    IVFSFile getFile(FilePath file);

    /**
     * Save a file to the virtual file system master template directory.
     *
     * @param file File to save.
     * @return Object that represents the new file.
     * @throws java.io.IOException If the file cannot be saved to the virtual file system.
     */
    IVFSFile saveFile(File file) throws IOException;

    /**
     * Save a file the specified path in the virtual file system master template directory.
     *
     * @param target The location in the virtual file system master template directory for the new file.
     * @param source File to save.
     * @return Object that represents the new file.
     * @throws IOException If the file cannot be saved to the virtual file system.
     */
    IVFSFile saveFile(FilePath target, File source) throws IOException;

    /**
     * Delete a file from the virtual file system.
     *
     * @param file File to delete.
     */
    void deleteFile(IVFSFile file);

    /**
     * Existence check on the given file.
     *
     * @param file File to check for existence
     * @return {@code true}, if file exists
     */
    boolean fileExists(FilePath file);

    /**
     * Get a list of files in the virtual file system.
     *
     * @return List of files.
     */
    List<IVFSFile> listFiles();
}
