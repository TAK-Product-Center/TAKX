package gov.takx.api.io;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.io.AImportExportWizard;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.registry.NoSuchPluginException;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Provides support for various import and export operations.
 * <p>
 * This interface is injectable into your plugin.  To do so, add the following instance variable to your class.
 * <pre>
 * {@code @Inject}
 * {@code private IImportExportService importExportService;}
 * </pre>
 *
 * @since 2.30
 */
@InjectableService
@NoPluginImplementations
public interface IImportExportService
{
    /**
     * Request the provided files be imported. The files will be handed off to any registered
     * {@link IImportExportPlugin} that can process the file. Calling this method is almost
     * identical to the user action of dragging and dropping a file on the globe.
     *
     * @param files The files to import into this application instance
     * @return CompletableFuture holding the list of results, which will be completed when this task has completed execution
     */
    CompletableFuture<List<ImportExportResult>> importFiles(@Nonnull List<File> files);

    /**
     * Request the specified exporter to be initiated. Calling this method is almost identical to the user action of
     * clicking an export button on the toolbar. Since no configuration options were provided, the export wizard UI
     * will be utilized to request the necessary details of the export from the user (i.e. what to export and where to
     * put it).
     *
     * @param family           Family of the export plugin
     * @param type             Type of the export plugin
     * @param formatIdentifier Format identifier for programmatic use (for example, "csv")
     * @return CompletableFuture holding the result, which will be completed when this task has completed execution
     * @throws NoSuchPluginException    if a plugin cannot be found for the provided family and type
     * @throws IllegalArgumentException if the plugin does not support the specified format
     * @since 2.35
     */
    CompletableFuture<ImportExportResult> export(@Nonnull String family, @Nonnull String type, String formatIdentifier);

    /**
     * Request the specified exporter to be initiated. The provided configuration options will allow the export to skip
     * any UI screens that gather information already defined in the configuration. Note: it is up to the export plugin
     * to take advantage of the provided configuration when creating its wizard panels. The configuration can be
     * accessed by calling {@link AImportExportWizard#getPreconfigurationSettings()}, assuming the wizard extends
     * the abstract base class {@link AImportExportWizard}.
     *
     * @param family           Family of the export plugin
     * @param type             Type of the export plugin
     * @param formatIdentifier Format identifier for programmatic use (for example, "csv")
     * @param configuration    The set of configuration options to provide to the export wizard
     * @param forceHeadless    Indicates if the export should be forced to run headless
     * @return CompletableFuture holding the result, which will be completed when this task has completed execution
     * @throws NoSuchPluginException    if a plugin cannot be found for the provided family and type
     * @throws IllegalArgumentException if the plugin does not support the specified format
     * @since 2.35
     */
    CompletableFuture<ImportExportResult> export(@Nonnull String family, @Nonnull String type, String formatIdentifier,
                                                 Map<String, Object> configuration, boolean forceHeadless);
}
