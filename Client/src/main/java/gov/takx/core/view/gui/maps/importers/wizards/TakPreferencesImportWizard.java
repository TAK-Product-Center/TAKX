package gov.takx.core.view.gui.maps.importers.wizards;

import gov.tak.platform.datapackage.PreferencesReader;
import gov.tak.platform.datapackage.TakPreferencesImporter;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.notification.Notification.Type;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

/**
 * Import a {@code .pref} file containing TAK preferences.
 *
 * @since 5.3
 * @see TakPreferencesImporter
 */
@PseudoBean
public class TakPreferencesImportWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private INotificationService notificationService;

    @Override
    public String getType()
    {
        return "TAK Preferences Importer";
    }

    @Override
    public String getDisplayName()
    {
        return "TAK Preferences";
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports TAK Preferences files";
    }

    @Override
    public URL getIcon()
    {
        return ButtonType.PLUGIN_PREFERENCES.getImageUrl();
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public boolean supportsMultipleFiles()
    {
        return true;
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        // Must be a .pref file and contain XML and a preferences entry
        if (!StringUtils.endsWithIgnoreCase(file.getName(), ".pref")) return false;

        try
        {
            final String content = Files.readString(file.toPath());
            return content.contains("<?xml") && content.contains("<preferences>");
        } catch (IOException ignore)
        {
            return false;
        }
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> configuration)
    {
        for (File file : files)
        {
            try
            {
                importPreferences(file);
            } catch (Exception e)
            {
                logger.error("Import of preferences file '{}' failed", file, e);

                final String message = "Preferences import from '" + file.getName() + "' failed: " + e.getMessage();
                notificationService.show(Notification.builder(message, Type.Error).build());
            }
        }
    }

    @UseClientClassLoader // We can be invoked on a random executor thread
    private void importPreferences(File preferencesFile) throws Exception
    {
        final var content = Files.readString(preferencesFile.toPath());
        final var preferences = new PreferencesReader().parsePreferences(content);
        new TakPreferencesImporter().importPreferences(preferences);

        final String message = "Imported preferences from: " + preferencesFile.getName();
        notificationService.show(Notification.builder(message, Type.Success).build());
    }
}
