package gov.takx.core.view.gui.maps.importers.kml;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for the {@link KMLArtifactCopier} class. The tests contained in this class use temporary files and
 * directories to test that KML-import-related artifacts are properly copied and referenced.
 * <p>
 * Note that the phrase "referenced KML file" is used to describe a local KML file that is linked in a source KML file
 * via the {@code <Link>} element, using a relative path within the {@code <href>} element.
 *
 * @since 2.29.1
 */
public class KMLArtifactCopierTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String sedanIconPath = "icons/sedan-96.png";
    private static final String truckIconPath = "icons/truck-96.png";
    private static final String childDirectoryName = "data";
    private static final String grandchildDirectoryName = "more_data";
    private static final String kmlSuffix = ".kml";

    private KMLArtifactCopier kmlArtifactCopier;

    private static Path sourceKmlDirectoryPath;
    private static Path targetKmlDirectoryPath;

    private Path sourceKmlFile;
    private Path targetKmlFile;

    private final String testDataNoStyle = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document id="Doc_%s">
                <name>Test Data, No Style</name>
                <Placemark>
                  <name>Point 1</name>
                  <Point>
                    <coordinates>
                      -104.9126711,40.9811989,0
                    </coordinates>
                  </Point>
                </Placemark>
              </Document>
            </kml>""";

    private final String testDataWithStyle = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document id="Doc_%s">
                <name>Test Data, With Style</name>
                <Style id="icon-1">
                  <IconStyle>
                    <color>ffd18802</color>
                    <scale>1</scale>
                    <Icon>
                      <href>%s</href>
                    </Icon>
                  </IconStyle>
                  <LabelStyle>
                    <scale>0</scale>
                  </LabelStyle>
                </Style>    <Placemark>
                  <name>Point 1</name>
                  <Point>
                    <coordinates>
                      -104.9126711,40.9811989,0
                    </coordinates>
                  </Point>
                </Placemark>
              </Document>
            </kml>""";

    private final String testReference = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
                <Document id="Doc_%s">
                    <name>Tracks</name>
                    <NetworkLink id="ref_1">
                        <name>ref_tracks</name>
                        <Link id="link_1">
                            <href>%s</href>
                            <refreshMode>onInterval</refreshMode>
                            <refreshInterval>3</refreshInterval>
                        </Link>
                    </NetworkLink>
                </Document>
            </kml>""";

    @BeforeAll
    public static void setupTemporaryKmlDirectories() throws IOException
    {
        sourceKmlDirectoryPath = Files.createTempDirectory("source-kml");
        targetKmlDirectoryPath = Files.createTempDirectory("target-kml");
    }

    @AfterAll
    public static void teardownTemporaryKmlDirectories() throws IOException
    {
        try
        {
            Files.delete(sourceKmlDirectoryPath);
        } catch (DirectoryNotEmptyException e)
        {
            logger.warn("Unable to delete non-empty source temp directory: {}  ...attempting recursive delete.", sourceKmlDirectoryPath);
        }

        try
        {
            Files.delete(targetKmlDirectoryPath);
        } catch (DirectoryNotEmptyException e)
        {
            logger.warn("Unable to delete non-empty target temp directory: {}  ...attempting recursive delete.", targetKmlDirectoryPath);
        }

        if (Files.exists(sourceKmlDirectoryPath))
        {
            FileUtils.deleteDirectory(sourceKmlDirectoryPath.toFile());
        }
        if (Files.exists(targetKmlDirectoryPath))
        {
            FileUtils.deleteDirectory(targetKmlDirectoryPath.toFile());
        }
    }

    @BeforeEach
    public void initializeKmlArtifactCopier()
    {
        kmlArtifactCopier = new KMLArtifactCopier();
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     * </pre>
     * <p>
     * ...where source.kml contains no artifact references.
     */
    @Test
    public void sourceKmlFileCopiesToTargetDirectory() throws IOException
    {
        sourceKmlFile = createKmlFileFromString(String.format(testDataNoStyle, '1'), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceKmlFile, targetKmlFile);

        cleanUpTestFiles(sourceKmlFile, targetKmlFile);
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- data.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- data.kml
     * </pre>
     * <p>
     * ...where source.kml references the artifact data.kml.
     */
    @Test
    public void referencedKmlFileInSameDirectoryCopiesToTargetDirectory() throws IOException
    {
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testDataNoStyle, '1'), sourceKmlDirectoryPath);
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(sourceReferencedKmlFile.getFileName());

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '2', sourceReferencedKmlFile.getFileName()), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceReferencedKmlFile, targetReferencedKmlFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, targetKmlFile, targetReferencedKmlFile);
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     * </pre>
     * <p>
     * ...where source.kml references the artifact data.kml.
     */
    @Test
    public void referencedKmlFileInChildDirectoryCopiesToTargetDirectory() throws IOException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testDataNoStyle, '1'), Paths.get(sourceKmlDirectoryPath.toString(), sourceChildDirectoryName));
        String sourceReferencedKmlFileName = sourceReferencedKmlFile.getFileName().toString();
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName));

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '2', Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceReferencedKmlFile, targetReferencedKmlFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, sourceChildDirectory,
                targetKmlFile, targetReferencedKmlFile, targetReferencedKmlFile.getParent());
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     * </pre>
     * <p>
     * ...where source.kml references the artifact data.kml, and data.kml references the artifact transitive_data.kml.
     */
    @Test
    public void transitivelyReferencedKmlFileCopiesToTargetDirectory() throws IOException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceGrandchildDirectory = createTempDirectoryWithParent(grandchildDirectoryName, sourceChildDirectory);
        String sourceGrandchildDirectoryName = sourceGrandchildDirectory.getFileName().toString();
        Path sourceTransitiveKmlFile = createKmlFileFromString(String.format(testDataNoStyle, '1'), Paths.get(sourceKmlDirectoryPath.toString(), sourceChildDirectoryName, sourceGrandchildDirectoryName));
        String sourceTransitiveKmlFileName = sourceTransitiveKmlFile.getFileName().toString();
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testReference, '2', Paths.get(sourceGrandchildDirectoryName, sourceTransitiveKmlFileName)), Paths.get(sourceKmlDirectoryPath.toString(), sourceChildDirectoryName));
        String sourceReferencedKmlFileName = sourceReferencedKmlFile.getFileName().toString();
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName));
        Path targetTransitiveKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceGrandchildDirectoryName, sourceTransitiveKmlFileName));

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '3', Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceTransitiveKmlFile, targetTransitiveKmlFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, sourceTransitiveKmlFile, sourceGrandchildDirectory, sourceChildDirectory,
                targetKmlFile, targetReferencedKmlFile, targetTransitiveKmlFile, targetTransitiveKmlFile.getParent(), targetReferencedKmlFile.getParent());
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- icon.png
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- icon.png
     * </pre>
     * <p>
     * ...where source.kml references the artifact icon.png.
     */
    @Test
    public void iconInSourceKmlFileCopiesToTargetDirectory() throws IOException, URISyntaxException
    {
        Path sourceIconFile = copyIconFromResources(sedanIconPath, sourceKmlDirectoryPath);
        Path targetIconFile = targetKmlDirectoryPath.resolve(sourceIconFile.getFileName());

        sourceKmlFile = createKmlFileFromString(String.format(testDataWithStyle, '1', sourceIconFile.getFileName()), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceIconFile, targetIconFile);

        cleanUpTestFiles(sourceKmlFile, sourceIconFile, targetKmlFile, targetIconFile);
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- icon.png
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- icon.png
     * </pre>
     * <p>
     * ...where source.kml references the artifact data.kml, and data.kml references the artifact icon.png.
     */
    @Test
    public void iconInReferencedKmlFileCopiesToTargetDirectory() throws IOException, URISyntaxException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceIconFile = copyIconFromResources(sedanIconPath, sourceChildDirectory);
        String sourceIconFileName = sourceIconFile.getFileName().toString();
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testDataWithStyle, '1', sourceIconFileName), Paths.get(sourceKmlDirectoryPath.toString(), sourceChildDirectoryName));
        String sourceReferencedKmlFileName = sourceReferencedKmlFile.getFileName().toString();
        Path targetIconFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceIconFileName));
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName));

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '2', Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceIconFile, targetIconFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, sourceIconFile, sourceChildDirectory,
                targetKmlFile, targetReferencedKmlFile, targetIconFile, targetIconFile.getParent());
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     *                          |--- icon.png
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     *                          |--- icon.png
     * </pre>
     * <p>
     * ...where source.kml references the artifact data.kml, data.kml references the artifact transitive_data.kml, and
     * transitive_data.kml references the artifact icon.png.
     */
    @Test
    public void iconInTransitivelyReferencedKmlFileCopiesToTargetDirectory() throws IOException, URISyntaxException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceGrandchildDirectory = createTempDirectoryWithParent(grandchildDirectoryName, sourceChildDirectory);
        String sourceGrandchildDirectoryName = sourceGrandchildDirectory.getFileName().toString();
        Path sourceIconFile = copyIconFromResources(sedanIconPath, sourceGrandchildDirectory);
        String sourceIconFileName = sourceIconFile.getFileName().toString();
        Path sourceTransitiveKmlFile = createKmlFileFromString(String.format(testDataWithStyle, '1', sourceIconFileName), Paths.get(sourceKmlDirectoryPath.toString(), sourceChildDirectoryName, sourceGrandchildDirectoryName));
        String sourceTransitiveKmlFileName = sourceTransitiveKmlFile.getFileName().toString();
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testReference, '2', Paths.get(sourceGrandchildDirectoryName, sourceTransitiveKmlFileName)), Paths.get(sourceKmlDirectoryPath.toString(), sourceChildDirectoryName));
        String sourceReferencedKmlFileName = sourceReferencedKmlFile.getFileName().toString();
        Path targetIconFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceGrandchildDirectoryName, sourceIconFileName));
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName));
        Path targetTransitiveKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceGrandchildDirectoryName, sourceTransitiveKmlFileName));

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '3', Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceIconFile, targetIconFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, sourceTransitiveKmlFile, sourceIconFile, sourceGrandchildDirectory, sourceChildDirectory,
                targetKmlFile, targetReferencedKmlFile, targetTransitiveKmlFile, targetIconFile, targetTransitiveKmlFile.getParent(), targetReferencedKmlFile.getParent());
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_icons
     *                  |
     *                  |--- icon.png
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_icons
     *                  |
     *                  |--- icon.png
     * </pre>
     * <p>
     * ...where source.kml references the artifact icon.png.
     */
    @Test
    public void iconInChildDirectoryCopiesToTargetDirectory() throws IOException, URISyntaxException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceIconFile = copyIconFromResources(sedanIconPath, sourceChildDirectory);
        String sourceIconFileName = sourceIconFile.getFileName().toString();
        Path targetIconFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceIconFileName));

        sourceKmlFile = createKmlFileFromString(String.format(testDataWithStyle, '1', Paths.get(sourceChildDirectoryName, sourceIconFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceIconFile, targetIconFile);

        cleanUpTestFiles(sourceKmlFile, sourceIconFile, sourceChildDirectory,
                targetKmlFile, targetIconFile, targetIconFile.getParent());
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- source(2).kml
     * </pre>
     * <p>
     * ...where the contents of source.kml and source(2).kml (both in the target directory) are different.
     */
    @Test
    public void sourceKmlFileIsIndexedOnCopyIfFileWithSameNameAlreadyExistsInTargetDirectory() throws IOException
    {
        Path preexistingKmlFileInTargetDirectory = createKmlFileFromString(String.format(testDataNoStyle, '1'), targetKmlDirectoryPath);

        sourceKmlFile = createTempFileWithSameName(preexistingKmlFileInTargetDirectory.getFileName().toString(), String.format(testDataNoStyle, '2'), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(createIndexedName(sourceKmlFile.getFileName().toString(), "(2)"));

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceKmlFile, targetKmlFile);

        cleanUpTestFiles(sourceKmlFile, targetKmlFile, preexistingKmlFileInTargetDirectory);
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- data(2).kml
     * </pre>
     * <p>
     * ...where source.kml references data.kml in the source directory, and the contents of data.kml and data(2).kml
     * (both in the target directory) are different.
     */
    @Test
    public void referencedKmlFileIsIndexedOnCopyIfFileWithSameNameAlreadyExistsInTargetDirectory() throws IOException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testDataNoStyle, '1'), sourceChildDirectory);
        String sourceReferencedKmlFileName = sourceReferencedKmlFile.getFileName().toString();
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, createIndexedName(sourceReferencedKmlFileName, "(2)")));
        Path targetChildDirectory = createTempDirectoryWithSameName(sourceChildDirectoryName, targetKmlDirectoryPath);
        Path preexistingKmlFileInTargetDirectory = createTempFileWithSameName(sourceReferencedKmlFileName, String.format(testDataNoStyle, '2'), targetChildDirectory);

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '3', Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceReferencedKmlFile, targetReferencedKmlFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, sourceChildDirectory,
                targetKmlFile, targetReferencedKmlFile, preexistingKmlFileInTargetDirectory, targetReferencedKmlFile.getParent());
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- some_data
     *                  |
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- some_data
     *                  |
     *                  |--- data.kml
     *                  |--- more_data
     *                          |
     *                          |--- transitive_data.kml
     *                          |--- transitive_data(2).kml
     * </pre>
     * <p>
     * ...where source.kml references data.kml in the source directory, data.kml references transitive_data.kml in the
     * source directory, and the contents of transitive_data.kml and transitive_data(2).kml (both in the target directory)
     * are different.
     */
    @Test
    public void transitiveKmlFileIsIndexedOnCopyIfFileWithSameNameAlreadyExistsInTargetDirectory() throws IOException
    {
        Path sourceChildDirectory = createTempDirectoryWithParent(childDirectoryName, sourceKmlDirectoryPath);
        String sourceChildDirectoryName = sourceChildDirectory.getFileName().toString();
        Path sourceGrandchildDirectory = createTempDirectoryWithParent(grandchildDirectoryName, sourceChildDirectory);
        String sourceGrandchildDirectoryName = sourceGrandchildDirectory.getFileName().toString();
        Path sourceTransitiveKmlFile = createKmlFileFromString(String.format(testDataNoStyle, '1'), sourceGrandchildDirectory);
        String sourceTransitiveKmlFileName = sourceTransitiveKmlFile.getFileName().toString();
        Path sourceReferencedKmlFile = createKmlFileFromString(String.format(testReference, '2', Paths.get(sourceGrandchildDirectoryName, sourceTransitiveKmlFileName)), sourceChildDirectory);
        String sourceReferencedKmlFileName = sourceReferencedKmlFile.getFileName().toString();
        Path targetReferencedKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName));
        Path targetTransitiveKmlFile = targetKmlDirectoryPath.resolve(Paths.get(sourceChildDirectoryName, sourceGrandchildDirectoryName, createIndexedName(sourceTransitiveKmlFileName, "(2)")));
        Path targetChildDirectory = createTempDirectoryWithSameName(sourceChildDirectoryName, targetKmlDirectoryPath);
        Path targetGrandchildDirectory = createTempDirectoryWithSameName(sourceGrandchildDirectoryName, targetChildDirectory);
        Path preexistingKmlFileInTargetDirectory = createTempFileWithSameName(sourceTransitiveKmlFileName, String.format(testDataNoStyle, '3'), targetGrandchildDirectory);

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '4', Paths.get(sourceChildDirectoryName, sourceReferencedKmlFileName)), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceTransitiveKmlFile, targetTransitiveKmlFile);

        cleanUpTestFiles(sourceKmlFile, sourceReferencedKmlFile, sourceTransitiveKmlFile, sourceGrandchildDirectory, sourceChildDirectory,
                targetKmlFile, targetReferencedKmlFile, targetTransitiveKmlFile, preexistingKmlFileInTargetDirectory, targetGrandchildDirectory, targetChildDirectory);
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- icon.png
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- icon.png
     * </pre>
     * <p>
     * ...is correctly copied to
     *
     * <pre>
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- icon.png
     *          |--- icon(2).png
     * </pre>
     * <p>
     * ...where the contents of icon.png and icon(2).png (both in the target directory) are different.
     */
    @Test
    public void iconFileIsIndexedOnCopyIfFileWithSameNameAlreadyExistsInTargetDirectory() throws IOException, URISyntaxException
    {
        Path sourceIconFile = copyIconFromResources(sedanIconPath, sourceKmlDirectoryPath);
        Path targetIconFile = targetKmlDirectoryPath.resolve(createIndexedName(sourceIconFile.getFileName().toString(), "(2)"));
        Path preexistingIconFileInTargetDirectory = copyIconFromResources(truckIconPath, targetKmlDirectoryPath);

        preexistingIconFileInTargetDirectory = Files.move(preexistingIconFileInTargetDirectory, preexistingIconFileInTargetDirectory.resolveSibling(sourceIconFile.getFileName()));

        sourceKmlFile = createKmlFileFromString(String.format(testDataWithStyle, '1', sourceIconFile.getFileName().toString()), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName());

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceIconFile, targetIconFile);

        cleanUpTestFiles(sourceKmlFile, sourceIconFile, targetKmlFile, targetIconFile, preexistingIconFileInTargetDirectory);
    }

    /**
     * Example scenario being tested, given source.kml:
     * <pre>
     *
     *      [SOURCE_DIRECTORY]
     *          |
     *          |--- source.kml
     *          |--- data.kml
     *
     *      [TARGET_DIRECTORY]
     *          |
     *          |--- data.kml
     * </pre>
     * <p>
     * ...where source.kml references the artifact data.kml, and the contents of data.kml in the source directory and
     * data.kml in the target directory are exactly the same, thus no copying of data.kml should occur.
     */
    @Test
    public void referencedKmlFileIsNotCopiedIfAlreadyExistentInTargetDirectory() throws IOException
    {
        Path preexistingKmlFileInTargetDirectory = createKmlFileFromString(String.format(testDataNoStyle, '1'), targetKmlDirectoryPath);
        Path sourceReferencedKMLFile = createTempFileWithSameName(preexistingKmlFileInTargetDirectory.getFileName().toString(), String.format(testDataNoStyle, '1'), sourceKmlDirectoryPath);

        sourceKmlFile = createKmlFileFromString(String.format(testReference, '2', sourceReferencedKMLFile.getFileName()), sourceKmlDirectoryPath);
        targetKmlFile = targetKmlDirectoryPath.resolve(createIndexedName(sourceReferencedKMLFile.getFileName().toString(), "(2)"));

        kmlArtifactCopier.copyKmlArtifacts(sourceKmlFile, targetKmlDirectoryPath);

        assertFileCorrectlyCopied(sourceReferencedKMLFile, preexistingKmlFileInTargetDirectory);
        assertThat(!targetKmlFile.toFile().exists()).isTrue();

        cleanUpTestFiles(sourceKmlFile, targetKmlDirectoryPath.resolve(sourceKmlFile.getFileName().toString()), sourceReferencedKMLFile, preexistingKmlFileInTargetDirectory);
    }

    /**
     * Creates a temporary file with the given file contents in the parent directory, using the same name as the
     * argument provided.
     *
     * @param name            The name of the temporary file to create.
     * @param fileContents    The contents that the temporary file will contain.
     * @param parentDirectory The directory in which to place the temporary file.
     * @return The absolute path of the newly created temporary file.
     * @throws IOException If an error occurs while creating the temporary file.
     */
    private Path createTempFileWithSameName(String name, String fileContents, Path parentDirectory) throws IOException
    {
        Path tempFileWithSameName = createKmlFileFromString(fileContents, parentDirectory);

        return Files.move(tempFileWithSameName, tempFileWithSameName.resolveSibling(name));
    }

    /**
     * Creates a temporary directory in the parent directory, using the same name as the argument provided.
     *
     * @param name            The name of the temporary directory to create.
     * @param parentDirectory The directory in which to place the temporary directory.
     * @return The absolute path of the newly created temporary directory.
     * @throws IOException If an error occurs while creating the temporary directory.
     */
    private Path createTempDirectoryWithSameName(String name, Path parentDirectory) throws IOException
    {
        Path tempDirectoryWithSameName = createTempDirectoryWithParent(childDirectoryName, parentDirectory);

        return Files.move(tempDirectoryWithSameName, tempDirectoryWithSameName.resolveSibling(name));
    }

    /**
     * Creates a KML file in the parent directory from the given KML string.
     *
     * @param kmlFileContents KML string from which the KML file is created.
     * @param parentDirectory The directory in which the KML file is created.
     * @return The absolute path to the newly created KML file.
     */
    private Path createKmlFileFromString(String kmlFileContents, Path parentDirectory) throws IOException
    {
        Path kmlFile = Files.createTempFile(parentDirectory, null, kmlSuffix);
        Files.writeString(kmlFile, kmlFileContents);
        return kmlFile;
    }

    /**
     * Creates an indexed version of the given file name. E.g. given "data.kml" and "(2)", this method would return
     * "data(2).kml".
     *
     * @param filename    The filename to index.
     * @param indexString The string to use as the new index.
     * @return The indexed version of the filename.
     */
    private String createIndexedName(String filename, String indexString)
    {
        String[] filenameSlices = filename.split("\\.", -1);

        return filenameSlices[0] + indexString + '.' + filenameSlices[1];
    }

    /**
     * Creates a temporary directory inside of a given parent directory.
     *
     * @param tempDirectoryName The name of the temporary directory to create.
     * @param parentDirectory   The parent directory in which to create the temporary directory.
     * @return The absolute path to the newly-created temporary directory.
     * @throws IOException If an error occurs while creating the temporary directory.
     */
    private Path createTempDirectoryWithParent(String tempDirectoryName, Path parentDirectory) throws IOException
    {
        return Files.createTempDirectory(parentDirectory, tempDirectoryName);
    }

    /**
     * Asserts that a source file was correctly copied to its target file. Correctly copied in this case means that the
     * target file exists, the source and target files contain the same contents, and the source and target file streams
     * were both properly closed.
     *
     * @param sourceFile The source file that was copied.
     * @param targetFile The target file to which the source was copied.
     * @throws IOException If an error occurs while checking for equality of file contents.
     */
    private void assertFileCorrectlyCopied(Path sourceFile, Path targetFile) throws IOException
    {
        assertThat(Files.exists(targetFile)).withFailMessage("Expected to find file at " + targetFile + " after copying from " + sourceFile).isTrue();
        assertThat(FileUtils.contentEquals(sourceFile.toFile(), targetFile.toFile())).withFailMessage("Target file (" + targetFile + ") is not the same as source file (" + sourceFile + ") after copying.").isTrue();
        assertThat(isFileClosed(sourceFile)).withFailMessage("Source file at " + sourceFile + " was not properly closed after copying.").isTrue();
        assertThat(isFileClosed(targetFile)).withFailMessage("Target file at " + targetFile + " was not properly closed after copying.").isTrue();
    }

    /**
     * Checks to see if the given file does not currently have an open stream, i.e. that it was properly closed.
     *
     * @param filePath The path of the file to check for stream closure.
     * @return True if the file is closed.
     */
    private boolean isFileClosed(Path filePath)
    {
        try
        {
            FileUtils.touch(filePath.toFile());
            return true;
        } catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Copies the icon given by the icon resource path to a specified directory. This method assumes that the icon
     * already exists in test resources.
     *
     * @param iconResourcePath The path to the icon, relative to the test resources directory (e.g. "icons/sedan.png").
     * @param parentDirectory  The absolute path to the directory in which the icon copy will be placed.
     * @return The absolute path to the icon copy.
     * @throws URISyntaxException If an error occurs while converting the resource URL to a URI.
     * @throws IOException        If an error occurs while copying the icon to the parent directory.
     */
    private Path copyIconFromResources(String iconResourcePath, Path parentDirectory) throws URISyntaxException, IOException
    {
        Path iconResource = Paths.get(getClass().getClassLoader().getResource(iconResourcePath).toURI());
        Path iconTarget = Paths.get(parentDirectory.toString(), iconResource.getFileName().toString());

        Files.copy(iconResource, iconTarget);

        return iconTarget;
    }

    /**
     * Deletes all files given by the Path arguments, logging a warning if unable to delete a file for any reason.
     *
     * @param paths The absolute paths of the files to delete.
     * @throws IOException If an error occurs while attempting to delete a file.
     */
    private void cleanUpTestFiles(Path... paths) throws IOException
    {
        for (Path currentFile : paths)
        {
            Files.delete(currentFile);

            if (currentFile.toFile().exists())
            {
                logger.warn("Unable to delete test file in temp directory: {}", currentFile);
            }
        }
    }
}
