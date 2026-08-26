package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.io.Format;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportPluginDefinition;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.registry.IImportExportPluginRegistry;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.ioservice.ImportExportService;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.data.ImportExportDataDialog;
import gov.takx.platform.io.ImportExportMode;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.ClassLoaderUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.FEATURE_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.STANDARD_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.TOOLBAR_BUTTON_IMAGE_SIZE;

/**
 */
@PseudoBean
public class DataToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final URL IMPORT_OVERLAY_URL = UIResources.getUrl(PLATFORM_ICONS, "import.png");
    private static final URL EXPORT_OVERLAY_URL = UIResources.getUrl(PLATFORM_ICONS, "export.png");

    @Inject
    private IImportExportPluginRegistry importExportPluginRegistry;

    @Inject
    private ImportExportService importExportService;

    private boolean componentsAdded;

    @PostConstruct
    private void postConstruct()
    {
        createTab(TabType.DATA, true);
    }

    /**
     * This toolbar is a bit unique in that it needs to create instances of the import/export plugins in order
     * to obtain their format list.  So, we have to wait until plugin containers are all created and plugins
     * are being started to finalize our toolbar contents.
     */
    private void finalizeToolbar(@Observes @Priority(ServiceOrdering.START_ServicesStarted) StartProjectServices ignored)
    {
        if (!componentsAdded)
        {
            createToolbarButtons(importExportPluginRegistry.getPluginDefinitions());
            componentsAdded = true;
        }
    }

    /**
     * Create buttons for the given list of {@code pluginDefinitions}.
     * @since 5.7
     */
    private void createToolbarButtons(Collection<IImportExportPluginDefinition> pluginDefinitions)
    {
        // Create buttons for all importers
        addButtonsToToolbar(pluginDefinitions, ButtonType.IMPORT_PLUGIN, ImportExportMode.IMPORT);

        tab.addComponent(new ToolbarSeparator(TabType.DATA, tab.getComponents().size()));

        // Create buttons for all exporters
        addButtonsToToolbar(pluginDefinitions, ButtonType.EXPORT_PLUGIN, ImportExportMode.EXPORT);
    }

    /**
     * Handle hot deployed plugins. Add buttons if it's an import/export plugin.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        // Ignore this event unless we've already started and created the initial set of buttons
        if (componentsAdded && pluginAdded.getPluginCategory() == PluginCategory.ImportExportPlugin)
        {
            createToolbarButtons(List.of((IImportExportPluginDefinition) pluginAdded.pluginDefinition()));
        }
    }

    @Override
    protected void addComponents()
    {
        // Do nothing now, wait until StartPlugins is fired
    }

    /**
     * Add buttons to the appropriate toolbar for each plugin definition.
     *
     * @param pluginDefinitions pluginDefinitions A collection of import/export plugin definitions
     * @param buttonType Type of button to create
     * @param mode Import/export mode
     * @since 2.37 (signature changed in 3.4)
     */
    private void addButtonsToToolbar(Collection<IImportExportPluginDefinition> pluginDefinitions, ButtonType buttonType,
                                     ImportExportMode mode)
    {
        for (ImportExportPluginInfo pluginInfo : createSortedPluginInfos(pluginDefinitions, mode))
        {
            final IImportExportPluginDefinition pluginDefinition = pluginInfo.pluginDefinition;
            final Format format = pluginInfo.format;
            final String formatId = format.getIdentifier().toUpperCase();
            final BufferedImage baseImage = getIconImage(pluginDefinition);
            final String tooltip = "Click to start a " + formatId + " " + mode.displayName().toLowerCase() + " wizard.";

            // Make the button available to be added to toolbar configurations. Since the DATA tab includes these
            // button types as dynamic buttons, it will also be added to any DATA tab instances automatically.
            configurationManager.addReferenceButton(createButton(buttonType, format.getTitle(),
                    getOverlayedImage(baseImage, mode == ImportExportMode.IMPORT ? IMPORT_OVERLAY_URL : EXPORT_OVERLAY_URL),
                    e -> launchDialog(pluginInfo, mode, format, baseImage), tooltip));
        }
    }

    /**
     * Generates a BufferedImage for the plugin icon.
     *
     * @param pluginDefinition The plugin definition
     * @return Image to use, sized at {@link UIResources#FEATURE_IMAGE_SIZE}
     * @since 2.36
     */
    @Nonnull
    private BufferedImage getIconImage(@Nonnull IImportExportPluginDefinition pluginDefinition)
    {
        final Optional<String> pluginIconPath = pluginDefinition.getIconPath();
        //noinspection ConstantConditions - won't be null since the default will be returned
        return pluginIconPath.isPresent()
                ? pluginDefinition.getIcon()
                : UIResources.getSizedImage(PLATFORM_ICONS, "data.png", FEATURE_IMAGE_SIZE, true);
    }

    /**
     * Launch a dialog to allow the user to configure and start an import/export.
     *
     * @param pluginInfo Plugin info from which to construct the wizard
     * @param mode Specifies whether this is an import or an export
     * @param format The data format the wizard will process
     * @param iconImage The icon image to use in the dialog
     * @since 2.37
     */
    private void launchDialog(@Nonnull ImportExportPluginInfo pluginInfo, @Nonnull ImportExportMode mode,
                              @Nullable Format format, @Nullable BufferedImage iconImage)
    {
        // Always get a "new" instance, never cache them as they are invalid across project boundaries
        final IImportExportPlugin plugin = pluginInfo.pluginDefinition.getInstance();
        final IImportExportWizard wizard = mode.getWizardCreator().apply(plugin);
        importExportService.injectWizardFormatsAndDelegate(plugin, wizard);

        final ImportExportDataDialog dialog = ImportExportDataDialog.dialogFor(wizard, mode, format, iconImage, true);

        if (dialog != null)
        {
            // Ensure that the proper plugin class loader is pushed
            ClassLoaderUtil.pushContextClassLoader(wizard.getClass().getClassLoader());
            try
            {
                ComponentUtils.centerComponentOnScreen(dialog);
                dialog.setVisible(true);
            } finally
            {
                ClassLoaderUtil.popContextClassLoader();
            }
        }
    }

    /**
     * Create a sorted set of {@link ImportExportPluginInfo} objects, one for each combination of plugin definition
     * and format, ordered by {@link Format#getTitle()}.
     *
     * @since 2.37
     */
    @Nonnull
    private SortedSet<ImportExportPluginInfo> createSortedPluginInfos(@Nonnull Collection<IImportExportPluginDefinition> definitions,
                                                                      @Nonnull ImportExportMode mode)
    {
        final SortedSet<ImportExportPluginInfo> sortedSet =
                new TreeSet<>(Comparator.comparing(pluginInfo -> pluginInfo.format.getTitle()));

        // Add all the plugin/format pairs
        for (IImportExportPluginDefinition definition : definitions)
        {
            if (logger.isDebugEnabled()) logger.debug("Processing: {}", definition.getUniqueKey());

            try
            {
                final IImportExportPlugin plugin = definition.getInstance();
                final IImportExportWizard wizard = mode.getWizardCreator().apply(plugin);

                if (wizard != null)
                {
                    plugin.getFormats().stream()
                            .map(format -> new ImportExportPluginInfo(definition, format))
                            .forEach(sortedSet::add);
                }
            } catch (Throwable e)
            {
                logger.error("Failed to create importer for plugin: {}", definition.getUniqueKey(), e);
            }
        }

        return sortedSet;
    }

    /**
     * Overlays an image onto the lower right corner of the base image.
     *
     * @param baseImage The base image on which to overlay
     * @param overlayURL The URL of the overlay image, expected to be a square image
     * @return A new image with the overlay image overlaid onto the base image's lower right corner
     * @since 2.37 (signature changed in 3.3)
     */
    @Nonnull
    private BufferedImage getOverlayedImage(@Nonnull BufferedImage baseImage, @Nonnull URL overlayURL)
    {
        final BufferedImage overlayImage = UIResources.getSizedImage(overlayURL, STANDARD_IMAGE_SIZE);
        final BufferedImage finalImage = new BufferedImage(TOOLBAR_BUTTON_IMAGE_SIZE.width,
                TOOLBAR_BUTTON_IMAGE_SIZE.height, BufferedImage.TYPE_INT_ARGB);

        BufferedImage baseImageScaled = ImageUtils.getScaledInstance(baseImage, ACTION_BUTTON_IMAGE_SIZE.width);

        Graphics2D g = finalImage.createGraphics();
        g.drawImage(baseImageScaled, 0, 0, null);
        g.drawImage(overlayImage, finalImage.getWidth() - overlayImage.getWidth(),
                finalImage.getHeight() - overlayImage.getHeight(), null);
        g.dispose();

        return finalImage;
    }

    /**
     * Holds info required to sort and produce buttons for each {@link IImportExportPluginDefinition} {@link Format}.
     *
     * @since 1.14
     */
    private record ImportExportPluginInfo(IImportExportPluginDefinition pluginDefinition, Format format)
    {
    }
}
