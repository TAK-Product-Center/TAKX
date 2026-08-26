package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

/**
 * Listener for media player events
 *
 * @since 2.8
 */
@Experimental
public interface IMediaPlayerListener
{
    /**
     * Notification that media was loaded by the player
     *
     * @param player The player
     * @param media  The media
     */
    void onMediaLoaded(IMediaPlayer player, IMedia media);

    /**
     * Notification that a player was closed
     *
     * @param player The player
     */
    void onPlayerClosed(IMediaPlayer player);
}
