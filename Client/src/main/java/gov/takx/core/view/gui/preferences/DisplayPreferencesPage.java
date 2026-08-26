package gov.takx.core.view.gui.preferences;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.GlobeNavigationStyle;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;

import javax.swing.ImageIcon;

import java.util.List;

/**
 * Preferences page handling options for the globe, markers, notifications, and the main application window.
 *
 * @since 5.4
 */
public class DisplayPreferencesPage extends APreferencesPage
{
    private static final ImageIcon OVERLAYS_ICON = assetIcon("Overlays_32x32.png");
    private static final ImageIcon UNITS_ICON = assetIcon("Range-And-Bearing_32x32.png");
    private static final ImageIcon TOOLBAR_CONFIG_ICON = platformIcon("toolbar_config.png");

    public DisplayPreferencesPage()
    {
        super("Display Preferences");

        addPageLink(UNITS_ICON, "Unit Display Format Preferences", "Adjust preferences related to display for units of measurement, angles, and coordinates", UnitsPreferencesPage::new);
        addPageLink(OVERLAYS_ICON, "Overlay Preferences", "Adjust preferences related to overlays and objects", OverlayPreferencesPage::new);

        // Toolbar config is too complicated for simple preferences page, so put it in a dialog
        addAction(TOOLBAR_CONFIG_ICON, "Toolbar Configuration", "Define/load/save Toolbar configurations",
                () -> new StandalonePreferencesDialog("Toolbar Configuration", new UIConfigurationPreferencesPanel()).setVisible(true));

        addSectionHeader("Map");
        addCheckboxEditor(UserPreferenceEntry.RESTORE_LAST_VIEW);
        addCheckboxEditor(PreferenceEntry.SHOW_CURRENT_LOCATION_MARKER);
        addCheckboxEditor(PreferenceEntry.SHOW_SELF_LOCATION_WIDGET);
        addCheckboxEditor(PreferenceEntry.GLOBE_POSITION_OVERLAY_TRACKS_MOUSE);
        addCheckboxEditor(UserPreferenceEntry.SHOW_SHAPE_LABELS);
        addEditor(PreferenceEntry.MAP_OBJECT_TOOLTIPS, PreferencesComponentFactory.makeComboBox(List.of("default", "on", "off")));
        addCheckboxEditor(UserPreferenceEntry.SHOW_RANGE_BEARING_LABELS);
        addEditor(UserPreferenceEntry.GLOBE_NAVIGATION_STYLE, PreferencesComponentFactory.makeComboBox(GlobeNavigationStyle.class, true, null));

        addSectionHeader("Markers");
        addEditor(UserPreferenceEntry.GLOBAL_ICON_SCALE, PreferencesComponentFactory.makeSliderEditor(25, 400, 100f));
        addCheckboxEditor(PreferenceEntry.USE_COT_TYPE);

        addSectionHeader("Notifications");
        addCheckboxEditor(PreferenceEntry.NOTIFICATIONS_POPUPS_ENABLED);
        addEditor(PreferenceEntry.NOTIFICATIONS_DURATION, PreferencesComponentFactory.makeIntSpinnerEditor());

        addSectionHeader("Main Window");
        addCheckboxEditor(PreferenceEntry.USE_MAX_WINDOW_BOUNDS);
        addCheckboxEditor(PreferenceEntry.SAVE_WINDOW_SETTINGS);
        addCheckboxEditor(UserPreferenceEntry.CONFIRM_QUIT);
        addCheckboxEditor(UserPreferenceEntry.DISPLAY_LOCAL_TIME);
        addCheckboxEditor(UserPreferenceEntry.AUTO_EXPAND);
        addCheckboxEditor(UserPreferenceEntry.AUTO_PIN);
        addCheckboxEditor(UserPreferenceEntry.DOCK_BUTTON_NOTIFICATIONS);
        addCheckboxEditor(PreferenceEntry.SYSTEM_MESSAGE_COUNT_VISIBLE);
    }

    private static class UnitsPreferencesPage extends APreferencesPage
    {
        UnitsPreferencesPage()
        {
            super("Unit Display Format Preferences");
            addEditor(UserPreferenceEntry.COORDINATE_SYSTEM, PreferencesComponentFactory.makeComboBox(CoordinateSystem.class, true, null));
            addEditor(UserPreferenceEntry.DISPLAY_NORTH_REFERENCE, PreferencesComponentFactory.makeComboBox(NorthReference.class));
            addEditor(UserPreferenceEntry.DISPLAY_BEARING_UNIT, PreferencesComponentFactory.makeComboBox(BearingUnits.class));

            addEditor(UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS, PreferencesComponentFactory.makeComboBox(AltitudeUnits.class));
            addEditor(UserPreferenceEntry.DISPLAY_RANGE_UNITS, PreferencesComponentFactory.makeComboBox(RangeUnits.class));
            addEditor(UserPreferenceEntry.DISPLAY_SPEED_UNITS, PreferencesComponentFactory.makeComboBox(SpeedUnits.class));
        }
    }

    private static class OverlayPreferencesPage extends APreferencesPage
    {
        OverlayPreferencesPage()
        {
            super("Overlay Preferences");

            addCheckboxEditor(PreferenceEntry.GLOBE_RADIAL_CENTER_ON_CLICK);
        }
    }
}
