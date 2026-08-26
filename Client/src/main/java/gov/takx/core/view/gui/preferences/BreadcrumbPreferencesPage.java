package gov.takx.core.view.gui.preferences;

import gov.tak.api.icons.TakColor;
import gov.takx.core.globe.breadcrumb.BreadcrumbSize;

import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_AUTO_ROTATE_COLORS;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_CSV_EXPORT_HEADER;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_DEFAULT_TRACK_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_DISTANCE_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_KML_EXPORT_CLAMP_TO_GROUND;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_KML_EXPORT_TIMESTAMPS;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_LOG_SELF_TRACKS;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_MAX_BREADCRUMBS;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_PERSISTENT_SELF_TRACKS;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_SERVER_TRACK_TIME_GAP;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_SIZE;
import static gov.takx.platform.preferences.PreferenceEntry.BREADCRUMBS_TRACK_TITLE_PREFIX;

/**
 * Preferences page for {@link gov.takx.api.breadcrumb.IBreadcrumb} trails and {@link gov.takx.api.breadcrumb.ITrack tracks}.
 *
 * @since 5.5
 */
public class BreadcrumbPreferencesPage extends APreferencesPage
{
    BreadcrumbPreferencesPage()
    {
        super("Track Preferences");

        addSectionHeader("Tracks History");

        addCheckboxEditor(BREADCRUMBS_LOG_SELF_TRACKS);
        addEditor(BREADCRUMBS_TRACK_TITLE_PREFIX, PreferencesComponentFactory.makeTextEditor());
        addCheckboxEditor(BREADCRUMBS_AUTO_ROTATE_COLORS);

        addTakColorChooserEditor(BREADCRUMBS_DEFAULT_TRACK_COLOR, TakColor.getTrackPalette());

        addCheckboxEditor(BREADCRUMBS_CSV_EXPORT_HEADER);
        addCheckboxEditor(BREADCRUMBS_KML_EXPORT_CLAMP_TO_GROUND);
        addCheckboxEditor(BREADCRUMBS_KML_EXPORT_TIMESTAMPS);
        addEditor(BREADCRUMBS_SERVER_TRACK_TIME_GAP, PreferencesComponentFactory.makeIntSpinnerEditor(1, null, 1));

        addSectionHeader("Breadcrumb");

        addCheckboxEditor(BREADCRUMBS_PERSISTENT_SELF_TRACKS);
        addEditor(BREADCRUMBS_MAX_BREADCRUMBS, PreferencesComponentFactory.makeSliderEditor(1, 500));
        // addCheckboxEditor(BREADCRUMBS_RENDER_LINE_TO_GROUND); todo: 3687
        addEditor(BREADCRUMBS_SIZE, PreferencesComponentFactory.makeComboBox(BreadcrumbSize.class));
        addEditor(BREADCRUMBS_DISTANCE_THRESHOLD, PreferencesComponentFactory.makeIntSpinnerEditor(0, null, 1));
    }
}
