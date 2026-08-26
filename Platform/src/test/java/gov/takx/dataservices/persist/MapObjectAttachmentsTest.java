package gov.takx.dataservices.persist;

import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@PseudoBean
public class MapObjectAttachmentsTest extends AIntegrationTest
{
    @Inject
    private IMapObjectManager mapObjectManager;

    private File tempDir;
    private final String[] EMPTY_STRING_ARRAY = new String[0];

    @BeforeAll
    public void setup() throws IOException
    {
        tempDir = Files.createTempDirectory("testDir_").toFile();
    }

    @AfterEach
    public void cleanupAfterMethod() throws IOException
    {
        deleteAllMapObjects();
        FileUtils.cleanDirectory(tempDir);
    }

    @AfterAll
    public void cleanupAfterAll() throws IOException
    {
        FileUtils.forceDelete(tempDir);
    }

    /**
     * Create a map object
     * @return The created map object
     */
    private IMapObject createMapObject()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.RESOURCE_ICON_FAMILY, "Red").uid("test").build();
        return mapObjectManager.getOrCreateMapObject(spec);
    }

    /**
     * Create a file inside the temp directory, marked for deletion on exit
     *
     * @param fileName the name of the random file
     * @return A {@link File} for use in testing
     */
    private File createTempFile(@Nonnull String fileName) throws IOException
    {
        File tempFile = new File(tempDir, fileName);
        tempFile.createNewFile();
        return tempFile;
    }

    /**
     * Get a AttributeSet entry for ATTACHED_FILES as a {@code List<String>}
     *
     * @param mapObject The map object to inspect
     * @return A List containing all the attached file names
     */
    private List<String> getAttachmentAttributeNames(IMapObject mapObject)
    {
        return new ArrayList<>(Arrays.asList(mapObject.getAttributeSet().getStringArrayAttribute(StandardAttributeNames.ATTACHED_FILES, EMPTY_STRING_ARRAY)));
    }

    /**
     * Get the attached file names as a {@code List<String>} via the {@link MapItem#getAttachments()} API
     *
     * @param mapObject The map object to inspect
     * @return A List containing all the attached file names
     */
    private List<String> getAttachmentFileNames(IMapObject mapObject)
    {
        return mapObject.getAttachments().stream().map(File::getName).toList();
    }

    /**
     * Handle assertion checks that are needed regularly
     * @param mapObject The MapObject to inspect
     * @param expectedAttachmentNames The list of expected attachment names to compare against
     */
    private void assertCorrectAttachments(IMapObject mapObject, List<String> expectedAttachmentNames)
    {
        assertThat(mapObject.getAttachments()).hasSize(expectedAttachmentNames.size());
        assertThat(getAttachmentFileNames(mapObject)).hasSameElementsAs(expectedAttachmentNames).hasSameElementsAs(getAttachmentAttributeNames(mapObject));
    }

    @Test
    public void mapObject_withNoAttachments_returnsEmpty()
    {
        assertThat(createMapObject().getAttachments()).isNotNull().isEmpty();
    }

    @Test
    public void mapObject_attachesFile() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        final File tempFile = createTempFile("test2.txt");
        final String newName = "newName.txt";

        mapObject.addAttachment(newName, tempFile);
        assertCorrectAttachments(mapObject, List.of(newName));
    }

    @Test
    public void mapObject_attachesInputStream() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        final File tempFile = createTempFile("test3.txt");
        final String newName = "newName.txt";

        InputStream tempSource = Files.newInputStream(tempFile.toPath());
        mapObject.addAttachment(newName, tempSource);
        tempSource.close();
        assertCorrectAttachments(mapObject, List.of(newName));
    }

    @Test
    public void mapObject_attachesMultipleFiles() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        int attachmentCount = 10;
        List<String> createdFileNames = new ArrayList<>(attachmentCount);
        String name;
        for (int i = 0; i < attachmentCount; i++)
        {
            name = "test4_" + i + ".txt";
            File tempFile = createTempFile(name);
            mapObject.addAttachment(name, tempFile);
            createdFileNames.add(name);
        }
        assertCorrectAttachments(mapObject, createdFileNames);
    }

    @Test
    public void mapObject_attachesMultipleInputStreams() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        int attachmentCount = 10;
        List<String> createdFileNames = new ArrayList<>(attachmentCount);
        String name;
        for (int i = 0; i < attachmentCount; i++)
        {
            name = "test5_" + i + ".txt";
            File tempFile = createTempFile(name);
            InputStream tempSource = Files.newInputStream(tempFile.toPath());
            mapObject.addAttachment(name, tempSource);
            tempSource.close();
            createdFileNames.add(name);
        }
        assertCorrectAttachments(mapObject, createdFileNames);
    }

    @Test
    @SuppressLogging(MapObject.class)
    public void mapObject_deleteNonexistentAttachment()
    {
        final MapObject mapObject = (MapObject) createMapObject();
        mapObject.deleteAttachments("test6.txt");
        assertThat(mapObject.getAttachments()).isNotNull().isEmpty();
    }

    @Test
    public void mapObject_deleteOnlyAttachment_byName() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        final File tempFile = createTempFile("test7.txt");
        final String newName = "newName.txt";

        mapObject.addAttachment(newName, tempFile);
        mapObject.deleteAttachments(newName);
        assertThat(mapObject.getAttachments()).isNotNull().isEmpty();
    }

    @Test
    public void mapObject_deleteOnlyAttachment_byFile() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        final File tempFile = createTempFile("test8.txt");
        final String newName = "newName.txt";

        mapObject.addAttachment(newName, tempFile);
        mapObject.deleteAttachments(mapObject.getAttachment(newName));
        assertThat(mapObject.getAttachments()).isNotNull().isEmpty();
    }

    @Test
    public void mapObject_deleteCorrectAttachment_byName() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        int attachmentCount = 10;
        List<String> createdFileNames = new ArrayList<>(attachmentCount);
        String name;
        for (int i = 0; i < attachmentCount; i++)
        {
            name = "test9_" + i + ".txt";
            File tempFile = createTempFile(name);
            mapObject.addAttachment(name, tempFile);
            createdFileNames.add(name);
        }

        assertCorrectAttachments(mapObject, createdFileNames);
        List<String> toDelete = List.of(createdFileNames.get(0), createdFileNames.get(8));
        createdFileNames.removeAll(toDelete);
        mapObject.deleteAttachments(toDelete.toArray(String[]::new));
        assertCorrectAttachments(mapObject, createdFileNames);
    }

    @Test
    public void mapObject_deleteCorrectAttachment_byFile() throws IOException
    {
        final MapObject mapObject = (MapObject) createMapObject();
        int attachmentCount = 10;
        List<File> createdFiles = new ArrayList<>(attachmentCount);
        List<String> createdFileNames = new ArrayList<>(attachmentCount);
        String name;
        for (int i = 0; i < attachmentCount; i++)
        {
            name = "test10_" + i + ".txt";
            createdFiles.add(mapObject.addAttachment(name, createTempFile(name)));
            createdFileNames.add(name);
        }

        assertCorrectAttachments(mapObject, createdFileNames);
        List<File> toDelete = List.of(createdFiles.get(3), createdFiles.get(5));
        createdFileNames.removeAll(toDelete.stream().map(File::getName).toList());
        mapObject.deleteAttachments(toDelete.toArray(File[]::new));
        assertCorrectAttachments(mapObject, createdFileNames);
    }
}
