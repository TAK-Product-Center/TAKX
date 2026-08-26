package gov.takx.api.playback;

import gov.takx.platform.playback.TimeBoundary;

import javax.swing.JPanel;

import java.awt.Color;

/**
 * A playback lane is one horizontal view of data (playback doesn't really care what kind) oriented to the
 * playback time line.  Each lane consists of:
 * <ul>
 * <li>an info panel, to visually distinguish what's in the lane and usually
 * some form of UI control to invoke a configuration widget for the lane.</li>
 * <li>A a time track panel, to visually represent the data oriented to the master playback time line</li>
 * </ul>
 *
 * @since 1.47
 */
public interface IPlaybackLane extends IDisposable
{
    /**
     * Get the panel that contains the informative display for this lane.
     *
     * @return Panel to display
     */
    JPanel getInfoPanel();

    /**
     * Get the panel that contains the time-oriented data visualization for this lane.
     *
     * @return Panel to display
     */
    JPanel getTimeDataPanel();

    /**
     * @return The time boundary of the data contained in this playback lane, {@code null} if no track data is present
     */
    TimeBoundary getTimeBoundary();

    /**
     * Set the background color on the components of this lane, the info and time track panels.
     * Implementors should update any other artifacts that may relate to background color.
     *
     * @param color The new background color
     */
    void setBackgroundColor(Color color);

    /**
     * Display the configuration editor for this playback lane.  The dialog allows the user to select which
     * Map Objects are to be included in the globe playback lane and to indicate which one, if any, should be
     * "centered on" during playback.
     */
    void displayConfigurationEditor();
}
