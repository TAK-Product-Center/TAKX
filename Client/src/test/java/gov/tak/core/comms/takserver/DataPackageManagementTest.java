package gov.tak.core.comms.takserver;

import gov.tak.api.comms.takserver.IDataPackageVfsListener;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.platform.cot.detail.CotDetailManager;
import gov.tak.platform.cot.exporter.CircleExporter;
import gov.tak.platform.datapackage.content.DataPackageContents;
import gov.tak.platform.datapackage.content.PackageManifest;
import gov.tak.platform.datapackage.content.PackageManifestContent;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.commons.file.FileUtils;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Validate {@link DataPackageManager} functionality for offline capabilities.
 *
 * @since 3.5
 */
public class DataPackageManagementTest extends AContainerTest
{
    private final DataPackageManager manager;

    public DataPackageManagementTest()
    {
        DataPackageManager bean = getBean(DataPackageManager.class);
        Object instance = CdiTestUtils.getInstance(bean);
        BlacksmithExtension.setBeansToUpdate(instance);
        manager = bean;

        // Used for dummy map objects
        ICotExporterManager exporterManager = getBean(ICotExporterManager.class);
        exporterManager.registerExporter(new CircleExporter());
    }

    @AfterEach
    public void teardownEach()
    {
        // Wipe any left-over contents between tests
        FileUtils.deleteDirectory(manager.getRootDir());
    }

    @AfterAll
    public void teardownAfterAll()
    {
        // Wipe any left-over contents after all tests are complete, including the root directory file
        FileUtils.deleteDirectory(manager.getRootDir().getParentFile());
    }

    @Test
    public void testCreateNewEmptyPackage()
    {
        String name = "test-package";
        File targetCreatedPath = new File(manager.getLocalImportRootDir(), name + ".zip");

        TestDataPackageVfsListener listener = new TestDataPackageVfsListener();
        listener.setAddedPredicate((packageFile, contents) -> packageFile.equals(targetCreatedPath));
        listener.setRemovedPredicate(packageFile -> packageFile.equals(targetCreatedPath));
        manager.addDataPackageVfsListener(listener);

        try
        {
            // Create the empty data package
            File testPackagePath = manager.createNewDataPackage(name);
            assertThat(testPackagePath).isEqualTo(targetCreatedPath);

            // Should only be one, a local import, its path should match the target.
            DataPackageContents testPackageContent = assertAndGetSinglePackageContents(targetCreatedPath);

            // The manifest's configured name should reflect the passed name.
            // There should be no contents.
            PackageManifest manifest = testPackageContent.getManifest();
            assertThat(manifest.getConfiguration().getName()).isEqualTo(name);
            assertThat(manifest.getContents().getContents()).isEmpty();

            // We should be able to delete the package.
            assertThatNoException().isThrownBy(() -> manager.deleteDataPackage(targetCreatedPath));

            // Listener should have been called for initial creation and the removal.
            assertThat(listener.allAddsSuccessful()).isTrue();
            assertThat(listener.allRemovesSuccessful()).isTrue();
        } catch (IOException ex)
        {
            fail("Could not create new data package from blank slate", ex);
        } finally
        {
            manager.removeDataPackageVfsListener(listener);
        }
    }

    @Test
    public void testCreateDuplicateNamedPackageFails()
    {
        String name = "duplicate";
        assertThatNoException().isThrownBy(() -> manager.createNewDataPackage(name));
        assertThatIOException().isThrownBy(() -> manager.createNewDataPackage(name));
    }

    @Test
    public void testAddTextFile()
    {
        String textFileContents = "hello world";
        String textFileName = "hello.txt";
        Path textFileLocalPath = manager.getRootDir().toPath().resolve(textFileName);
        assertThatNoException().isThrownBy(() -> Files.writeString(textFileLocalPath, textFileContents));

        TestDataPackageVfsListener listener = new TestDataPackageVfsListener();
        listener.setUpdatedPredicate((packageFile, contents) ->
        // Content uids (maps to 'zipPath' element) should match the added file name
        contents.getManifest().getContents().getContents().stream()
                .allMatch(c -> c.getManifestUid() != null && c.getManifestUid().endsWith(textFileName)));

        manager.addDataPackageVfsListener(listener);

        try
        {
            File testPackagePath = manager.createNewDataPackage("package-with-file");
            String textFileZipPath = manager.addFileToDataPackage(testPackagePath, textFileLocalPath.toFile());
            assertThat(listener.allAddsSuccessful()).isTrue();
            assertThat(listener.allUpdatesSuccessful()).isTrue();

            // Should only be one, a local import
            DataPackageContents testPackageContent = assertAndGetSinglePackageContents(testPackagePath);

            // The manifest contents should include our text file.
            PackageManifest manifest = testPackageContent.getManifest();
            List<PackageManifestContent> contents = manifest.getContents().getContents();
            assertThat(contents).hasSize(1);
            PackageManifestContent content = contents.get(0);
            assertThat(content.getManifestUid()).isEqualTo(textFileZipPath);

            // Delete it.
            assertThatNoException().isThrownBy(() -> manager.deleteDataPackage(testPackagePath));
            assertThat(listener.allRemovesSuccessful()).isTrue();
        } catch (IOException ex)
        {
            fail("Could not create and update new data package from blank slate", ex);
        } finally
        {
            manager.removeDataPackageVfsListener(listener);
        }
    }

    @Test
    public void testAddMapObjectWithAttachedImage()
    {
        // Empty image file to attach to map object later.
        String dummyImageName = "empty.png";
        File dummyImage = new File(manager.getRootDir(), dummyImageName);
        //noinspection ResultOfMethodCallIgnored
        assertThatNoException().isThrownBy(dummyImage::createNewFile);
        dummyImage.deleteOnExit();

        // Craft a map object with an attached image.
        String shapeUid = "the-shape-uid";
        final var dummyMO = new DummyMapObject(shapeUid);
        dummyMO.addAttachment(dummyImageName, dummyImage);

        var listener = new TestDataPackageVfsListener();
        manager.addDataPackageVfsListener(listener);

        try
        {
            getBean(CotDetailManager.class).startService();

            File testPackagePath = manager.createNewDataPackage("package-with-map-object");
            String moFileZipPath = manager.addMapObjectToDataPackage(testPackagePath, dummyMO);
            assertThat(listener.allAddsSuccessful()).isTrue();
            assertThat(listener.allUpdatesSuccessful()).isTrue();

            // Should only be one, a local import
            DataPackageContents testPackageContent = assertAndGetSinglePackageContents(testPackagePath);

            // The manifest contents should include:
            //  - Our map object cot event file.
            //  - Our image file
            PackageManifest manifest = testPackageContent.getManifest();
            List<PackageManifestContent> contents = manifest.getContents().getContents();
            assertThat(contents.size()).isEqualTo(2);

            // Map object uid == map object's uid
            // And the zip path is what we got told from the initial add call before.
            PackageManifestContent moContent = contents.get(0);
            assertThat(moContent.getUid()).isEqualTo(shapeUid);
            assertThat(moContent.getManifestUid()).isEqualTo(moFileZipPath);

            // Image file should be pulled from mo, and also added to the data package.
            PackageManifestContent imageContent = contents.get(1);
            assertThat(imageContent.getUid()).isEqualTo(shapeUid);
            assertThat(imageContent.getManifestUid()).endsWith(dummyImageName);

            // Remove the files from the package.
            List<String> addedZipPaths = List.of(moContent.getManifestUid(), imageContent.getManifestUid());
            assertThatNoException().isThrownBy(() -> manager.removeFilesFromDataPackage(testPackagePath, addedZipPaths));
            testPackageContent = assertAndGetSinglePackageContents(testPackagePath);
            assertThat(testPackageContent.getManifest().getContents().getContents()).isEmpty();

            // Delete it.
            assertThatNoException().isThrownBy(() -> manager.deleteDataPackage(testPackagePath));
            assertThat(listener.allRemovesSuccessful()).isTrue();
        } catch (IOException ex)
        {
            fail("Could not create and update new data package from blank slate", ex);
        } finally
        {
            getBean(CotDetailManager.class).stopService();
            manager.removeDataPackageVfsListener(listener);
        }
    }

    @Test
    public void testIsDeleteOnReceive() throws IOException
    {
        var dataPackage = createTempDataPackage(DataPackageManager.EMPTY_MANIFEST);
        assertThat(manager.isDeleteOnReceive(dataPackage)).isFalse();

        dataPackage = createTempDataPackage(DataPackageManager.EMPTY_TEMP_MANIFEST);
        assertThat(manager.isDeleteOnReceive(dataPackage)).isTrue();
    }

    @Nonnull
    private File createTempDataPackage(@Nonnull String manifest) throws IOException
    {
        final var bytes = manifest.getBytes();
        final var dataPackage = File.createTempFile("dataPackage", "zip");

        try (var fos = new FileOutputStream(dataPackage); var zos = new ZipOutputStream(fos))
        {
            final var zipEntry = new ZipEntry(DataPackageManager.MANIFEST_PATH);
            zos.putNextEntry(zipEntry);
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }

        dataPackage.deleteOnExit();

        return dataPackage;
    }

    @Nonnull
    private DataPackageContents assertAndGetSinglePackageContents(@Nonnull File expectedPath)
    {
        List<DataPackageContents> packageContents = manager.getAllDataPackageContents();
        assertThat(packageContents).hasSize(1);
        DataPackageContents testPackageContent = packageContents.get(0);
        assertThat(testPackageContent.getPackageFile()).isEqualTo(expectedPath);
        assertThat(testPackageContent.isLocalImport()).isTrue();
        assertThat(testPackageContent.isPeerImport()).isFalse();
        assertThat(testPackageContent.isServerDownload()).isFalse();
        return testPackageContent;
    }

    /**
     * Test listener which tracks data package adds/updates/removals.
     * <br>
     * Conditions can be applied per each listener method so that each is only marked as a success
     * under some circumstances.
     */
    private static class TestDataPackageVfsListener implements IDataPackageVfsListener
    {
        private final List<Boolean> addMatches = new ArrayList<>();
        private final List<Boolean> updatedMatches = new ArrayList<>();
        private final List<Boolean> removeMatches = new ArrayList<>();
        private BiPredicate<File, DataPackageContents> addedPredicate = (a, b) -> true;
        private BiPredicate<File, DataPackageContents> updatedPredicate = (a, b) -> true;
        private Predicate<File> removedPredicate = (a) -> true;

        public void setAddedPredicate(@Nonnull BiPredicate<File, DataPackageContents> addedPredicate)
        {
            this.addedPredicate = addedPredicate;
        }

        public void setUpdatedPredicate(@Nonnull BiPredicate<File, DataPackageContents> updatedPredicate)
        {
            this.updatedPredicate = updatedPredicate;
        }

        public void setRemovedPredicate(@Nonnull Predicate<File> removedPredicate)
        {
            this.removedPredicate = removedPredicate;
        }

        @Override
        public void onPackageAdded(@Nonnull File packageFile, @Nonnull DataPackageContents contents)
        {
            addMatches.add(addedPredicate.test(packageFile, contents));
        }

        @Override
        public void onPackageUpdated(@Nonnull File packageFile, @Nonnull DataPackageContents contents)
        {
            updatedMatches.add(updatedPredicate.test(packageFile, contents));
        }

        @Override
        public void onPackageRemoved(@Nonnull File packageFile)
        {
            removeMatches.add(removedPredicate.test(packageFile));
        }

        public boolean allAddsSuccessful()
        {
            return !addMatches.isEmpty() && addMatches.stream().allMatch(b -> b);
        }

        public boolean allUpdatesSuccessful()
        {
            return !updatedMatches.isEmpty() && updatedMatches.stream().allMatch(b -> b);
        }

        public boolean allRemovesSuccessful()
        {
            return !removeMatches.isEmpty() && removeMatches.stream().allMatch(b -> b);
        }
    }

    /**
     * Dummy type offering a simple shape without any service dependencies.
     */
    private static class DummyMapObject extends ShapeCircleMapObject
    {
        private final String uid;

        private DummyMapObject(String uid)
        {
            this.uid = uid;
            geoPoint = new GeoPoint(40, 30, 1, IGeoPoint.AltitudeReference.HAE, 0, 0);
            geoPointMetaData = new GeoPointMetaData();
            geoPointMetaData.set(geoPoint);
            attributeSet = new AttributeSet();
            shape = new Circle();
            shape.setLocation(geoPoint);
            shape.setRadius(10);
            setCotType(CotTypes.SHAPE_CIRCLE);
        }

        @Nonnull
        @Override
        public String getUid()
        {
            return uid;
        }

        @Override
        public String getFamily()
        {
            return IMarkerMapObject.MARKER_FAMILY;
        }

        @Override
        public String getType()
        {
            return IMarkerMapObject.MARKER_TYPE;
        }

        @Override
        public MapObjectOptions getMapObjectOptions()
        {
            return MapObjectOptions.STANDARD_RESOURCE_OPTIONS;
        }
    }
}
