package gov.takx.core.view.gui.maps.importers;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.plugin.io.IExtendedImportExportWizard;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.ioservice.ImportExportService;
import gov.takx.core.ioservice.ImportWizardManager;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.core.view.gui.data.ImportExportDataDialog;
import gov.takx.core.view.gui.files.SelectByIconDialog;
import gov.takx.core.view.gui.maps.ImportPropertiesDialog;
import gov.takx.core.view.gui.maps.importers.wizards.AFileImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.FileDropImportWizard;
import gov.takx.core.view.gui.maps.importers.wizards.ImportCancelledException;
import gov.takx.core.view.gui.maps.importers.wizards.MapsforgeFileWizard;
import gov.takx.core.view.gui.maps.importers.wizards.OverlayImageImportWizard;
import gov.takx.core.view.gui.wizard.WizardProperties;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.io.ImportExportMode;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.platform.logging.NullImportExportProgressMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import static gov.takx.platform.io.panels.CommonWizardSettings.COPY_TO_VFS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class FileImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The key associated with the value that indicates which plugin family a default importer belongs to.
     */
    public static final String PREFS_PLUGIN_FAMILY_KEY = "plugin_family";

    /**
     * The key associated with the value that indicates the type of plugin a default importer belongs to.
     */
    public static final String PREFS_PLUGIN_TYPE_KEY = "plugin_type";

    /**
     * The key associated with url for the plugin's icon.
     */
    public static final String PREFS_PLUGIN_ICON_KEY = "plugin_icon_url";

    /**
     * The key for the node that has any configuration properties for a wizard.
     */
    public static final String PREFS_PRECONFIG_PROPS_KEY = "preconfig_props";

    /**
     * File count at which a user will be warned that they are trying to import a large number
     * of files that may take a very long time to pre-process and process.
     */
    private static final int HIGH_FILE_COUNT = 2500;

    @Inject
    private ImportWizardManager wizardManager;

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    @Inject
    private ImportExportService importExportService;

    @Inject
    private IUserPreferenceManager userPreferenceManager;

    @Inject
    private IUserProxy userProxy;

    @Inject
    @Headless
    private boolean headless;

    /**
     * The files to import.
     */
    private final List<File> files;

    /**
     * A reference to the VFS where the files will be placed.
     */
    private final IVirtualFileSystem fileSystem;

    /**
     * The map object that the file(s) was dropped on, if any.
     */
    private final IMapObject mapObject;

    /**
     * The location where the files were dropped. May be {@code null}.
     */
    private final ILocation location;

    /**
     * Track temporary default importers. These are importers that the user has selected with the "All" option
     * (e.g. import all CSV files with this importer now, but not always) but the importer can only import one
     * file at a time.
     */
    private final Map<String, String> tempDefaultImporters;

    private FileImporterConfigurations defaultImporterPreferences;

    /**
     * Constructor.
     *
     * @param files      The files to import.
     * @param fileSystem The VFS where the files will be placed.
     */
    public FileImporter(File[] files, IVirtualFileSystem fileSystem)
    {
        this(files, fileSystem, null);
    }

    /**
     * Constructor.
     *
     * @param files      The files to import.
     * @param fileSystem The VFS where the files will be placed.
     */
    public FileImporter(List<File> files, IVirtualFileSystem fileSystem)
    {
        this(files, fileSystem, null);
    }

    /**
     * Constructor.
     *
     * @param files      The files to import.
     * @param fileSystem The VFS where the files will be placed.
     * @param location   The location on the map where the files will be placed as file map objects.
     */
    public FileImporter(File[] files, IVirtualFileSystem fileSystem, ILocation location)
    {
        this(Arrays.asList(files), fileSystem, location);
    }

    /**
     * Constructor.
     *
     * @param files      The files to import.
     * @param fileSystem The VFS where the files will be placed.
     * @param location   The location on the map where the files will be placed as file map objects.
     */
    private FileImporter(List<File> files, IVirtualFileSystem fileSystem, ILocation location)
    {
        this(files, fileSystem, location, null);
    }

    /**
     * Constructor.
     *
     * @param files        The files to import.
     * @param fileSystem   The VFS where the files will be placed.
     * @param location     The location on the map where the files will be placed as file map objects.
     * @param mapObject    The map object that the file(s) were dropped on. May be {@code null}
     *                     if the file(s) was not dropped on a MO.
     */
    public FileImporter(List<File> files, IVirtualFileSystem fileSystem, ILocation location, IMapObject mapObject)
    {
        // Copy the list, as we need to ensure that the List implementation supports remove()
        // and we'll be removing items from it and do not want to mess with the original.
        this.files = new ArrayList<>(files);
        files.sort(new FileExtensionComparator());
        this.fileSystem = fileSystem;
        this.mapObject = mapObject;
        this.location = location;
        tempDefaultImporters = new HashMap<>();
    }

    @PostConstruct
    private void postConstruct()
    {
        defaultImporterPreferences = UserPreferenceEntry.GLOBE_IMPORTERS_NODE.getNodeAs(FileImporterConfigurations.class, true);
    }

    /**
     * Request that the import process be started. It will run on a separate thread.
     *
     * @return CompletableFuture holding the list of results, which will be completed when this task has completed execution
     */
    public CompletableFuture<List<ImportExportResult>> doImport()
    {
        return doImport(false);
    }

    /**
     * Request that the import process be started. It will run on a separate thread.
     *
     * @param forceSimpleFileImport Force all files to be dropped on the map as simple file map objects.
     * @return CompletableFuture holding the list of results, which will be completed when this task has completed execution
     */
    public CompletableFuture<List<ImportExportResult>> doImport(final boolean forceSimpleFileImport)
    {
        // Dump the whole set of operations in 'doWizardImports' onto a background thread.
        // Since the call to supplyAsync returns a CompletableFuture<CompletableFuture<List<ImportExportResult>>>,
        // we need to finalize it with a call to 'join' to remove one level of CompletableFuture.

        return CompletableFuture.supplyAsync(() -> doWizardImports(forceSimpleFileImport))
                .thenApply(CompletableFuture::join);
    }

    /**
     * Find the files that are candidates for import wizards and deal with them
     *
     * @return CompletableFuture holding the list of results, which will be completed when this task has completed execution
     */
    private CompletableFuture<List<ImportExportResult>> doWizardImports(boolean forceSimpleFileImport)
    {
        if (!confirmLargeImport(files))
        {
            return CompletableFuture.completedFuture(null);
        }

        List<ImportData> dataToImport = new ArrayList<>();

        while (!files.isEmpty())
        {
            File file = files.remove(0);
            String fileExtension = getFileExtension(file);
            List<File> filesToImport = new ArrayList<>();
            filesToImport.add(file);

            IImportWizard importWizard = forceSimpleFileImport
                    ? new FileDropImportWizard()
                    : getDefaultWizard(file);

            // Double-check the file. Some importers might do more sophisticated checks. e.g. The imagery overlay
            // importer verifies that a jpeg has location data before offering to import it.
            boolean canImportFile = !(importWizard instanceof IExtendedImportExportWizard extendedWizard)
                    || wizardManager.checkFile(extendedWizard, fileExtension, file);

            if (mapObject != null && !canImportFile)
            {
                showInvalidImportMessage(file.getName(), mapObject.getType());
            } else if (importWizard != null && canImportFile)
            {
                // Check to see if this wizard can import more than one file at a time.
                boolean multiFile = importWizard instanceof IExtendedImportExportWizard extendedWizard
                        && extendedWizard.supportsMultipleFiles();

                if (multiFile)
                {
                    new ArrayList<>(files).stream()
                            .filter(additionalFile -> wizardManager.wizardAcceptsFile(importWizard, additionalFile))
                            .forEach(additionalFile -> {
                                files.remove(additionalFile);
                                filesToImport.add(additionalFile);
                            });
                }

                // Restore any saved preferences and set the file in the pre-configuration map.
                final FileImporterConfigurations.FileImporterConfiguration defaultConfiguration = getDefaultImporterConfiguration(file);

                if (multiFile)
                {
                    preconfigureWizard(importWizard, defaultConfiguration, filesToImport);
                } else
                {
                    preconfigureWizard(importWizard, defaultConfiguration, filesToImport.get(0));
                }

                // Create the data object that will show the wizard and do the work.
                ImportData data = new ImportData(importWizard, file);
                if (!headless) data.showImporterWizardDialog();

                // Queue the data object to be executed with the user's selection.
                if (data.results != JOptionPane.CANCEL_OPTION)
                {
                    dataToImport.add(data);
                }
            } else // no pref specified... checking all the wizards to see who will import
            {
                Map<String, IImportWizard> wizards = wizardManager.getCompatibleWizards(file);
                List<UserSelectionResult> results = new ArrayList<>();

                // No wizards available to import the file(s).
                if (wizards.isEmpty())
                {
                    showNoCompatibleWizardsMessage(file.getName());
                }
                // If there's only one wizard or all of them specify override, there's no need to present options to the user.
                else if (wizards.size() == 1 || wizardManager.isOverrideWizard((IImportWizard) wizards.values().toArray()[0]))
                {
                    for (Map.Entry<String, IImportWizard> entry : wizards.entrySet())
                    {
                        results.add(new UserSelectionResult(entry.getKey(), entry.getValue(), SelectByIconDialog.SelectionType.all, null));
                    }
                }
                // If there's more than one wizard available, and they are standard non-override wizards, query the user.
                else
                {
                    if (headless)
                    {
                        logger.error("Could not determine the correct importer to use for the file {} because app is running headless.  To import this file in headless mode an importer needs to be specified", file.getName());
                    } else
                    {
                        var result = showImportOptions(wizards, file, files);
                        if (result != null) results.add(result);
                    }
                }

                // The result could be null if the user cancels the selection.
                if (!results.isEmpty())
                {
                    for (UserSelectionResult result : results)
                    {
                        // Check to see if this wizard can import more than one file at a time.
                        boolean multiFile = result.wizard instanceof IExtendedImportExportWizard extendedWizard
                                && extendedWizard.supportsMultipleFiles();

                        // Persist the "Always" choice.
                        if (result.selectionType == SelectByIconDialog.SelectionType.always)
                        {
                            updateDefaultImporterPreference(result.uniqueId, file, result.iconURL);
                        }

                        // If "Always" or "All" was selected, scan the rest of the files to see if any others qualify.
                        if (result.selectionType == SelectByIconDialog.SelectionType.always
                                || result.selectionType == SelectByIconDialog.SelectionType.all)
                        {
                            if (multiFile)
                            {
                                new ArrayList<>(files).stream()
                                        .filter(additionalFile -> wizardManager.wizardAcceptsFile(result.wizard, additionalFile))
                                        .forEach(additionalFile -> {
                                            files.remove(additionalFile);
                                            filesToImport.add(additionalFile);
                                        });
                            } else if (result.selectionType == SelectByIconDialog.SelectionType.all)
                            {
                                tempDefaultImporters.put(fileExtension, result.uniqueId);
                            }
                        }

                        // Up to here, we've been dealing with a reusable, static wizard used to check compatibility.
                        // Get a fresh wizard before configuring.
                        var newWizard = getNewWizard(KeyUtils.getFamilyFromKey(result.uniqueId), KeyUtils.getTypeFromKey(result.uniqueId));

                        if (multiFile)
                        {
                            preconfigureWizard(newWizard, null, filesToImport);
                        } else
                        {
                            preconfigureWizard(newWizard, null, filesToImport.get(0));
                        }

                        ImportData importData = new ImportData(newWizard, file);
                        if (!headless) importData.showImporterWizardDialog();

                        if (importData.results != JOptionPane.CANCEL_OPTION)
                        {
                            dataToImport.add(importData);
                        }
                    }
                }
            }
        }

        // When the user imports imagery content we should ask them if they want to copy the imagery source to the VFS.
        if (!headless && dataToImport.stream().anyMatch(i -> i.wizard instanceof MapsforgeFileWizard || i.wizard instanceof OverlayImageImportWizard))
        {
            final var dialog = new ImportPropertiesDialog();
            dialog.setVisible(true);

            // User cancelled? Abort import.
            if (dialog.isCancelled()) return CompletableFuture.completedFuture(Collections.emptyList());

            // Fill properties from the dialog into all imported contents.
            for (ImportData importData : dataToImport)
            {
                Map<String, Object> props = importData.preconfigProps;
                if (props == null) props = importData.preconfigProps = new HashMap<>();
                props.put(COPY_TO_VFS, dialog.isCopyToVfs());
            }
        }

        // Now that we are done interacting with the user we can start running everything
        final List<CompletableFuture<ImportExportResult>> operations = dataToImport.stream().map(ImportData::runOperation).toList();

        CompletableFuture<Void> allOperations = CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]));

        return allOperations.thenApply(operation -> operations.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    /**
     * Initialize the wizard with any pre-configuration options that may allow the user to skip some steps.
     *
     * @param wizard               The wizard to update.
     * @param defaultConfiguration The configuration options retrieved from the Preferences.
     * @param file                 The file to import if the importer does not support multi-file import.
     */
    private void preconfigureWizard(IImportWizard wizard, FileImporterConfigurations.FileImporterConfiguration defaultConfiguration, File file)
    {
        preconfigureWizard(wizard, defaultConfiguration, file, null);
    }

    /**
     * Initialize the wizard with any pre-configuration options that may allow the user to skip some steps.
     *
     * @param wizard               The wizard to update.
     * @param defaultConfiguration The configuration options retrieved from the Preferences.
     * @param files                The files to import if the importer does support multi-file import.
     */
    private void preconfigureWizard(IImportWizard wizard, FileImporterConfigurations.FileImporterConfiguration defaultConfiguration, List<File> files)
    {
        preconfigureWizard(wizard, defaultConfiguration, null, files);
    }

    /**
     * Initialize the wizard with any pre-configuration options that may allow the user to skip some steps.
     *
     * @param wizard               The wizard to update.
     * @param defaultConfiguration The configuration options retrieved from the Preferences.
     * @param file                 The file to import if the importer does not support multi-file import.
     * @param files                The files to import if the importer does support multi-file import.
     */
    private void preconfigureWizard(IImportWizard wizard, FileImporterConfigurations.FileImporterConfiguration defaultConfiguration, File file, List<File> files)
    {
        final Map<String, Object> preConfigurationProperties = defaultConfiguration != null
                ? new HashMap<>(defaultConfiguration.getProperties())
                : new HashMap<>();

        if (file != null)
        {
            preConfigurationProperties.put(CommonWizardSettings.FILE, file);
        } else
        {
            preConfigurationProperties.put(CommonWizardSettings.FILES, files);
        }

        preConfigurationProperties.put(CommonWizardSettings.LOCATION, location);
        preConfigurationProperties.put(CommonWizardSettings.VFS, fileSystem);

        if (mapObject != null)
        {
            preConfigurationProperties.put(CommonWizardSettings.MAP_OBJECT_FAMILY, mapObject.getFamily());
            preConfigurationProperties.put(CommonWizardSettings.MAP_OBJECT_TYPE, mapObject.getType());
            preConfigurationProperties.put(CommonWizardSettings.UNIT_ID, mapObject.getUnitID());
        }

        wizard.preconfigureWizard(preConfigurationProperties);
    }

    /**
     * Validate that a list of files scheduled to be attached actually exist on disk.  Should be called
     * from the EDT.
     *
     * @param attachments files to attach to check validity of
     * @return list of valid files, found on disk.
     */
    public static List<File> validateAttachmentFiles(File[] attachments)
    {
        List<File> invalidFiles = new ArrayList<>();
        List<File> validFiles = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        for (File file : attachments)
        {
            if (!file.exists())
            {
                invalidFiles.add(file);
                if (!sb.isEmpty()) sb.append("\n");
                sb.append("            ");
                sb.append(file.getName());
            } else
            {
                validFiles.add(file);
            }
        }

        if (!invalidFiles.isEmpty())
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Error:  The following files are invalid and will be skipped:\n\n" + sb, "File Error");
        }
        return validFiles;
    }

    private void showNoCompatibleWizardsMessage(String fileName)
    {
        userProxy.reportMessage("No Compatible Importers", "No importer found to import " + fileName + ".   ");
    }

    /**
     * Display the set of available importers to the user and allow them to choose which one to use with the option
     * to make it the default.
     *
     * @param wizards The available wizards.
     * @param file    The file to import.
     * @param remainingFiles The remaining files beyond the explicitly provided one.
     * @return  The user's selection, or {@code null} if none
     */
    @InvokeAndWaitOnEDT
    @Nullable
    private UserSelectionResult showImportOptions(Map<String, IImportWizard> wizards, File file, List<File> remainingFiles)
    {
        boolean multiFile = !remainingFiles.isEmpty();
        boolean isDirectory = file.isDirectory();
        String remainingSuffix = multiFile ? " (+ " + remainingFiles.size() + " other" + (remainingFiles.size() > 1 ? "s" : "") + ")" : "";
        String title = "Import " + file.getName() + remainingSuffix + " using";
        SelectByIconDialog dialog = new SelectByIconDialog(
                JFrame.getFrames()[0],
                title,
                multiFile && !isDirectory,
                !isDirectory);

        final List<Map.Entry<String, IImportWizard>> sortedWizards = sortWizards(wizards);

        for (Map.Entry<String, IImportWizard> entry : sortedWizards)
        {
            IImportWizard wizard = entry.getValue();

            if (wizard instanceof IExtendedImportExportWizard extendedImportExportWizard)
            {
                dialog.addEntry(
                        entry.getKey(),
                        extendedImportExportWizard.getDisplayName(),
                        extendedImportExportWizard.getHelpMessage(),
                        extendedImportExportWizard.getIcon(),
                        wizard);
            }
            // Support old style plugins that don't explicitly provide an icon, name or help message.
            else
            {
                String uniqueKey = entry.getKey();
                String family = KeyUtils.getFamilyFromKey(uniqueKey);
                String type = KeyUtils.getTypeFromKey(uniqueKey);

                // Try to extract an icon.
                final URL iconURL = pluginRegistryManager.getImportExportPluginRegistry().getPluginDefinition(family, type)
                        .flatMap(pluginDefinition -> pluginDefinition.getIconPath()
                                .map(path -> pluginDefinition.getResourceClassLoader().getResource(path)))
                        .orElse(null);

                dialog.addEntry(uniqueKey, type, null, iconURL, wizard);
            }
        }

        dialog.setVisible(true);

        final SelectByIconDialog.IconEntry entry = dialog.getResult();

        return entry != null
                ? new UserSelectionResult(entry.uniqueId(), entry.wizard(), dialog.getSelectionType(), entry.iconURL().toString())
                : null;
    }

    /**
     * Sort the wizards alphabetically.
     *
     * @param wizards The available wizards.
     * @return List of sorted wizards
     **/
    private List<Map.Entry<String, IImportWizard>> sortWizards(Map<String, IImportWizard> wizards)
    {
        final List<Map.Entry<String, IImportWizard>> entries = new ArrayList<>(wizards.entrySet());

        // Sort the list based on display name (defaulting back to the wizard's unique key)
        entries.sort((o1, o2) -> {
            final String o1Key = o1.getValue() instanceof IExtendedImportExportWizard extendedWizard
                    ? extendedWizard.getDisplayName()
                    : o1.getKey();

            final String o2Key = o2.getValue() instanceof IExtendedImportExportWizard extendedWizard
                    ? extendedWizard.getDisplayName()
                    : o2.getKey();

            return o1Key.compareTo(o2Key);
        });

        return entries;
    }

    /**
     * Data structure to capture the user's dialog selection result.
     */
    private record UserSelectionResult(String uniqueId, IImportWizard wizard, SelectByIconDialog.SelectionType selectionType, String iconURL)
    {
    }

    /**
     * Use the preferences to find the default importer if one exists.
     *
     * @param file The file to be imported.
     * @return The default importer or {@code null} if one does not exist.
     */
    private IImportWizard getDefaultWizard(File file)
    {
        String fileExtension = getFileExtension(file);

        // Check to see if the file was dropped on a map object and if the map object has specified an importer to use.
        if (mapObject != null)
        {
            final IImportWizard importer = getPluginSpecifiedImporter(file);

            if (importer != null) return importer;
        }

        // Check to see if the user has used the "All" options to import all related files with the same importer.
        final String tempDefaultWizardKey = tempDefaultImporters.get(fileExtension);
        if (tempDefaultWizardKey != null)
        {
            final IImportWizard tempDefaultWizard = wizardManager.getImportWizard(tempDefaultWizardKey);
            if (tempDefaultWizard != null && wizardManager.wizardAcceptsFile(tempDefaultWizard, file))
            {
                return getNewWizard(KeyUtils.getFamilyFromKey(tempDefaultWizardKey), KeyUtils.getTypeFromKey(tempDefaultWizardKey));
            }
        }

        // Otherwise, check the persisted preferences.
        final FileImporterConfigurations.FileImporterConfiguration defaultConfiguration = getDefaultImporterConfiguration(file);

        return defaultConfiguration != null
                ? getNewWizard(defaultConfiguration.getFamily(), defaultConfiguration.getType())
                : null;
    }

    /**
     * Get the importer specified in the plugin definition, if any.  This also validates that the specified
     * wizard can import the file being processed.
     *
     * @param file File being processed
     * @return Importer to use, if specified and the wizard claims it can process the file
     * @see ImportWizardManager#wizardAcceptsFile(IImportWizard, File)
     * @since 3.0
     */
    private IImportWizard getPluginSpecifiedImporter(File file)
    {
        if (mapObject.getPluginDefinition() instanceof IDevicePluginDefinition devicePluginDefinition)
        {
            final String importerKey = devicePluginDefinition.getFileImporterKey();

            if (importerKey != null)
            {
                IImportWizard wizard = wizardManager.getImportWizard(importerKey);

                if (wizard == null)
                {
                    logger.error("The map object specified an importer that is unavailable: {}", importerKey);
                } else if (!wizardManager.wizardAcceptsFile(wizard, file))
                {
                    logger.warn("The importer specified by the map object can not process the file. Using default import process.");
                } else
                {
                    final String family = KeyUtils.getFamilyFromKey(importerKey);
                    final String type = KeyUtils.getTypeFromKey(importerKey);

                    return getNewWizard(family, type);
                }
            } else
            {
                logger.warn("The map object does not specify an importer. Using default import process.");
            }
        }

        return null;
    }

    /**
     * Look up the default importer configuration for the given file (by file extension).
     *
     * @param file The file to import.
     * @return The default importer's configuration or {@code null} if one does not exist.
     */
    private FileImporterConfigurations.FileImporterConfiguration getDefaultImporterConfiguration(File file)
    {
        return defaultImporterPreferences.getImporterConfiguration(getFileExtension(file));
    }

    /**
     * Get the file extension, substituting an empty String for {@code null}
     *
     * @param f The file.
     * @return The non-null file extension.
     */
    private String getFileExtension(File f)
    {
        String fileExtension = FileUtils.getExtension(f);
        return fileExtension == null ? "" : fileExtension;
    }

    /**
     * Update an importer as being the default importer for a specific file type.
     *
     * @param key     The importer key.
     * @param file    The file being imported when it was set to be (or not to be) the default.
     * @param iconURL The url to the icon used for the importer.
     */
    private void updateDefaultImporterPreference(String key, File file, String iconURL)
    {
        defaultImporterPreferences.setImporterConfiguration(key, getFileExtension(file), iconURL);
        userPreferenceManager.savePreferences();
    }

    /**
     * Get the wizard for the specified family and type.
     *
     * @param family The wizard family.
     * @param type   The wizard type.
     * @return The wizard associated with the family and type or {@code null} if one does not exist.
     */
    private IImportWizard getNewWizard(String family, String type)
    {
        if (family == null || type == null)
        {
            return null;
        } else
        {
            IImportWizard wizard = getNewInternalWizard(family, type);

            if (wizard == null)
            {
                IImportExportPlugin plugin = pluginRegistryManager.getImportExportPluginRegistry().getPlugin(family, type);

                if (plugin != null)
                {
                    wizard = plugin.createImporter();

                    if (wizard != null)
                    {
                        importExportService.injectWizardFormatsAndDelegate(plugin, wizard);
                    }
                }
            }

            return wizard;
        }
    }

    /**
     * Get a new instance of an internal wizard by reflection.
     *
     * @param family The wizard's family.
     * @param type   The wizard's type.
     * @return A new wizard, or {@code null} if one does not exist for the given family/type pair or if an error occurred.
     */
    private IImportWizard getNewInternalWizard(String family, String type)
    {
        IImportWizard result = null;

        if (family.equalsIgnoreCase(AFileImportWizard.FAMILY))
        {
            IImportWizard wizard = wizardManager.getImportWizard(family, type);

            if (wizard != null)
            {
                try
                {
                    result = wizard.getClass().getConstructor().newInstance();
                } catch (ReflectiveOperationException e)
                {
                    logger.error("Error instantiating internal wizard via reflection.", e);
                }
            }
        }

        return result;
    }

    /**
     * Display a message indicating that an import target (e.g. a map object on which a file was dropped at the end of a
     * drag-and-drop action) cannot accept a file.
     *
     * @param fileName The file name.
     * @param target   The target of the import.
     */
    private void showInvalidImportMessage(final String fileName, final String target)
    {
        userProxy.reportWarning("Invalid File", target + " cannot import the file " + fileName);
    }

    /**
     * Checks to see if the import is large enough to warrant a confirmation from the user, and returns false
     * if and only if this condition is true and the user selects to cancel.
     *
     * @param files files to count
     * @return false on user cancellation, true otherwise.
     */
    private boolean confirmLargeImport(List<File> files)
    {
        final int count = FileUtils.countFiles(files, HIGH_FILE_COUNT);
        if (count >= HIGH_FILE_COUNT)
        {
            final Object[] options = {"Yes", "No"};

            // Show option dialog on EDT and wait for result.
            FutureTask<Integer> dialogTask = new FutureTask<>(() -> AlwaysOnTopJOptionPane.showOptionDialog(JFrame.getFrames()[0],
                    "Warning!  You have selected over " + count + " files to be inspected for import.\n" +
                            "This may take a long time and cause the application to become temporarily\n" +
                            "unresponsive. Would you like to continue?",
                    "Continue?",
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options[0]));

            SwingUtilities.invokeLater(dialogTask);

            try
            {
                // User said "No".
                if (dialogTask.get() == 1)
                {
                    return false;
                }
            } catch (Exception e)
            {
                logger.error("Failed to obtain result from user. Proceeding with import process.", e);
            }
        }

        return true;
    }

    /**
     * This class is mostly being used as a data passing object for dialogs that are being invoked on the EDT
     */
    private static class ImportData
    {
        private final IImportWizard wizard;
        private final File filepath;
        private int results;
        private Map<String, Object> preconfigProps;
        private ImportExportDataDialog dialog;

        ImportData(IImportWizard wizard, File file)
        {
            this.wizard = wizard;
            filepath = file;
        }

        /**
         * Run the import operation.
         *
         * @return CompletableFuture holding the result, which will be completed when this task has completed execution
         */
        public CompletableFuture<ImportExportResult> runOperation()
        {
            if (dialog != null)
            {
                return dialog.runOperation();
            } else if (wizard instanceof AFileImportWizard)
            {
                try
                {
                    ((AFileImportWizard) wizard).runOperation(preconfigProps);

                    // File imports don't produce ImportExportResults, so we just return a null
                    return CompletableFuture.completedFuture(null);
                } catch (Exception e)
                {
                    logger.error("Import failed", e);
                    return CompletableFuture.failedFuture(e);
                }
            } else
            {
                final CompletableFuture<ImportExportResult> future = new CompletableFuture<>();
                try
                {
                    final ImportExportResult result = wizard.doImport(new WizardProperties(preconfigProps),
                            new NullImportExportProgressMonitor());
                    future.complete(result);
                } catch (Exception e)
                {
                    logger.error("Import failed", e);
                    future.completeExceptionally(e);
                }
                return future;
            }
        }

        private void setDialog(ImportExportDataDialog dialog)
        {
            this.dialog = dialog;
        }

        @InvokeAndWaitOnEDT
        private void showImporterWizardDialog()
        {
            if (wizard instanceof AFileImportWizard)
            {
                try
                {
                    preconfigProps = ((AFileImportWizard) wizard).showImporterWizardDialog();
                    results = JOptionPane.YES_OPTION;
                } catch (ImportCancelledException e)
                {
                    results = JOptionPane.CANCEL_OPTION;
                }
            } else
            {
                results = JOptionPane.YES_OPTION;
                Format format = null;
                String extension = FileUtils.getExtension(filepath);
                Iterator<Format> iterator = wizard.getFormats().iterator();
                while (iterator.hasNext() && format == null)
                {
                    Format f = iterator.next();
                    for (String t : f.getFileTypes())
                    {
                        if (t.equalsIgnoreCase(extension))
                        {
                            format = f;
                            break;
                        }
                    }
                }

                ImportExportDataDialog dialog = ImportExportDataDialog.dialogFor(wizard, ImportExportMode.IMPORT, format, null, false);

                if (dialog != null)
                {
                    setDialog(dialog);
                    if (dialog.getCurrentPanel() != null)
                    {
                        ComponentUtils.centerComponentOnScreen(dialog);
                        dialog.setVisible(true);
                        results = dialog.getReturnCode();
                        preconfigProps = dialog.getPreconfigureProperties();
                    }
                }
            }
        }
    }

    /**
     * Sort files by file extension.
     */
    private class FileExtensionComparator implements Comparator<File>
    {
        @Override
        public int compare(File file1, File file2)
        {
            return getFileExtension(file1).compareTo(getFileExtension(file2));
        }
    }
}
