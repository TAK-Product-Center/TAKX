package gov.takx.api.playback;

import gov.takx.api.playback.ui.IPlaybackStage;

import java.awt.Component;

/**
 * Contract for playback system.  The methods here are available to the core, but not to plugins.
 *
 */
public interface IPlaybackSystem extends IPlaybackManager
{
    /**
     * Set the playback stage, which holds that primary UI control/display elements for playback.
     *
     * @param playbackStage The current playback stage
     */
    void setPlaybackStage(IPlaybackStage playbackStage);

    /**
     * Get the GlobalTimeControl component from the playback system
     *
     * @return the GlobalTimeControl component from the playback system
     * @since 2.18
     */
    Component getGlobalTimeControl();
}
