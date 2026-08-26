package gov.takx.core.filesystem;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.filesystem.*;
import gov.takx.api.mapobject.*;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.IFileProgress;
import gov.takx.test.AIntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @since 5.2
 */
@PseudoBean
class VirtualFileSystemTest extends AIntegrationTest
{
    private static final RaptorLocation LOCATION = new RaptorLocation(20, 30);

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    private IVirtualFileSystem fileSystem;

    @BeforeAll
    public void setup()
    {
        fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
    }

    /**
     * tests {@link VirtualFileSystem#createFileMapObject(FilePath, File, ILocation, IFileProgress)}
     * 'happy path' test, with namespace, subfolder in file path.
     */
    @Test
    public void whenUseSubdirAndNamespace_createFileMapObject_fileCreated() throws IOException
    {
        String fileName = "LayerTest.xml";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).getPath();
        FilePath filePath = new FilePath("myNamespace", "my SubFolder", fileName);
        RaptorLocation location = LOCATION;

        IFileMapObject fileMapObject = fileSystem.createFileMapObject(
                filePath,
                new File(path),
                location,
                null);

        validateFileExistsAndNamed(fileMapObject, fileName);
        assertThat(fileMapObject.getLocation().sameAs(location)).isTrue();
        // verify mo has all filepath components (included in filepath)
        assertThat(fileMapObject.getFilePath()).isEqualTo(filePath);
        // also verify VFSFile filepath
        IVFSFile ivsFile = fileSystem.getFile(filePath);
        assertThat(ivsFile).isNotNull();
        assertThat(ivsFile.getFilePath()).isEqualTo(filePath);
    }

    /**
     * Similar test, method but method without param IFileProgress, and with no namespace, no subdirectory.
     * tests {@link VirtualFileSystem#createFileMapObject(FilePath, File, ILocation)}
     * giving all parameters correctly, with namespace.
     */
    @Test
    public void whenUseFilePathFileAndLocation_createFileMapObject_fileCreated() throws IOException
    {
        String fileName = "LayerTest.xml";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).getPath();
        assertThat(path).isNotNull();

        IFileMapObject fileMapObject = fileSystem.createFileMapObject(
                new FilePath(fileName),
                new File(path),
                LOCATION);

        validateFileExistsAndNamed(fileMapObject, fileName);
    }

    /**
     * Similar test, but on method without param Ilocation, with IFileProgress.
     * tests {@link VirtualFileSystem#createFileMapObject(FilePath, File, IFileProgress)}
     * giving all parameters correctly, with namespace.
     */
    @Test
    public void whenImplementFileProgress_createFileMapObject_fileCreated() throws IOException
    {
        String fileName = "LayerTest.xml";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).getPath();
        assertThat(path).isNotNull();
        // Create an implementation of IFileProgress that keeps track of the progress
        final int[] min = {100};
        final int[] max = {0};
        final int[] progress = {0};
        IFileProgress fileProgress = new IFileProgress()
        {
            @Override
            public void setFileMaximum(int maximum)
            {
                max[0] = maximum;
            }

            @Override
            public void setFileMinimum(int minimum)
            {
                min[0] = minimum;
            }

            @Override
            public void setFileProgress(int value)
            {
                progress[0] = value;
            }
        };

        IFileMapObject fileMapObject = fileSystem.createFileMapObject(
                new FilePath("myNamespace", fileName),
                new File(path),
                fileProgress);

        validateFileExistsAndNamed(fileMapObject, fileName);

        // also utilize mapObjectManager to get file and validate
        IMapObject mapObject = mapObjectManager.getMapObject(fileMapObject.getKey());
        assertThat(mapObject).isNotNull();
        assertThat(mapObject.getName()).isEqualTo(fileMapObject.getName());

        // Verify that the progress reached 100 (completed)
        assertThat(max[0]).isNotEqualTo(0);
        assertThat(min[0]).isNotEqualTo(100);
        assertThat(progress[0]).isEqualTo(100);
    }

    /**
     * Similar test, but on method with only params FilePath, File
     * tests {@link VirtualFileSystem#createFileMapObject(FilePath, File)}
     * giving all parameters correctly, with namespace.
     */
    @Test
    public void whenPassOnlyFileAndFilePath_createFileMapObject_fileCreated() throws IOException
    {
        String fileName = "LayerTest.xml";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).getPath();
        assertThat(path).isNotNull();

        IFileMapObject fileMapObject = createFileMapObjectHelper(new FilePath("myNamespace", fileName));
        validateFileExistsAndNamed(fileMapObject, fileName);
    }

    /**
     * tests {@link VirtualFileSystem#createFileMapObject(FilePath, File, ILocation, IFileProgress)}
     * giving empty file / source but target name, and others specified correctly.
     */
    @Test
    public void whenSourceFileEmpty_createFileMapObject_fileCreated() throws IOException
    {
        String fileName = "emptyFile";
        final File tempFile = File.createTempFile(fileName, ".xml");
        tempFile.deleteOnExit();

        IFileMapObject fileMapObject = fileSystem.createFileMapObject(
                new FilePath("myNamespace", "mySubFolder", fileName),
                tempFile,
                LOCATION,
                null);

        validateFileExistsAndNamed(fileMapObject, fileName);
    }

    /**
     * tests {@link VirtualFileSystem#createFileMapObject(FilePath, File, ILocation, IFileProgress)}
     * giving Invalid target, but since that is required, expect to catch Exception.
     */
    @Test
    public void whenInvalidTargetParameter_createFileMapObject_throws()
    {
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource("LayerTest.xml")).getPath();
        assertThat(path).isNotNull();

        assertThatThrownBy(() -> fileSystem.createFileMapObject(
                new FilePath("invalid\0path"),
                new File(path),
                LOCATION,
                null)).isInstanceOf(InvalidPathException.class);
    }

    /**
     * tests {@link VirtualFileSystem#createFile( FilePath)} with namespace & randomized name
     */
    @Test
    public void whenPassOnlyFilePathWithSeparator_createFile_fileIsCreated() throws IOException
    {
        FilePath filePath = new FilePath("myNamespace", "icons/" + UUID.randomUUID());
        IVFSFile ivfsFile = fileSystem.createFile(filePath);

        assertThat(ivfsFile.exists()).isTrue();
        assertThat(ivfsFile.getFilePath()).isEqualTo(filePath);
    }

    /**
     * create file with VFS, {@link VirtualFileSystem#deleteFile(IVFSFile)}, verify null with VFS
     */
    @Test
    public void whenCreateAndDeleteFile_getFile_IsNull() throws IOException
    {
        FilePath filePath = new FilePath("myNamespace", "LayerTest.xml");
        IFileMapObject fileMapObject = createFileMapObjectHelper(filePath);
        assertThat(fileMapObject.getVfsFile().exists()).isTrue();

        IVFSFile ivsFile = fileSystem.getFile(filePath);
        assertThat(ivsFile).isNotNull();

        fileSystem.deleteFile(ivsFile);
        IVFSFile deletedIvsFile = fileSystem.getFile(filePath);
        assertThat(deletedIvsFile).isNull();
    }

    /**
     * Add mock watchers to VFS, create file with VFS, delete file, verify watcher was called by those events
     */
    @Test
    public void whenVfsWatcherAdded_VfsFileCreatedAndDeleted_watcherUpdatedForEachEvent() throws IOException
    {
        DefaultVirtualFileSystemWatcher watcherMock = mock(DefaultVirtualFileSystemWatcher.class);

        fileSystem.addFileWatcher(watcherMock);
        // create a file
        FilePath filePath = new FilePath("myNamespace", "LayerTest.xml");
        createFileMapObjectHelper(filePath);

        // creating a file should also callback to DefaultVirtualFileSystemWatcher's onFileAdded(IVFSFile file);
        // mockito verify if watcher's onFileAdded is invoked as it should be
        verify(watcherMock, Mockito.atLeastOnce()).onFileAdded(any(IVFSFile.class));

        var ivsFile = fileSystem.getFile(filePath);
        assertThat(ivsFile).isNotNull();
        fileSystem.deleteFile(ivsFile);
        verify(watcherMock, Mockito.atLeastOnce()).onFileRemoved(any(IVFSFile.class));
    }

    private IFileMapObject createFileMapObjectHelper(FilePath filePath) throws IOException
    {
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filePath.getName())).getPath();
        assertThat(path).isNotNull();

        return fileSystem.createFileMapObject(filePath, new File(path));
    }

    private static void validateFileExistsAndNamed(IFileMapObject fileMapObject, String fileName)
    {
        var ivsFile = fileMapObject.getVfsFile();
        assertThat(ivsFile).isNotNull();
        assertThat(ivsFile.getFileName()).isEqualTo(fileName);
    }
}
