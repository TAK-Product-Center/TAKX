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
    private String uuid;
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
     * @param title The title for the player. this is required and non-null.
     * @since 2.37
     */
    public MediaPlayerOptions(@Nonnull String title)
    {
        this.title = title;
    }

    /**
     * @return The media player's title
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
     * @param displayControls Pass {@code true} to enable/show playback controls in the player, {@code false} to disable/hide
     */
    public void setPlaybackControlsEnabled(boolean displayControls)
    {
        this.displayControls = displayControls;
    }

    /**
     * @return {@code true} if built-in playback controls are enabled
     */
    public boolean isPlaybackControlsEnabled()
    {
        return displayControls;
    }

    /**
     * @param autoplayMedia Pass {@code true} to automatically start playing media upon player creation
     */
    public void setAutoplayMedia(boolean autoplayMedia)
    {
        this.autoplayMedia = autoplayMedia;
    }

    /**
     * @return {@code true} if the media will be automatically played
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
