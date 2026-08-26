package gov.takx.test.mock.plugin.io;

import gov.takx.api.plugin.annotations.ImportExportFormat;
import gov.takx.api.plugin.annotations.ImportExportPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.io.AImportExportPlugin;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IImportWizard;

/**
 * Simple I/O Plugin for testing purposes
 *
 */
@ImportExportPlugin(
        spec = @PluginSpecification(
                family = TestImportPlugin2.FAMILY,
                type = TestImportPlugin2.TYPE,
                version = "1.0.0",
                iconPath = ""),
        descriptor = @PluginDescriptor(
                name = "Electric Dog Polisher",
                description = "What, me worry?",
                longDescription = "Does the Hokey Pokey",
                copyright = "2017",
                vendor = "Department of Silly Walks"),
        formats = @ImportExportFormat(
                identifier = ".nmea",
                title = "NMEA",
                notes = "Import a NMEA file",
                fileTypes = {"nmea", "NMEA"}))
public class TestImportPlugin2 extends AImportExportPlugin
{
    public static final String FAMILY = "gov.takx.test.iofamily";
    public static final String TYPE = "TestIOType";

    @Override
    public IImportWizard createImporter()
    {
        return new TestImportWizard2();
    }

    @Override
    public IExportWizard createExporter()
    {
        return null;
    }
}
