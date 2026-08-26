package gov.takx.test.mock.plugin.io;

import gov.takx.api.plugin.annotations.ImportExportPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.io.AImportExportPlugin;
import gov.takx.api.plugin.io.IExportWizard;

/**
 */
@ImportExportPlugin(
        spec = @PluginSpecification(
                family = TestImportPlugin.FAMILY,
                type = TestImportPlugin.TYPE,
                version = TestImportPlugin.VERSION,
                iconPath = ""),
        descriptor = @PluginDescriptor(
                name = TestImportPlugin.FAMILY),
        formats = {})

public class TestImportPlugin extends AImportExportPlugin
{
    public static final String FAMILY = "TestImportPlugin";
    public static final String TYPE = "Importer";
    public static final String VERSION = "1.0.0";

    @Override
    public IExportWizard createExporter()
    {
        return null;
    }

    @Override
    public TestImportWizard createImporter()
    {
        return new TestImportWizard();
    }
}
