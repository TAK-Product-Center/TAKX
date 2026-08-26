package gov.takx.test.mock.plugin.io;

import gov.takx.api.plugin.annotations.ImportExportFormat;
import gov.takx.api.plugin.annotations.ImportExportPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.io.AImportExportPlugin;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IImportWizard;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * A Fake Import/Export Plugin that can be used for testing core import/export functionality.
 *
 * @since 2.30
 */
@ImportExportPlugin(
        spec = @PluginSpecification(
                family = FakeImportExportPlugin.FAMILY,
                type = FakeImportExportPlugin.TYPE,
                version = FakeImportExportPlugin.VERSION,
                iconPath = ""),
        descriptor = @PluginDescriptor(
                name = "Simple Importer & Exporter",
                description = "Import/Export Plugin",
                longDescription = "Import/Export Plugin for tests"),
        formats = {
                @ImportExportFormat(
                        identifier = FakeImportExportPlugin.FILE_EXTENSION,
                        title = "FAKE",
                        notes = "Import/Export a fake file",
                        fileTypes = {FakeImportExportPlugin.FILE_EXTENSION})
        })
public class FakeImportExportPlugin extends AImportExportPlugin
{
    public static final String FAMILY = "gov.fake.test";
    public static final String TYPE = "FakeExporter";
    public static final String VERSION = "1.0.0";
    public static final String FILE_EXTENSION = "fakefile";

    @Inject
    private Instance<FakeImportWizard> fakeImportWizardInstance;

    @Override
    public IImportWizard createImporter()
    {
        return fakeImportWizardInstance.get();
    }

    @Override
    public IExportWizard createExporter()
    {
        return new FakeExportWizard();
    }
}
