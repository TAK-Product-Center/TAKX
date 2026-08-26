package gov.takx.core.ioservice;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.io.Format;
import gov.takx.api.io.IImportExportService;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.registry.IImportExportPluginRegistry;
import gov.takx.api.registry.NoSuchPluginException;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.core.view.gui.data.ImportExportDataDialog;
import gov.takx.core.view.gui.maps.importers.FileImporter;
import gov.takx.core.view.gui.wizard.WizardProperties;
import gov.takx.platform.io.ImportExportMode;
import gov.takx.platform.logging.NullImportExportProgressMonitor;
import gov.takx.platform.utils.ClassLoaderUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provides functionality to plugins related to import and export plugins, including kicking off file imports and exports.
 *
 * @since 2.30
 */
@ApplicationScoped
public class ImportExportService implements IImportExportService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IImportExportPluginRegistry importExportPluginRegistry;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;
    private IVirtualFileSystem fileSystem;

    @PostConstruct
    private void postConstruct()
    {
        fileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
    }

    /**
     * Create an importer from the import/export plugin of the provided family and type.
     *
     * @param family The plugin family
     * @param type   The plugin type
     * @return The import wizard.
     * @throws IllegalArgumentException if the plugin does not support import or an error occurred creating the importer.
     * @since 2.35
     */
    public IImportWizard createImporter(String family, String type)
    {
        IImportExportPlugin plugin = getImportExportPlugin(family, type);
        return (IImportWizard) createWizard(plugin, ImportExportMode.IMPORT);
    }

    /**
     * Retrieves an import/export plugin of the specified family and type from the plugin registry.
     *
     * @param family The plugin family
     * @param type   The plugin type
     * @return The import/export plugin
     * @throws NoSuchPluginException if a plugin cannot be found for the provided family and type
     * @since 2.35
     */
    private IImportExportPlugin getImportExportPlugin(@Nonnull String family, @Nonnull String type)
    {
        IImportExportPlugin plugin = importExportPluginRegistry.getPlugin(family, type);

        if (plugin == null)
        {
            throw new NoSuchPluginException("Plugin does not exist with family " + family + ", type " + type);
        }

        return plugin;
    }

    /**
     * Create a wizard of the specified type from the provided plugin.
     *
     * @param plugin The import/export plugin
     * @return The wizard.
     * @throws IllegalArgumentException if the plugin does not support the mode or an error occurred creating the wizard.
     * @since 2.35
     */
    private IImportExportWizard createWizard(IImportExportPlugin plugin, ImportExportMode mode)
    {
        IImportExportWizard wizard = mode.getWizardCreator().apply(plugin);

        if (wizard == null)
        {
            throw new IllegalArgumentException("Unable to create a wizard from plugin " + plugin + " for mode " + mode);
        }

        injectWizardFormatsAndDelegate(plugin, wizard);

        return wizard;
    }

    /**
     * Injects the delegate and the formats declared in the plugin annotation into the provided wizard.
     *
     * @param plugin I/O plugin
     * @param wizard wizard
     * @since 2.35
     */
    public void injectWizardFormatsAndDelegate(IImportExportPlugin plugin, IImportExportWizard wizard)
    {
        wizard.setFormats(plugin.getFormats());
        wizard.setDelegate(new ImportExportPluginDelegate(plugin));
    }

    /**
     * Create an exporter from the provided plugin.
     *
     * @param plugin The import/export plugin
     * @return The export wizard.
     * @throws IllegalArgumentException if the plugin does not support export or an error occurred creating the exporter.
     * @since 2.35
     */
    private IExportWizard createExporter(IImportExportPlugin plugin)
    {
        return (IExportWizard) createWizard(plugin, ImportExportMode.EXPORT);
    }

    @Override
    public CompletableFuture<List<ImportExportResult>> importFiles(@Nonnull List<File> files)
    {
        if (files.isEmpty())
        {
            logger.warn("The provided List of files to import is empty.");
            return CompletableFuture.completedFuture(null);
        }

        return new FileImporter(files, fileSystem).doImport();
    }

    @Override
    public CompletableFuture<ImportExportResult> export(@Nonnull String family, @Nonnull String type, String formatIdentifier)
    {
        return export(family, type, formatIdentifier, Collections.emptyMap(), false);
    }

    @Override
    public CompletableFuture<ImportExportResult> export(@Nonnull String family, @Nonnull String type, String formatIdentifier,
                                                        Map<String, Object> configuration, boolean forceHeadless)
    {
        IImportExportPlugin plugin = getImportExportPlugin(family, type);
        IExportWizard exportWizard = createExporter(plugin);

        List<Format> supportedFormats = plugin.getFormats(); // Get the formats from the plugin.

        Optional<Format> format = supportedFormats.stream()
                .filter(f -> f.getIdentifier().equals(formatIdentifier))
                .findFirst();

        if (format.isEmpty())
        {
            throw new IllegalArgumentException("Plugin does not support the specified format; family " + family +
                    ", type " + type + ", format " + formatIdentifier);
        }

        exportWizard.preconfigureWizard(configuration);

        if (!headless && !forceHeadless)
        {
            ExportData exportData = new ExportData(exportWizard, format.get());
            exportData.launchImportExportDialog();
            return exportData.runOperation();
        } else
        {
            // Running headless, so directly invoke the wizard
            final CompletableFuture<ImportExportResult> future = new CompletableFuture<>();
            try
            {
                // Copy all of the configuration into the wizard properties, since many plugins ignore the preconfiguration properties
                IWizardProperties properties = new WizardProperties();
                configuration.forEach(properties::setProperty);
                final ImportExportResult result = exportWizard.doExport(properties, new NullImportExportProgressMonitor());
                future.complete(result);
            } catch (Exception e)
            {
                logger.error("Export failed", e);
                future.completeExceptionally(e);
            }

            return future;
        }
    }

    /**
     * This class is mostly being used as a data passing object for dialogs that are being invoked on the EDT.
     *
     * @since 2.35
     */
    private static class ExportData
    {
        private final IExportWizard wizard;
        private final Format format;
        private int results = 0;
        private ImportExportDataDialog dialog;

        private ExportData(IExportWizard wizard, Format format)
        {
            this.wizard = wizard;
            this.format = format;
        }

        /**
         * Launch a dialog to allow the user to configure and start an export.
         */
        @InvokeAndWaitOnEDT
        public void launchImportExportDialog()
        {
            // Passing doRun = false when creating the dialog will cause it to not actually execute the import/export
            // operation after the UI wizard is complete. This will allow us to execute the operation separately so we can
            // get access to a future.
            dialog = ImportExportDataDialog.dialogFor(wizard, ImportExportMode.EXPORT, format, null, false);
            if (dialog != null)
            {
                ComponentUtils.centerComponentOnScreen(dialog);

                // Ensure that the proper plugin class loader is pushed
                ClassLoaderUtil.pushContextClassLoader(wizard.getClass().getClassLoader());
                try
                {
                    dialog.setVisible(true);
                    results = dialog.getReturnCode();
                } finally
                {
                    ClassLoaderUtil.popContextClassLoader();
                }
            }
        }

        private boolean isCancelled()
        {
            return results == JOptionPane.CANCEL_OPTION;
        }

        /**
         * Run the export operation.
         *
         * @return CompletableFuture holding the result, which will be completed when this task has completed execution
         */
        public CompletableFuture<ImportExportResult> runOperation()
        {
            if (dialog != null && !isCancelled())
            {
                return dialog.runOperation();
            }

            return CompletableFuture.completedFuture(null);
        }
    }
}
