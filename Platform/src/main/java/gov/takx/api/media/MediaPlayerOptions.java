package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;

/**
 * Options for controlling a media player's appearance/behavior.
 *
 * @since 2.8
 */
@Experimental
public class MediaPlayerOptions
{
    private String uuid = null;
    private String title;
    private boolean displayControls = true;
    private boolean autoplayMedia = true;

    /**
     * Use this constructor to set the default values in an options object.<br>
     * <br>
     * Defaults:
     * <ul>
     *     <li>autoplayMedia=true</li>
     *     <li>displayControls=true</li>
     * </ul>
     *
     * @param title The title for the player. this is required and non null.
     * @since 2.37
     */
    public MediaPlayerOptions(@Nonnull String title)
    {
        this.title = title;
    }

    /**
     * Get the media player's title.
     *
     * @return The title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set the media player's title.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Set whether the player will include built-in playback controls (play/pause/seek).
     *
     * @param displayControls True to enable playback controls in the player
     */
    public void setPlaybackControlsEnabled(boolean displayControls)
    {
        this.displayControls = displayControls;
    }

    /**
     * Get whether the player includes built-in playback controls (play/pause/seek).
     *
     * @return True if built-in playback controls are enabled
     */
    public boolean isPlaybackControlsEnabled()
    {
        return displayControls;
    }

    /**
     * Set whether media specified in constructor will automatically start playing.
     *
     * @param autoplayMedia True to automatically start playing media upon player creation
     */
    public void setAutoplayMedia(boolean autoplayMedia)
    {
        this.autoplayMedia = autoplayMedia;
    }

    /**
     * Get whether the media will be automatically played upon creation.
     *
     * @return True if the media will be automatically played
     */
    public boolean isAutoplayMedia()
    {
        return autoplayMedia;
    }

    /**
     * Set the UUID.
     */
    public void setUUID(String id)
    {
        uuid = id;
    }

    /**
     * @return the UUID
     */
    public String getUUID()
    {
        return uuid;
    }
}
