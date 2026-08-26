package gov.takx.core.view.gui.preferences;

import gov.tak.api.icons.TakColor;
import gov.tak.platform.comms.commo.DeviceDisplayType;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.contentcleaner.ContentCleaner;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import static gov.takx.platform.preferences.PreferenceEntry.CALL_SIGN;
import static gov.takx.platform.preferences.PreferenceEntry.CONTACT_EMAIL;
import static gov.takx.platform.preferences.PreferenceEntry.CONTACT_URN;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DISPLAY_TYPE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_ROLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_TEAM;

/**
 * Preferences page for "My Preferences", including call sign, team, display type, role, and sub-pages for GPS and SA Reporting.
 *
 * @see SituationalAwarenessReportingPreferencesPage
 * @since 5.4
 */
public class MyPreferencesPage extends APreferencesPage
{
    private static final ImageIcon GPS_ICON = assetIcon("Network-GPS_36x36.png");
    private static final ImageIcon REPORTING_ICON = assetIcon("My-Location-Icon-Color_36x36.png");

    @Inject
    private ContentCleaner contentCleaner;

    public MyPreferencesPage()
    {
        super("My Preferences");

        addSectionHeader("Device Preferences");

        addEditor(CALL_SIGN, PreferencesComponentFactory.makeTextEditor());

        addTakColorChooserEditor(SA_REPORTING_TEAM, TakColor.getTeamPalette());

        addEditor(SA_REPORTING_DISPLAY_TYPE, PreferencesComponentFactory.makeComboBox(DeviceDisplayType.class));
        addEditor(SA_REPORTING_ROLE, new RoleSelectionPreferencesEditor());
        addEditor(CONTACT_EMAIL, PreferencesComponentFactory.makeTextEditor());
        addEditor(CONTACT_URN, PreferencesComponentFactory.makeIntSpinnerEditor(-1, Integer.MAX_VALUE, 1));

        addPageLink(GPS_ICON, "Location Preferences", "", LocationPreferencesPage::new);
        addPageLink(REPORTING_ICON, "Reporting Preferences", "", SituationalAwarenessReportingPreferencesPage::new);

        addAction(null, "Prepare for Cloning / Clear Device unique information",
                "Prepares the application for system cloning. Clears the program properties and identifiers, " +
                        "including the unique device identifier and the user's callsign, and closes the application. ",
                () -> {
                    var result = AlwaysOnTopJOptionPane.showOptionDialog(
                            "<html>Are you sure you want to clear the application's program properties and unique identifiers?<br><br>" +
                                    "<center><b>Important:</b> Once the application exits, do not rerun before cloning the system.</center></html>",
                            "Prepare for Cloning", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
                            null, null, JOptionPane.CANCEL_OPTION);

                    if (result == JOptionPane.OK_OPTION) contentCleaner.cleanProgramProperties();
                });
    }

    private static class LocationPreferencesPage extends APreferencesPage
    {
        LocationPreferencesPage()
        {
            super("Location Preferences");
            addCustomTile(new LocationPreferencesPanel());
            addCheckboxEditor(PreferenceEntry.SHOW_CURRENT_LOCATION_MARKER);
            addCheckboxEditor(PreferenceEntry.SELF_MARKER_QUICK_MOVE);
        }
    }
}
