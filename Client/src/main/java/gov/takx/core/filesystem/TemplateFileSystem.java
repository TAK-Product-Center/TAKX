package gov.takx.core.filesystem;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.ITemplateFileSystem;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 1.3
 */
public class TemplateFileSystem implements ITemplateFileSystem
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final File TEMPLATES_ROOT_DIR = new GlobalVfsAccessor().getTemplatesRootDir();

    private final File rootDir;

    private TemplateFileSystem(File rootDirectory)
    {
        rootDir = rootDirectory;

        if (!rootDir.exists() && !rootDir.mkdirs()) logger.error("Failed to create Template root directory: {}", rootDir);
    }

    @Override
    public File getRootDir()
    {
        return rootDir;
    }

    @Override
    public IVFSFile getFile(FilePath file)
    {
        return new TemplateFile(file.toSystemPath(rootDir));
    }

    @Override
    public IVFSFile saveFile(File file) throws IOException
    {
        return saveFile(new FilePath(file.getName()), file);
    }

    @Override
    public IVFSFile saveFile(FilePath target, File source) throws IOException
    {
        final File newFile = new File(rootDir, target.toString());

        logger.debug("Copying {} to {}", source, newFile);

        FileUtils.copy(source, newFile);

        return getFile(target);
    }

    @Override
    public void deleteFile(IVFSFile file)
    {
        logger.debug("Deleting file {}", file);

        final File f = (File) file;
        if (!f.delete()) logger.warn("Failed to delete VFS file: {}", f);
    }

    @Override
    public boolean fileExists(FilePath file)
    {
        File f = new TemplateFile(file.toSystemPath(rootDir));
        boolean exists = f.exists();

        logger.debug("Existence check of file {}: {}", f, exists);

        return exists;
    }

    @Override
    public List<IVFSFile> listFiles()
    {
        final File[] list = rootDir.listFiles();

        return list != null
                ? Arrays.stream(list).map(f -> new TemplateFile(f.getAbsolutePath())).collect(Collectors.toList())
                : Collections.emptyList();
    }

    /**
     * Create a file system for a given namespace and unique key. This virtual file system will be part the Global VFS.
     *
     * @param namespace The namespace of the FS to create (APP, DATABASE, etc.), {@code null} means family dir
     * @param uniqueKey The unique key for the FS within the namespace. May not be {@code null}
     * @return A file system object
     */
    public static TemplateFileSystem getFileSystem(@Nullable String namespace, @Nonnull String uniqueKey)
    {
        // If we didn't get a namespace, then this is a family VFS and it just uses the key directly
        final FilePath path = namespace == null
                ? new FilePath(FileUtils.makeSafeName(uniqueKey))
                : new FilePath(namespace, FileUtils.makeSafeName(uniqueKey));

        final File systemDir = new File(TEMPLATES_ROOT_DIR, path.toString());

        if (!systemDir.exists() && !systemDir.mkdirs()) logger.error("Failed to create template directory: {}", systemDir);

        if (logger.isDebugEnabled())
        {
            final String type = (namespace == null) ? "family" : namespace;
            logger.debug("Creating {} template file system at {}", type, systemDir.getAbsolutePath());
        }

        return new TemplateFileSystem(systemDir);
    }

    /**
     * Simple mechanism to access the global VFS manager from a static context here.
     * @since 5.0
     */
    @PseudoBean
    private static class GlobalVfsAccessor
    {
        @Inject
        @VFSManagerScope(VFSScope.Global)
        private IVirtualFileSystemManager globalVFSManager;

        private File getTemplatesRootDir()
        {
            return Paths.get(globalVFSManager.getRootDirectory().toString(), "Files", "templates").toFile();
        }
    }
}
