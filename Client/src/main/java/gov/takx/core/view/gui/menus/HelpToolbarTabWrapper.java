package gov.takx.core.view.gui.menus;

import com.google.common.io.Resources;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IPluginDocumentation;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.AboutDialog;
import gov.takx.core.view.gui.WorldWindDiagnostics;
import gov.takx.core.view.gui.reporting.ReportingDialog;
import gov.takx.module.DeploymentAttachments;
import gov.takx.module.IPluginModuleManager;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import javax.swing.JPanel;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.ForkJoinPool;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static gov.takx.platform.ui.UIResources.TOOLBAR_BUTTON_IMAGE_SIZE;

/**
 */
public class HelpToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String USER_DOC_BASE_PATH = "docs/" + PreferenceEntry.NAME.getStringValue() + "_User_Manual";
    private static final String USER_DOC_PDF_PATH = USER_DOC_BASE_PATH + ".pdf";
    private static final String USER_DOC_ZIP_PATH = USER_DOC_BASE_PATH + ".zip";

    private static final String OPENGL_INFO_TOOLTIP = "Display diagnostic information about the graphics capabilities of this computer.";
    private static final String REPORT_ERROR_TOOLTIP = "Submit an issue report";

    @Inject
    private SwingUIBinder swingUIBinder;

    @Inject
    private IUserProxy userProxy;

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    @Inject
    @ProgramPreference(PreferenceEntry.NAME)
    private String programName;

    @PostConstruct
    private void postConstruct()
    {
        createTab(TabType.HELP, true);
    }

    @Override
    protected void addComponents()
    {
        final String aboutTooltip = "Find out details about your version of " + programName + " and your system's properties.";
        final String helpTooltip = "Open the " + programName + " help file.";

        tab.addComponent(createButton(ButtonType.ABOUT, e -> new AboutDialog().setVisible(true), aboutTooltip));
        tab.addComponent(createButton(ButtonType.OPENGL_INFO, e -> WorldWindDiagnostics.showInfo(), OPENGL_INFO_TOOLTIP));
        tab.addComponent(createButton(ButtonType.HELP, programName + ButtonType.HELP.getDisplayText(), e -> showHelp(), helpTooltip));
        tab.addComponent(createButton(ButtonType.REPORT_ERROR, e -> new ReportingDialog().setVisible(true), REPORT_ERROR_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.HELP, tab.getComponents().size()));

        // Add in all plugin-supplied help docs, which will be automatically added to the tab that was just created
        pluginRegistryManager.allRegistries().stream()
                // Synthetic plugins are manufactured, and they never have help documents, so we filter them out
                .filter(registry -> !(registry instanceof ISyntheticPluginRegistry))
                .flatMap(registry -> registry.getPluginDefinitions().stream())
                .forEach(this::addPluginDocumentation);
    }

    /**
     * Handle hot deployed plugins. Create a button for any plugin that contains documentation.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        addPluginDocumentation(pluginAdded.pluginDefinition());
    }

    private void showHelp()
    {
        boolean showGlobalHelp = true;

        IMapObject mo = mapObjectManager.getSelectedMapObject();
        if (null != mo)
        {
            IMapObjectController moc = mo.getController();
            final JPanel helpPanel = swingUIBinder.forDevice(moc).getPluginHelpPanel();
            if (helpPanel != null)
            {
                // Since we just created it to see if it was non-null, we need to release resources on it
                if (helpPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();

                showGlobalHelp = false;
                moc.showHelpDialog();
            }
        }

        if (showGlobalHelp) openUserManual();
    }

    /**
     * Opens the user manual on the system.
     *
     * @since 3.3
     */
    public static void openUserManual()
    {
        ForkJoinPool.commonPool().execute(() -> {
            final File helpFile = new CoreHelpExtractor().getHelpFile();
            if (helpFile != null) FileUtils.browseFile(helpFile);
        });
    }

    /**
     * Helper class to extract the core's help document, which may be a zipped set of HTML or a PDF.
     * @since 5.1
     */
    @PseudoBean
    private static class CoreHelpExtractor
    {
        private static File extractedDir; // So we only extract the doc once

        @Inject
        private IUserProxy userProxy;

        /**
         * If the zipped help file is found, extract that zip into a temp directory and return a file referring to
         * the {@code index.html} within the extracted content.  Otherwise, return a reference to the PDF doc location.
         * Only extract the zipped help file one time.
         *
         * @return The file to open
         */
        @Nullable
        File getHelpFile()
        {
            final File zipFile = new File(USER_DOC_ZIP_PATH);

            if (zipFile.exists())
            {
                try
                {
                    synchronized (CoreHelpExtractor.class)
                    {
                        if (extractedDir == null)
                        {
                            final File tempDirectory = Files.createTempDirectory("help_doc_").toFile();
                            FileUtils.unzipFile(zipFile, tempDirectory);
                            org.apache.commons.io.FileUtils.forceDeleteOnExit(tempDirectory); // Must happen after unzip

                            //noinspection AssignmentToStaticFieldFromInstanceMethod
                            extractedDir = tempDirectory;
                        }

                        return new File(extractedDir, "index.html");
                    }
                } catch (IOException e)
                {
                    logger.warn("Failed to extract help from: {}", zipFile.getAbsolutePath(), e);
                    userProxy.reportError("Opening Help Failed", e.getMessage(), e);
                    return null;
                }
            } else
            {
                return new File(USER_DOC_PDF_PATH);
            }
        }
    }

    /**
     * Add any documentation defined by the given plugin definition annotations.
     *
     * @param definition Plugin definition to process
     * @since 3.0
     */
    private void addPluginDocumentation(@Nonnull IPluginDefinition<?> definition)
    {
        for (IPluginDocumentation pluginDoc : definition.getPluginDocumentation())
        {
            // Make the button available to be added to toolbar configurations. Since the HELP tab includes this
            // button type as a dynamic button, it will also be added to any HELP tab instances automatically.
            configurationManager.addReferenceButton(createDocumentationButton(definition, pluginDoc));
        }
    }

    /**
     * Create a plugin documentation button from the plugin documentation definition.
     *
     * @param definition Plugin definition from which it came
     * @param pluginDoc Documentation declaration to process
     * @return The created button for the plugin documentation.
     * @since 3.0
     */
    private ToolbarButton createDocumentationButton(@Nonnull IPluginDefinition<?> definition, @Nonnull IPluginDocumentation pluginDoc)
    {
        logger.debug("Add plugin documentation: {}", pluginDoc);

        final var buttonImage = getButtonImage(definition, pluginDoc.getIconPath());

        return createButton(ButtonType.PLUGIN_DOCUMENTATION, pluginDoc.getDisplayName(),
                buttonImage, new PluginHelpActionListener(definition, pluginDoc), pluginDoc.getTooltip());
    }

    /**
     * Get the image to use on the help button. If the given iconPath is empty, then use the icon defined on the plugin.
     *
     * @param definition Plugin definition from which it came
     * @param iconPath Icon path from pluginDoc specification, may be empty
     * @return image to use on help button
     * @since 3.0
     */
    private BufferedImage getButtonImage(@Nonnull IPluginDefinition<?> definition, @Nonnull String iconPath)
    {
        if (StringUtils.isEmpty(iconPath)) return definition.getIcon();

        final String fullPath = iconPath.startsWith("/") ? iconPath : "/" + iconPath;
        final URL imageUrl = definition.getResourceClassLoader().getResource(fullPath);
        return UIResources.getSizedImage(imageUrl == null ? ImageUtils.DEFAULT_IMAGE_URL : imageUrl, TOOLBAR_BUTTON_IMAGE_SIZE);
    }

    /**
     * Command button action listener to launch a help document.
     *
     * @since 3.0
     */
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @PseudoBean
    private class PluginHelpActionListener implements ActionListener
    {
        @Inject
        private IPluginModuleManager pluginModuleManager;

        private final IPluginDocumentation pluginDoc;
        private final IPluginDefinition<?> definition;
        private File extractedHelpDoc;
        private File extractedHelpDir;

        PluginHelpActionListener(IPluginDefinition<?> definition, IPluginDocumentation pluginDoc)
        {
            this.pluginDoc = pluginDoc;
            this.definition = definition;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final String filePath = pluginDoc.getFilePath();

            try
            {
                if (pluginDoc.isHtmlDoc())
                {
                    // Extract the resource (which must be a zip file or a directory) and launch a browser on the
                    // index.html file within the extracted content.
                    FileUtils.browseFile(new File(extractHtmlDir(filePath), "index.html"));
                } else if (pluginDoc.isExternal())
                {
                    // Launch an external URL.
                    Desktop.getDesktop().browse(URI.create(filePath));
                } else
                {
                    // Extract and open a single file
                    FileUtils.browseFile(extractHelpDoc(filePath));
                }
            } catch (Exception ex)
            {
                logger.error("Failed to launch help for plugin {}", definition.getUniqueKey(), ex);
                userProxy.reportError("Opening Help Failed", ex.getMessage(), ex);
            }
        }

        /**
         * Extract the given resource (which must be a zip file or a directory) into a temp directory.
         * <p/>
         * Only extract the resource once.
         *
         * @since 5.1
         */
        @Nonnull
        private File extractHtmlDir(@Nonnull String resourcePath) throws IOException
        {
            if (extractedHelpDir != null) return extractedHelpDir;

            final JarFile jarFile = pluginModuleManager.findDeploymentForClassLoader(definition.getResourceClassLoader())
                    .map(deployment -> deployment.getAttachment(DeploymentAttachments.JAR_FILE))
                    .orElseThrow(() -> new IOException("Couldn't find plugin deployment " + definition.getUniqueKey()));

            final ZipEntry entry = getZipEntry(jarFile, resourcePath);

            try
            {
                final File tempDirectory = Files.createTempDirectory("plugin_doc_").toFile();

                if (entry.isDirectory())
                {
                    // Unzip the content, removing the dirName prefix so its contents end up directly in our temp dir
                    final String dirName = entry.getName();
                    ZipUtil.unpack(new File(jarFile.getName()), tempDirectory,
                            fileName -> fileName.startsWith(dirName) ? fileName.replaceFirst(dirName, "") : null);
                } else
                {
                    // Otherwise, it must be a zip file, so just unzip it
                    FileUtils.unzipFile(extractHelpDoc(resourcePath), tempDirectory);
                }

                org.apache.commons.io.FileUtils.forceDeleteOnExit(tempDirectory); // Delete the whole directory on exit

                return extractedHelpDir = tempDirectory;
            } catch (IOException | ZipException e)
            {
                logger.warn("Failed to extract help document '{} : {}'", definition.getUniqueKey(), resourcePath, e);
                throw new IOException("Failed to extract " + resourcePath + "\n" + e.getMessage());
            }
        }

        /**
         * @return The named entry from the given {@code jarFile}.
         * @throws IOException if no entry is found by that name
         * @since 5.1
         */
        private ZipEntry getZipEntry(JarFile jarFile, @Nonnull String resourcePath) throws IOException
        {
            // Since we're looking up a jar file entry by name (not a resource), we need to remove any
            // leading slash that might be in the resource path.
            final String entryName = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            final ZipEntry entry = jarFile.getEntry(entryName);

            if (entry == null) throw new IOException("Missing resource: " + jarFile.getName() + "/" + resourcePath);

            return entry;
        }

        /**
         * Extract the help document from the plugin's resources and place it into a temp file that has the same
         * extension as the original, so that the file "launch" will work properly.
         * <p/>
         * Only extract the file once.
         *
         * @param filePath The resource path within the plugin jar
         * @return File referring to the extracted content
         * @throws IOException If the resource can't be located or extraction fails
         */
        @Nonnull
        private File extractHelpDoc(@Nonnull String filePath) throws IOException
        {
            if (extractedHelpDoc != null) return extractedHelpDoc;

            final URL url = definition.getResourceClassLoader()
                    .getResource(filePath.startsWith("/") ? filePath : "/" + filePath);

            if (url != null)
            {
                // Need to create a temp file with the same suffix as the resource
                final String ext = FileUtils.getExtension(filePath);
                final String suffix = ext != null ? "." + ext : null;

                final File helpDoc = Files.createTempFile("help_", suffix).toFile();
                helpDoc.deleteOnExit();

                try (final FileOutputStream to = new FileOutputStream(helpDoc))
                {
                    Resources.copy(url, to);
                }

                return extractedHelpDoc = helpDoc;
            } else
            {
                throw new IOException("Can't locate resource [" + filePath + "] from [" + definition.getUniqueKey() + "]");
            }
        }
    }
}
