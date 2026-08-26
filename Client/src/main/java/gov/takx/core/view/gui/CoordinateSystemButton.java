package gov.takx.core.view.gui;

import gov.takx.core.view.gui.preferences.APreferencesButton;
import gov.takx.platform.preferences.UserPreferenceEntry;

/**
 * Coordinate system button.
 *
 */
public class CoordinateSystemButton extends APreferencesButton
{
    /**
     * Constructor.
     */
    public CoordinateSystemButton()
    {
        super("Coordinate System", UserPreferenceEntry.COORDINATE_SYSTEM);
    }
}
