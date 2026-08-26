package gov.takx.api.plugin.io;

import gov.takx.api.io.Format;
import gov.takx.api.plugin.IAnnotatedPlugin;
import gov.takx.api.plugin.annotations.ImportExportFormat;
import gov.takx.api.plugin.annotations.ImportExportPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contract for Import / Export plugins.  In General, plugins should not implement this interface
 * but instead extend the abstract base class {@link AImportExportPlugin}.
 *
 */
public interface IImportExportPlugin extends IAnnotatedPlugin<ImportExportPlugin>
{
    /**
     * Create an import wizard, if supported by the plugin.
     *
     * @return A wizard for importing data into TAKX. May return {@code null} if this plugin does not support importing data.
     */
    IImportWizard createImporter();

    /**
     * Create an export wizard, if supported by the plugin.
     *
     * @return A wizard for exporting data from TAKX. May return {@code null} if this plugin does not support exporting data.
     */
    IExportWizard createExporter();

    /**
     * Gets the formats supported by this import / export plugin.
     *
     * @return The formats supported by this import / export plugin.
     */
    default List<Format> getFormats()
    {
        return Arrays.stream(getImportExportFormats())
                .map(format -> new Format(format.identifier(), format.title(), format.notes(), format.fileTypes()))
                .collect(Collectors.toList());
    }

    /**
     * @return The {@link ImportExportFormat} annotations
     */
    default ImportExportFormat[] getImportExportFormats()
    {
        return getPluginAnnotation().formats();
    }
}
