package gov.takx.core.view.gui.maps.importers;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.notification.Notification.Type;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.module.PluginModuleManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Importer to handle a list of plugin jar files and perform a "hot" deployment of those jars.
 *
 * @since 5.7
 * @see PluginModuleManager#loadPluginJars(List)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PluginJarImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final File PLUGINS_DIR = new File(System.getProperty("user.dir"), "plugins");

    // Pattern to extract everything before the version number in the jar file name
    private static final Pattern BASE_NAME_PATTERN = Pattern.compile("^(?<base>.+?)-\\d+(?:\\.\\d+)*.*");

    private final List<File> jarFiles;

    @Inject
    private PluginModuleManager pluginModuleManager;

    @Inject
    private INotificationService notificationService;

    /**
     * @param jarFiles List of jar files to import
     */
    public PluginJarImporter(@Nonnull List<File> jarFiles)
    {
        this.jarFiles = jarFiles;
    }

    public void doImport()
    {
        if (checkForDuplicates()) return;

        final List<File> copiedFiles = copyToPluginsDir();
        if (copiedFiles == null)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Failed to copy files", "Unable to install plugins");
            return;
        }

        pluginModuleManager.loadPluginJars(copiedFiles);
    }

    /**
     * Copy the given source files into the {@code plugins} directory.
     *
     * @return List of copied files in the plugins directory, or {@code null} if any copy fails.
     */
    @Nullable
    private List<File> copyToPluginsDir()
    {
        final List<File> copiedFiles = new ArrayList<>();
        for (var jarFile : jarFiles)
        {
            try
            {
                FileUtils.copyFileToDirectory(jarFile, PLUGINS_DIR);
                copiedFiles.add(new File(PLUGINS_DIR, jarFile.getName()));
            } catch (IOException e)
            {
                logger.error("Failed to copy plugin jar {} to {}", jarFile, PLUGINS_DIR, e);
                final var notification = Notification.builder("Failed to copy plugin jar " + jarFile.getName(), Type.Error).build();
                notificationService.show(notification);
                return null;
            }
        }
        return copiedFiles;
    }

    /**
     * Check to see if any file being dropped has the same base name as an existing plugin jar.  If so, prompt the
     * user on whether to proceed.
     *
     * @return {@code true} if there were duplicates and the user chose not to proceed, {@code false} to proceed
     */
    private boolean checkForDuplicates()
    {
        final String[] fileList = PLUGINS_DIR.list();
        if (fileList == null) return false;

        final var existingFileNames = Arrays.asList(fileList);

        final var existingBaseNames = existingFileNames.stream()
                .map(BASE_NAME_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group("base"))
                .collect(Collectors.toSet());

        final List<String> duplicateNames = new ArrayList<>();
        for (var jarFile : jarFiles)
        {
            final String name = jarFile.getName();

            // On an identical name, we can't perform the copy, so bail.
            if (existingFileNames.contains(name))
            {
                final String message = "Cannot import duplicate jar file: '" + name + "'";
                AlwaysOnTopJOptionPane.showErrorDialog(message, "Duplicate jar file");
                return true; // Do not proceed
            }

            final var m = BASE_NAME_PATTERN.matcher(name);
            if (m.matches() && existingBaseNames.contains(m.group("base"))) duplicateNames.add(name);
        }

        if (!duplicateNames.isEmpty())
        {
            final String message = "These plugin jar file(s) appear to already be installed.\nAre you sure you want to proceed?\n\n"
                    + String.join("\n", duplicateNames);
            final int response = AlwaysOnTopJOptionPane.showConfirmDialog(null, message, "Possible Duplicates", JOptionPane.YES_NO_OPTION);
            return response != JOptionPane.YES_OPTION;
        }
        return false;
    }
}
