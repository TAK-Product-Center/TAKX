package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.preferences.PreferenceEntry;

/**
 * Preference page for managing Playback settings.
 * 
 * @since 5.7
 */
@PseudoBean
class PlaybackPreferencesPage extends APreferencesPage
{
    PlaybackPreferencesPage()
    {
        super("Playback Preferences");
        addCheckboxEditor(PreferenceEntry.PLAYBACK_AUTO_SCROLL_AT_END);
        addCheckboxEditor(PreferenceEntry.PLAYBACK_SHOW_LIVE_MARKERS);
    }
}
