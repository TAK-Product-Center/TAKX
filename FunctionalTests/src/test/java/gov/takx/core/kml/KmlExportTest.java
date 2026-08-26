package gov.takx.core.kml;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IShape;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verify KML and KMZ export by {@link KmlExporter}.
 */
public class KmlExportTest extends AIntegrationTest
{
    private static final String UNIT_ID = "Unit 1";

    private KmlExporter kmlExporter;
    private IMapObject mapObject;

    public KmlExportTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    @BeforeAll
    public void beforeAll()
    {
        kmlExporter = getBean(KmlExporter.class);

        final var spec = MapObjectSpec.builder("Test", "Test", UNIT_ID).location(GeoPoint.ZERO_POINT).build();
        mapObject = getBean(IMapObjectManager.class).getOrCreateMapObject(spec);
    }

    @AfterAll
    public void afterAll()
    {
        deleteAllMapObjects();
    }

    @Test
    public void testKmlExport() throws IOException
    {
        final File testKmlFile = Files.createTempFile("test", ".kml").toFile();
        testKmlFile.deleteOnExit();

        final List<IShape> exportableShapeObjects = List.of(new Circle("Shape 1", GeoPoint.ZERO_POINT, 100));

        assertThat(kmlExporter.writeKmlData(testKmlFile, exportableShapeObjects)).isTrue();
        assertThat(testKmlFile).exists();
        assertThat(testKmlFile.length()).isGreaterThan(0);
    }

    @Test
    public void testKmzExport() throws IOException
    {
        final List<IMapObject> exportableMapObjects = List.of(mapObject);

        final File testKmzFile = Files.createTempFile("test", ".kmz").toFile();
        testKmzFile.deleteOnExit();

        assertThat(kmlExporter.writeKmzData(testKmzFile, List.of(), exportableMapObjects, List.of())).isTrue();
        assertThat(testKmzFile).exists();

        // Test that the kmz file contains a doc.kml file.
        try (ZipFile zipFile = new ZipFile(testKmzFile))
        {
            assertThat(zipFile.getEntry("doc.kml")).isNotNull();
        }
    }
}
