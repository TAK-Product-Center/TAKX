package gov.takx.api.playback.ui;

import gov.takx.api.annotation.NoPluginImplementations;

import javax.swing.JComponent;

/**
 * Contract for component representing the playback command/control UI.
 *
 * @since 1.47
 */
@NoPluginImplementations
public interface IPlaybackControls
{
    /**
     * Add a custom component to the playback control panel.  It is expected that the contents of the component
     * will fit nicely in a toolbar style environment, meaning it shouldn't be massively tall or wide.
     *
     * @param component Component to add
     */
    void addCustomControlComponent(JComponent component);

    /**
     * Remove a previously added custom component from the control panel.
     *
     * @param component Component to remove
     */
    void removeCustomControlComponent(JComponent component);

    /**
     * Collapse the row of individual buttons that control view/zoom.
     */
    void collapseIndividualViewControlButtons();

    /**
     * Expand the row of individual buttons that control view/zoom.
     */
    void expandIndividualViewControlButtons();
}
