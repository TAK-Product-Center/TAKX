package gov.takx.core.ioservice;

import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.io.IImportExportService;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.IExtendedQueryConstraints;
import gov.takx.api.query.IExtendedQueryService;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.commons.file.FileUtils;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePluginWithOptions;
import gov.takx.test.mock.plugin.io.FakeImportExportPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * A simple test of the {@link IImportExportService}.
 *
 * @since 2.30
 */
public class ImportExportServiceTest extends AIntegrationTest
{
    public ImportExportServiceTest()
    {
        setPluginsToRegister(TestDevicePluginWithOptions.class, FakeImportExportPlugin.class);
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void testImportFiles()
    {
        final URL url = getClass().getClassLoader().getResource("test.fakefile");
        try
        {
            final File tmpInputFile = Files.createTempFile("", ".fakefile").toFile();
            tmpInputFile.deleteOnExit();

            FileUtils.copyResource(url, tmpInputFile);

            IImportExportService importExportService = getBean(IImportExportService.class);
            final CompletableFuture<List<ImportExportResult>> future = importExportService.importFiles(Collections.singletonList(tmpInputFile));

            future.join(); // Wait for completion, throws exception if completed exceptionally

            if (getRdmFromDatabase().isEmpty())
            {
                fail("Expected a MessageType0 RDM to be returned in the import test, but the query did not return any.");
            }
        } catch (Exception e)
        {
            fail("Error reading file in import test - " + url);
        }
    }

    /**
     * @return Returns the latest MessageType0 RDM from the database.
     */
    private List<IRaptorDataMessage> getRdmFromDatabase()
    {
        final IExtendedQueryService queryService = getBean(IExtendedQueryService.class);
        final IExtendedQueryConstraints queryConstraints = queryService.createEmptyConstraints();

        queryConstraints.setStructuresFromMapObject(TestDevicePluginWithOptions.FAMILY, TestDevicePluginWithOptions.TYPE);
        queryConstraints.addMessageStructureConstraint(TestDevicePluginWithOptions.MESSAGE_TYPE_0);

        // Sort by RDM Id since sorting by receive time can be non-deterministic when multiple RDMs have the same
        // receive time.
        queryConstraints.setSortingOptions(CoreField.id.selector, IQueryConstraints.SortOrder.DESCENDING);

        queryConstraints.setMaxMessageCount(1);
        return queryService.query(queryConstraints);
    }

    @Test
    public void testExport()
    {
        File tmpOutputFile = null;
        try
        {
            tmpOutputFile = Files.createTempFile("", ".fakefile").toFile();
            tmpOutputFile.deleteOnExit();
        } catch (IOException e)
        {
            fail("Error writing file in export test");
        }

        // Create map object to export
        MapObjectSpec mapObjectSpec = MapObjectSpec.builder(TestDevicePluginWithOptions.FAMILY, TestDevicePluginWithOptions.TYPE, "fake")
                .location(new RaptorLocation(1, 2, 3))
                .build();
        IMapObject mapObject = getBean(IMapObjectManager.class).getOrCreateMapObject(mapObjectSpec);

        Map<String, Object> configuration = new HashMap<>();
        configuration.put(CommonWizardSettings.SELECTED_MAP_OBJECTS, Collections.singletonList(mapObject));
        configuration.put(CommonWizardSettings.FILE, tmpOutputFile.getAbsolutePath());

        IImportExportService importExportService = getBean(IImportExportService.class);
        CompletableFuture<ImportExportResult> future = importExportService.export(FakeImportExportPlugin.FAMILY,
                FakeImportExportPlugin.TYPE, FakeImportExportPlugin.FILE_EXTENSION, configuration, true);

        ImportExportResult result = future.join();

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getMapObjectCount()).isEqualTo(1);
        assertThat(tmpOutputFile).hasContent("1.0,2.0,3.0");
    }
}
