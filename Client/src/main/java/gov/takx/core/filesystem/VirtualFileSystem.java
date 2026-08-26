package gov.takx.core.filesystem;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.filesystem.IFileGeoCoder;
import gov.takx.api.filesystem.ITemplateFileSystem;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.IVirtualFileSystemWatcher;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.IFileProgress;
import gov.takx.dataservices.filesystem.ExifFileGeoCoder;
import gov.takx.dataservices.filesystem.IMoveFileGeoCoder;
import gov.takx.dataservices.filesystem.VFSWatchDispatcher;
import gov.takx.dataservices.filesystem.WorldFileGeoCoder;
import gov.takx.dataservices.persist.FileMapObject;
import gov.takx.dataservices.persist.VFSFile;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.takx.api.mapobject.IFileMapObject.FILE_TYPE;

/**
 * VFS implementation.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class VirtualFileSystem implements IVirtualFileSystem
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<IFileGeoCoder> GEO_CODERS = Arrays.asList(
            new IMoveFileGeoCoder(),
            new ExifFileGeoCoder(),
            new WorldFileGeoCoder());

    @Inject
    private IRaptorEventLogManager eventLogManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    private final IVirtualFileSystemManager vfsManager;
    private final String namespace;
    private final File rootDir;
    private final VFSWatchDispatcher watchDispatcher;

    private final ITemplateFileSystem templateFs;
    private File familyRootDir;
    private File templateFamilyRootDir;

    /**
     * Protected constructor (accessed from within package only).
     *
     * @param vfsManager      Our manager
     * @param namespace       VFS namespace
     * @param rootDirectory   Root directory of the VFS
     * @param templateFs      template file system to copy in on initial creation of VFS
     * @param watchDispatcher Handler for listener notifications
     */
    VirtualFileSystem(IVirtualFileSystemManager vfsManager, String namespace, String rootDirectory,
                      ITemplateFileSystem templateFs, VFSWatchDispatcher watchDispatcher)
    {
        this.vfsManager = vfsManager;
        this.namespace = namespace;
        rootDir = new File(rootDirectory);
        this.watchDispatcher = watchDispatcher;

        this.templateFs = templateFs;

        if (createRootDir()) installTemplates();
    }

    @Override
    public String getNamespace()
    {
        return namespace;
    }

    @Override
    public IVirtualFileSystemManager getVfsManager()
    {
        return vfsManager;
    }

    @Override
    public File getRootDir()
    {
        return rootDir;
    }

    /**
     * Install any configured templates into the new VFS.
     *
     * @since 3.0
     */
    private void installTemplates()
    {
        if (templateFs != null)
        {
            for (IVFSFile file : templateFs.listFiles())
            {
                try
                {
                    FileUtils.copy(file.getFile(), new File(rootDir, file.getFileName()));
                } catch (IOException e)
                {
                    logger.warn("Unable to copy template file: {}", file.getPath(), e);
                }
            }
        }
    }

    public void setFamilyRootDirectory(File familyRootDir)
    {
        this.familyRootDir = familyRootDir;
    }

    public void setTemplateFamilyRootDirectory(File templateFamilyRootDir)
    {
        this.templateFamilyRootDir = templateFamilyRootDir;
    }

    @Override
    public File getFamilyRootDir()
    {
        return familyRootDir;
    }

    @Override
    public File getTemplateRootDir()
    {
        return templateFs != null ? templateFs.getRootDir() : null;
    }

    @Override
    public File getTemplateFamilyRootDir()
    {
        return templateFamilyRootDir;
    }

    @Override
    public boolean deleteFile(@Nonnull IVFSFile vfsFile)
    {
        fireFileRemoved(vfsFile);

        final File f = vfsFile.getFile();
        return (f == null) || f.delete();
    }

    @Override
    public IVFSFile addFile(@Nonnull FilePath target) throws IOException
    {
        return createFile(target, false);
    }

    @Override
    public IVFSFile createFile(@Nonnull FilePath target) throws IOException
    {
        return createFile(target, true);
    }

    private VFSFile createFile(@Nonnull FilePath target, boolean mustCreate) throws IOException
    {
        if (logger.isDebugEnabled()) logger.debug("Creating file {}", target);

        final String filePath = target.toString();
        final File newFile = new File(rootDir, filePath);

        //noinspection ResultOfMethodCallIgnored
        newFile.getParentFile().mkdirs();

        if (mustCreate || !newFile.exists())
        {
            if (!newFile.createNewFile()) logger.warn("Failed to create file: {}", newFile);
        }

        VFSFile vfsFile = new VFSFile(getVfsManager(), namespace, filePath);

        fireFileAdded(vfsFile);

        return vfsFile;
    }

    @Override
    public IFileMapObject createFileMapObject(@Nonnull FilePath target, @Nonnull File source) throws IOException
    {
        return createFileMapObject(target, source, null, null);
    }

    @Override
    public IFileMapObject createFileMapObject(@Nonnull FilePath target, @Nonnull File source, ILocation location) throws IOException
    {
        return createFileMapObject(target, source, location, null);
    }

    @Override
    public IFileMapObject createFileMapObject(@Nonnull FilePath target, @Nonnull File source, IFileProgress progress) throws IOException
    {
        return createFileMapObject(target, source, null, progress);
    }

    @Override
    public IFileMapObject createFileMapObject(@Nonnull FilePath target, final File source, ILocation location, IFileProgress progress)
            throws IOException
    {
        final String filePath = target.toString();
        final File newFile = new File(rootDir, filePath);

        if (source != null)
        {
            if (logger.isDebugEnabled()) logger.debug("Copying {} to {}", source, filePath);

            FileUtils.copyProgress(source, newFile, progress);
        }

        final VFSFile vfsFile = new VFSFile(getVfsManager(), namespace, filePath);

        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.FILE_FAMILY, FILE_TYPE)
                .location(location)
                .name(target.getName())
                .initializer(mo -> ((FileMapObject) mo).setFile(vfsFile))
                .build();

        final FileMapObject fmo = (FileMapObject) mapObjectManager.getOrCreateMapObject(spec);

        eventLogManager.logMapObjectOnline(new Date(), fmo, "");

        fireFileAdded(vfsFile);

        return fmo;
    }

    @Override
    public IVFSFile getFile(@Nonnull FilePath filePath)
    {
        final String relativePath = filePath.toString();
        final VFSFile vfsFile = new VFSFile(vfsManager, namespace, relativePath);

        return vfsFile.exists()
                ? vfsFile
                : null;
    }

    @Override
    public IFileMapObject getFileMapObject(@Nonnull FilePath filePath)
    {
        return mapObjectPersist.findMapObjectsOfType(IFileMapObject.class).stream()
                .filter(f -> f.getFileSystemNamespace().equals(namespace) && f.getFilePath().equals(filePath))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<IVFSFile> listFiles()
    {
        final Path rootPath = rootDir.toPath();

        try (Stream<Path> walk = Files.walk(rootPath))
        {
            return walk.filter(Files::isRegularFile)
                    .map(filePath -> new VFSFile(getVfsManager(), namespace, rootPath.relativize(filePath).toString()))
                    .collect(Collectors.toList());
        } catch (IOException e)
        {
            logger.error("Failed to load VFS files in: {}", rootPath, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<IFileMapObject> listFileMapObjects()
    {
        return mapObjectPersist.findMapObjectsOfType(IFileMapObject.class).stream()
                .filter(f -> f.getFileSystemNamespace().equals(namespace))
                .collect(Collectors.toList());
    }

    @Override
    public ILocation geoDecode(@Nonnull File file)
    {
        if (!file.isFile()) return null;

        return GEO_CODERS.stream()
                .map(geoCoder -> geoCoder.geoDecode(file))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean renameFile(@Nonnull IVFSFile file, @Nonnull FilePath newName)
    {
        return file.getFile().renameTo(new File(newName.toSystemPath(rootDir)));
    }

    @Override
    public void exportFile(@Nonnull FilePath sourceFile, @Nonnull File targetFile) throws IOException
    {
        exportFile(sourceFile, targetFile, null);
    }

    @Override
    public void exportFile(@Nonnull FilePath sourceFile, @Nonnull File targetFile, IFileProgress progress) throws IOException
    {
        IVFSFile vfsFile = getFile(sourceFile);
        if (vfsFile == null)
            throw new IllegalArgumentException("VFS file not found for path: " + sourceFile);
        exportFile(vfsFile, targetFile, progress);
    }

    @Override
    public void exportFile(@Nonnull IVFSFile sourceFile, @Nonnull File targetFile) throws IOException
    {
        exportFile(sourceFile, targetFile, null);
    }

    @Override
    public void exportFile(@Nonnull IVFSFile sourceFile, @Nonnull File targetFile, IFileProgress progress) throws IOException
    {
        FileUtils.copyProgress(sourceFile.getFile(), targetFile, progress);
    }

    @Override
    public boolean fileExists(FilePath file)
    {
        if (file == null) return false;

        final IVFSFile vfsFile = getFile(file);
        return vfsFile != null && vfsFile.exists();
    }

    @Override
    public void addFileWatcher(@Nonnull IVirtualFileSystemWatcher watcher)
    {
        watchDispatcher.addWatcher(namespace, watcher);
    }

    @Override
    public void removeFileWatcher(@Nonnull IVirtualFileSystemWatcher watcher)
    {
        watchDispatcher.removeWatcher(namespace, watcher);
    }

    private void fireFileAdded(@Nonnull IVFSFile file)
    {
        watchDispatcher.fireFileAdded(namespace, file);
    }

    private void fireFileRemoved(@Nonnull IVFSFile file)
    {
        watchDispatcher.fireFileRemoved(namespace, file);
    }

    /**
     * Create the root directory, if needed.
     *
     * @return true if the file system was created fresh (root directory did not exist before calling this method),
     * or false if the file system already existed.
     */
    private boolean createRootDir()
    {
        boolean createNew = !rootDir.exists();

        if (createNew && !rootDir.mkdirs())
        {
            logger.error("Failed to create VFS root: {}", rootDir);
        }

        return createNew;
    }
}
