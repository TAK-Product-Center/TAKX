package gov.takx.api.registry;

import gov.takx.api.ui.AIconSelectionPanel;

/**
 * Notification that the user has selected an icon for manipulation during a runtime session.
 *
 * @see AIconSelectionPanel
 * @since 1.13
 */
public interface IIconSelectionListener
{
    /**
     * Notification that an icon was selected.
     *
     * @param iconPath Path to the selected icon
     */
    void iconSelected(String iconPath);
}
