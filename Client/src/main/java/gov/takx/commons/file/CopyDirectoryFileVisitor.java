package gov.takx.commons.file;

import gov.takx.api.platform.IProgressMonitor;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * File visitor that copies one directory to another, omitting any specified files.
 */
public class CopyDirectoryFileVisitor extends SimpleFileVisitor<Path>
{
    /**
     * The source path. This can be inferred from the first call to preVisitDirectory(...).
     */
    private Path sourcePath = null;

    /**
     * The destination directory.
     */
    private final Path targetPath;

    /**
     * List of files to ignore during the copy.
     */
    private final List<Path> filesToIgnore;

    /**
     * Callback object used to report progress.
     */
    private final IProgressMonitor progressMonitor;

    /**
     * Constructor.
     *
     * @param targetPath      The target location.
     * @param progressMonitor Object used for callbacks to update copy progress. May be {@code null}.
     * @param filesToIgnore   Files that should not be copied to the new location.
     */
    public CopyDirectoryFileVisitor(Path targetPath, IProgressMonitor progressMonitor, List<Path> filesToIgnore)
    {
        this.targetPath = targetPath;
        this.progressMonitor = progressMonitor;
        this.filesToIgnore = new ArrayList<>(filesToIgnore); // We're going to modify the list, so make a copy.
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
    {
        // Infer the source path from the first Files.copy(...) call.
        if (sourcePath == null)
        {
            sourcePath = dir;

            if (progressMonitor != null)
            {
                progressMonitor.setMaximum(FileUtils.countFiles(sourcePath.toFile()));
            }
        }

        Path target = targetPath.resolve(sourcePath.relativize(dir));

        Files.createDirectories(target);

        if (progressMonitor != null)
        {
            progressMonitor.increment();
        }

        // Postgres will choke if the permissions are too lax, so copy them over too.
        if (!SystemUtils.IS_OS_WINDOWS)
        {
            Files.setPosixFilePermissions(target, Files.getPosixFilePermissions(dir));
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
    {
        if (!ignoreFile(file))
        {
            Path target = targetPath.resolve(sourcePath.relativize(file));
            Set<PosixFilePermission> filePermissions = null;

            // Postgres will choke if the permissions are too lax, so copy them if we're on a Unix-based system.
            // We need to get them now because some temp files may not exist after the copy.
            if (!SystemUtils.IS_OS_WINDOWS)
            {
                filePermissions = Files.getPosixFilePermissions(file);
            }

            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

            // Write the permissions to the new file. Keeps Postgres happy as noted above.
            if (filePermissions != null)
            {
                Files.setPosixFilePermissions(target, filePermissions);
            }

            if (progressMonitor != null)
            {
                progressMonitor.increment();
            }
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * Check to see if the file should be ignored.
     *
     * @param file The file path.
     * @return {@code true} iff the files should be ignored during the copy procedure.
     */
    private boolean ignoreFile(Path file)
    {
        // Since we're using full paths, each file to be ignored can
        // only qualify once and we don't need to check the same file again.

        return filesToIgnore.remove(file);
    }
}
