package gov.takx.core.view.gui.preferences;

import gov.takx.api.preferences.IPreferenceMonitor;

import javax.swing.ImageIcon;

import static gov.takx.platform.preferences.PreferenceEntry.DEV_MODE_SETTINGS_ENABLED;
import static gov.takx.platform.preferences.PreferenceEntry.STALE_TIME_EXPIRE_EVERYTHING;
import static gov.takx.platform.preferences.PreferenceEntry.STALE_TIME_EXPIRE_UNKNOWNS;
import static gov.takx.platform.preferences.PreferenceEntry.STALE_TIME_MINUTES_UNTIL_DELETE_STALE;

/**
 * The main user preferences page.
 *
 * @since 5.4
 */
public class MainPreferencesPage extends APreferencesPage
{
    private static final ImageIcon MY_PREFERENCES_ICON = assetIcon("My-Preferences_36x36.png");
    private static final ImageIcon DISPLAY_PREFERENCES_ICON = assetIcon("Display-Preferences_36x36.png");
    private static final ImageIcon NETWORK_PREFERENCES_ICON = assetIcon("Network-Preferences_36x36.png");
    private static final ImageIcon TOOL_PREFERENCES_ICON = assetIcon("Layout-Manager_32x32.png");
    private static final ImageIcon CONTROL_PREFERENCES_ICON = assetIcon("Settings_32x32.png");
    private static final ImageIcon FILE_PREFERENCES_ICON = assetIcon("Directory-Preferences_36x36.png");
    private static final ImageIcon DEVELOPER_PREFERENCES_ICON = assetIcon("Skull-Logo_48x48.png");
    private static final ImageIcon DEVICE_TRACKS_ICON = platformIcon("device_track_pref.png");
    private static final ImageIcon TRACKS_ICON = assetIcon("Track-History_32x32.png");

    private final IPreferenceMonitor<Boolean> devModeMonitor = DEV_MODE_SETTINGS_ENABLED.newBooleanMonitor(this::addDeveloperPreferences, false);

    public MainPreferencesPage()
    {
        super("Preferences");

        addPageLink(MY_PREFERENCES_ICON, "My Preferences", "Adjust how this device is seen on the network", MyPreferencesPage::new);
        addPageLink(DISPLAY_PREFERENCES_ICON, "Display Preferences", "Adjust settings controlling the visual look & feel and data display", DisplayPreferencesPage::new);
        addPageLink(NETWORK_PREFERENCES_ICON, "Network Preferences", "Adjust network parameters", NetworkPreferencesPage::new);
        addPageLink(TOOL_PREFERENCES_ICON, "Tool Preferences", "Adjust preferences related to tools and plugins", ToolsPreferencesPage::new);
        addPageLink(CONTROL_PREFERENCES_ICON, "Control Preferences", "Adjust control options", ControlPreferencesPage::new);
        addPageLink(FILE_PREFERENCES_ICON, "File & Directory Preferences", "Adjust file associations and directory locations", FilePreferencesPage::new);
        addPageLink(TRACKS_ICON, "Track Preferences", "Adjust how trails and tracks are recorded and displayed", BreadcrumbPreferencesPage::new);
        // Track styles (TSDs & CLDs) cannot be handled in our "immediate update" method, so put them in a dialog
        addAction(DEVICE_TRACKS_ICON, "Device Track Preferences",
                "Adjust device track styling options (not to be confused with tracks/breadcrumbs)",
                () -> new StandalonePreferencesDialog("Device Track Preferences", new GlobalSmartTracksPanel()).setVisible(true));

        if (DEV_MODE_SETTINGS_ENABLED.getBooleanValue())
        {
            addDeveloperPreferences(true);
        } else
        {
            devModeMonitor.start();
        }
    }

    private void addDeveloperPreferences(boolean enabled)
    {
        if (enabled)
        {
            devModeMonitor.dispose();
            addPageLink(DEVELOPER_PREFERENCES_ICON, "Developer Preferences",
                    "Messing with these settings will void your warranty; proceed at your own risk",
                    DeveloperPreferencesPage::new);
        }
    }

    private static class ControlPreferencesPage extends APreferencesPage
    {
        ControlPreferencesPage()
        {
            super("Control Preferences");
            addCheckboxEditor(STALE_TIME_EXPIRE_EVERYTHING);
            addCheckboxEditor(STALE_TIME_EXPIRE_UNKNOWNS);
            addEditor(STALE_TIME_MINUTES_UNTIL_DELETE_STALE, PreferencesComponentFactory.makeIntSpinnerEditor());
        }
    }

    private static class FilePreferencesPage extends APreferencesPage
    {
        FilePreferencesPage()
        {
            super("File & Directory Preferences");

            // File associations cannot be handled in our "immediate update" method, so put them in a dialog
            addAction(FILE_PREFERENCES_ICON, "File associations", "Manage file associations for default importer selection",
                    () -> new StandalonePreferencesDialog("File Associations", new FilesPreferencesPanel()).setVisible(true));
        }
    }
}
