package gov.takx.api.playback.ui;

import gov.takx.api.annotation.NoPluginImplementations;

import java.awt.Rectangle;

/**
 * Contract for the Playback Stage, which contains all the primary UI controls for the playback system.
 *
 * @since 1.47
 */
@NoPluginImplementations
public interface IPlaybackStage
{
    /**
     * @return The playback controls UI
     */
    IPlaybackControls getPlaybackControls();

    /**
     * Get the rectangle defining the UI coordinates of the viewport view of the scroll region containing the
     * time data panels for each playback lane.
     *
     * @return View rectangle
     */
    Rectangle getScrollPaneViewRect();

    /**
     * Reset the current zoom factor on the time span.
     */
    void resetZoom();

    /**
     * set minimum height of playback panel in pixels
     */
    void setMinimumHeight(int height);
}
