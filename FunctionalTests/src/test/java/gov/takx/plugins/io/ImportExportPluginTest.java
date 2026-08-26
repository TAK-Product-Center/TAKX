package gov.takx.plugins.io;

import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.plugin.io.ACancellableImportWizard;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.wizard.WizardProperties;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.io.TestCancellableImportPlugin;
import gov.takx.test.mock.plugin.io.TestImportPlugin;
import gov.takx.test.mock.plugin.io.TestImportPlugin2;
import gov.takx.test.mock.plugin.io.TestImportWizard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for various I/O API Tests.
 * 
 * @since 3.4 (combined with V2ImportExportPluginTest)
 */
public class ImportExportPluginTest extends AIntegrationTest
{
    private static final int SHAPE_COUNT = 5;

    private IMapObjectManager mapObjectManager;
    private TestImportPlugin importPlugin;
    private TestImportPlugin2 importPlugin2;
    private TestCancellableImportPlugin cancellableImportPlugin;

    public ImportExportPluginTest()
    {
        setPluginsToRegister(TestImportPlugin.class, TestImportPlugin2.class, TestCancellableImportPlugin.class);
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);

        importPlugin = getPluginInstance(TestImportPlugin.FAMILY, TestImportPlugin.TYPE);
        importPlugin2 = getPluginInstance(TestImportPlugin2.FAMILY, TestImportPlugin2.TYPE);
        cancellableImportPlugin = getPluginInstance(TestCancellableImportPlugin.FAMILY, TestCancellableImportPlugin.TYPE);
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void testUiBindingFound()
    {
        final SwingUIBinder swingUIBinder = getBean(SwingUIBinder.class);
        IImportExportSwingUIProvider uiProvider = swingUIBinder.forWizard(importPlugin2.createImporter());
        assertThat(uiProvider).isNotNull();
    }

    @Test
    public void testAnnotationValuesReturned_whenCallingPluginMethods()
    {
        List<Format> formats = importPlugin2.getFormats();
        assertThat(formats).hasSize(1).extracting(Format::getIdentifier).containsOnly(".nmea");
        assertThat(formats.get(0).getFileTypes()).hasSize(2);
    }

    @Test
    public void verifyCancellableImport()
    {
        ACancellableImportWizard wizard = (ACancellableImportWizard) cancellableImportPlugin.createImporter();
        URL url = getClass().getClassLoader().getResource("kentucky.nmea");
        assertThat(url).isNotNull();

        File file = new File(url.getPath());
        List<File> files = new ArrayList<>();
        files.add(file);
        Map<String, Object> preconfSettings = new HashMap<>();
        preconfSettings.put(CommonWizardSettings.FILES, files);
        wizard.preconfigureWizard(preconfSettings);
        // TODO: AIntegrationTest needs to support wiring up of I/O plugin annotations for these to work
//            Assert.assertTrue(wizard.offerToProcessFile("nmea", file));
//            ImportExportResult result = wizard.doImport(null, new NullImportExportProgressMonitor());
//            Assert.assertFalse(result.getSuccess());

        assertThat(wizard.supportsMultipleFiles()).isTrue();
        assertThat(wizard.overrideDefaultImporters()).isTrue();
    }

    @Test
    public void createShapes() throws Exception
    {
        TestImportWizard importWizard = importPlugin.createImporter();
        importWizard.setGenerateShapes(true, SHAPE_COUNT);
        ImportExportResult result = importWizard.doImport(new WizardProperties(), null);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(SHAPE_COUNT);
        assertThat(result).isNotNull();

        IMapObjectPersist mapObjectPersist = getBean(IMapObjectPersist.class);
        for (String key : result.getAffectedObjects().keySet())
        {
            assertThat(mapObjectPersist.findMapObject(key)).isNotNull();
        }
    }
}
