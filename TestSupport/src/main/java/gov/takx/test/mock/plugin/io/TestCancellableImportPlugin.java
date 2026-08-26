package gov.takx.test.mock.plugin.io;

import gov.takx.api.plugin.annotations.ImportExportFormat;
import gov.takx.api.plugin.annotations.ImportExportPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.io.ACancellableImportWizard;
import gov.takx.api.plugin.io.AImportExportPlugin;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IImportWizard;

/**
 * Test Plugin for contributing a {@link ACancellableImportWizard} for testing purposes.
 *
 */
@ImportExportPlugin(
        spec = @PluginSpecification(
                family = TestCancellableImportPlugin.FAMILY,
                type = TestCancellableImportPlugin.TYPE,
                version = "1.0.0",
                iconPath = ""),
        descriptor = @PluginDescriptor(
                name = "Electric Dog Polisher",
                description = "What, me worry?",
                longDescription = "What if the Hokey Pokey really IS what it's all about?",
                copyright = "2017",
                vendor = "Ministry of Silly Walks"),
        formats = @ImportExportFormat(
                identifier = ".nmea",
                title = "NMEA",
                notes = "Import a NMEA file",
                fileTypes = {"nmea", "NMEA"}))
public class TestCancellableImportPlugin extends AImportExportPlugin
{
    public static final String FAMILY = "gov.takx.test.iofamily";
    public static final String TYPE = "TestV2CancellableIOType";

    @Override
    public IImportWizard createImporter()
    {
        return new TestCancellableImportWizard();
    }

    @Override
    public IExportWizard createExporter()
    {
        return null;
    }
}
