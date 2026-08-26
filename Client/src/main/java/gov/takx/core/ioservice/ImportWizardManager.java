package gov.takx.core.ioservice;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.io.Format;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.io.IExtendedImportExportWizard;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.registry.IImportExportPluginRegistry;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.maps.importers.wizards.AFileImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.CustomMobacLayerWizard;
import gov.takx.core.view.gui.maps.importers.wizards.DataPackageImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.ElevationMapImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.FileDropImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.ImageryMapImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.KMLMapObjectWizard;
import gov.takx.core.view.gui.maps.importers.wizards.KMLOverlayWizard;
import gov.takx.core.view.gui.maps.importers.wizards.MapsforgeFileWizard;
import gov.takx.core.view.gui.maps.importers.wizards.ModelMapImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.OverlayImageImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.PlacemarkOverlayWizard;
import gov.takx.core.view.gui.maps.importers.wizards.ShapeOverlayWizard;
import gov.takx.core.view.gui.maps.importers.wizards.TakPreferencesImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.TakServerImportWizard;
import gov.takx.core.view.gui.media.MediaImportWizard;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Application service to manage the collection of import wizards provided internally and from plugins.  The collection
 * of wizards is regenerated for each project to avoid keeping plugin references across project boundaries (since the
 * CDI containers are rebuilt as well).
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class ImportWizardManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IImportExportPluginRegistry importExportPluginRegistry;

    @Inject
    private ImportExportService importExportService;

    /**
     * A comprehensive map of all family/type unique keys to wizards combinations.
     */
    private final Map<String, IImportWizard> wizards = new HashMap<>();

    /**
     * Crank up our service on the project.  Load all the import wizards.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices event)
    {
        wizards.clear();

        // Internal wizards that won't show up in the plugin registry. A no argument constructor is required for all
        // internal wizards as they may be instantiated via reflection.
        registerFileImportWizard(new KMLMapObjectWizard());
        registerFileImportWizard(new KMLOverlayWizard());
        registerFileImportWizard(new MapsforgeFileWizard());
        registerFileImportWizard(new CustomMobacLayerWizard());
        registerFileImportWizard(new ImageryMapImportWizard());
        registerFileImportWizard(new ElevationMapImportWizard());
        registerFileImportWizard(new ModelMapImportWizard());
        registerFileImportWizard(new ShapeOverlayWizard());
        registerFileImportWizard(new OverlayImageImportWizard());
        registerFileImportWizard(new PlacemarkOverlayWizard());
        registerFileImportWizard(new DataPackageImportWizard());
        registerFileImportWizard(new TakServerImportWizard());
        registerFileImportWizard(new TakPreferencesImportWizard());
        registerFileImportWizard(new MediaImportWizard());
        registerFileImportWizard(new FileDropImportWizard());

        // Plugin wizards.
        for (IImportExportPlugin plugin : importExportPluginRegistry.getPlugins())
        {
            final String key = plugin.getUniqueKey();
            logger.debug("Create importer for plugin: {}", key);

            try
            {
                final IImportWizard importer = plugin.createImporter();

                if (importer != null)
                {
                    importExportService.injectWizardFormatsAndDelegate(plugin, importer);
                    wizards.put(key, importer);
                } else
                {
                    logger.debug("Plugin returned NULL from createImporter call: {}", key);
                }
            } catch (Throwable e)
            {
                logger.error("Failed to create importer for plugin: {}", key, e);
            }
        }
    }

    /**
     * Register an internal file import wizard.
     * @since 3.2
     */
    private void registerFileImportWizard(AFileImportWizard importWizard)
    {
        wizards.put(importWizard.getKey(), importWizard);
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        wizards.clear();
    }

    /**
     * Get the import wizard for the given family/type.
     *
     * @param family plugin family
     * @param type   plugin type
     * @return Import wizard registered for that family/type, or {@code null} if not found
     */
    public IImportWizard getImportWizard(String family, String type)
    {
        return getImportWizard(KeyUtils.getUniqueKey(family, type));
    }

    /**
     * Get the import wizard for the given plugin unique key (family and type).
     *
     * @param key unique key for plugin (family/type)
     * @return Import wizard registered for that key, or {@code null} if not found
     */
    public IImportWizard getImportWizard(String key)
    {
        return wizards.get(key);
    }

    /**
     * Get the displayable name associated with a wizard.
     *
     * @param family The wizard's family
     * @param type   The wizard's type
     * @return The wizard's name or {@code null} if no wizard was found for the given family and type
     */
    public String getWizardName(String family, String type)
    {
        final IImportWizard wizard = getImportWizard(family, type);

        return wizard instanceof IExtendedImportExportWizard extendedWizard ? extendedWizard.getDisplayName() : type;
    }

    /**
     * Find all wizards that are offering to import the file. Note that there may be 1 or more wizards that declare
     * that they override any of the stock / default TAKX importers, or other importers that declare they
     * don't override that authority. The ones that declare override get precedence.
     *
     * @param file The file to import.
     * @return The collection of import wizards that are offering to import the file.
     */
    public Map<String, IImportWizard> getCompatibleWizards(File file)
    {
        Map<String, IImportWizard> compatibleWizards = new HashMap<>();
        Map<String, IImportWizard> overrideWizards = new HashMap<>();

        wizards.entrySet().stream()
                .filter(entry -> wizardAcceptsFile(entry.getValue(), file))
                .forEach(entry -> compatibleWizards.put(entry.getKey(), entry.getValue()));

        compatibleWizards.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof IExtendedImportExportWizard extendedWizard &&
                        extendedWizard.overrideDefaultImporters())
                .forEach(entry -> overrideWizards.put(entry.getKey(), entry.getValue()));

        return !overrideWizards.isEmpty() ? overrideWizards : compatibleWizards;
    }

    /**
     * Return whether this is an override type import wizard.  To be so, it must implement the extended
     * interface and return true for the override method.
     *
     * @param wizard the import wizard to test
     * @return {@code true} if an override wizard, {@code false} otherwise
     */
    public boolean isOverrideWizard(IImportWizard wizard)
    {
        return wizard instanceof IExtendedImportExportWizard extendedWizard && extendedWizard.overrideDefaultImporters();
    }

    /**
     * Check to see if a wizard wants to offer to process a file. Older wizards may not extend
     * {@link IExtendedImportExportWizard}, so we will also have to do a file extension check.
     *
     * @param wizard The wizard
     * @param file   The file to check
     * @return {@code true} iff the wizard is offering to import the file
     */
    public boolean wizardAcceptsFile(IImportWizard wizard, File file)
    {
        String fileExtension = getFileExtension(file);

        if (wizard instanceof IExtendedImportExportWizard extendedWizard)
        {
            return checkFile(extendedWizard, fileExtension, file);
        }
        // We still have to support wizards that don't implement IExtendedImportExportWizard.
        else
        {
            for (Format format : wizard.getFormats())
            {
                for (String fileExt : format.getFileTypes())
                {
                    if (fileExtension.equalsIgnoreCase(fileExt))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check to see if a wizard wants to offer to import a file. If the wizard takes a second or more to inspect the
     * file, log a warning.
     *
     * @param wizard        The wizard
     * @param fileExtension The file extension
     * @param file          The file
     * @return {@code true} iff the wizard is offering to import the file.
     */
    public boolean checkFile(IExtendedImportExportWizard wizard, String fileExtension, File file)
    {
        boolean result = false;
        long start = System.currentTimeMillis();
        try
        {
            result = wizard.offerToProcessFile(fileExtension, file);
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ":  Exception accessing Importer of type {}", wizard.getClass(), e);
        }
        long elapsed = System.currentTimeMillis() - start;

        if (elapsed >= 1_000)
        {
            logger.warn("{}", String.format("The %s wizard took %.1f seconds to inspect file %s", wizard.getDisplayName(),
                    (elapsed / 1000.0), file.getName()));
        }

        return result;
    }

    /**
     * Get the file extension, substituting an empty String for {@code null}
     *
     * @param f The file
     * @return The non-null file extension
     */
    private String getFileExtension(File f)
    {
        String fileExtension = FileUtils.getExtension(f);
        return fileExtension == null ? "" : fileExtension;
    }
}
