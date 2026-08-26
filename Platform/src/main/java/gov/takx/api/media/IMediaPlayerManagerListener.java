package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

/**
 * Interface to receive media player manager events.
 *
 * @since 2.37
 */
@Experimental
public interface IMediaPlayerManagerListener
{

    /**
     * Notification that a media player has been added to the UI.
     *
     * @param player The media player that has been added
     */
    void playerAdded(IMediaPlayer player);

    /**
     * Notification that a media player has been removed from the UI.
     *
     * @param player The media player which was removed
     */
    void playerRemoved(IMediaPlayer player);
}
