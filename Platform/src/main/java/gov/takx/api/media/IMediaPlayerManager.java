package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Interface to provide centralized management of media player UI elements.
 *
 * @since 2.37
 */
@InjectableService
@Experimental
@NoPluginImplementations
public interface IMediaPlayerManager
{
    /**
     * Registers to listen for media player manager events.
     *
     * @param mediaManagerListener The listener to add
     */
    void addMediaPlayerManagerListener(@Nonnull IMediaPlayerManagerListener mediaManagerListener);

    /**
     * Unregisters for media player manager events.
     *
     * @param mediaManagerListener The listener to remove
     */
    void removeMediaPlayerManagerListener(@Nonnull IMediaPlayerManagerListener mediaManagerListener);

    /**
     * Create a standard media player loaded with the specified media.
     *
     * @param media The media to load
     * @return a new media player loaded with the provided media
     */
    @Nonnull
    IMediaPlayer createDefaultPlayer(@Nonnull IMedia media);

    /**
     * Create the media player per the values provided by the {@link MediaPlayerOptions} and load the provided
     * {@link IMedia}.
     *
     * @param options the player options to configure the initial state of the new media player
     * @param media   the media to load into the media player
     * @return a new media player loaded with the provided media and configured with the provided options
     */
    @Nonnull
    IMediaPlayer createMediaPlayer(@Nonnull MediaPlayerOptions options, @Nonnull IMedia media);

    /**
     * Create a custom player panel for the provide {@link MediaPlayerOptions}, {@link IMedia} contents, and the
     * provided {@link IMediaPlayer}.
     *
     * @param options the player options to configure the initial state of the media player
     * @param media   the media to load into the media player
     * @param player  a media player provided
     */
    void createMediaPlayer(@Nonnull MediaPlayerOptions options, @Nonnull IMedia media, @Nonnull IMediaPlayer player);

    /**
     * Set the provided {@link IMediaPlayer} as the player for the provided panel related to the provided UUID.
     *
     * @param uuid   UUID for the player's media/panel
     * @param player the media player to set
     */
    void setMediaPlayer(@Nonnull String uuid, @Nonnull IMediaPlayer player);

    /**
     * Removes (closes) the media player provided and cleans up any associated resources/listeners.
     *
     * @param mediaPlayer The media player to remove
     */
    void removeMediaPlayer(@Nonnull IMediaPlayer mediaPlayer);

    /**
     * Removes (closes) the media player for the given media and cleans up any associated resources/listeners. This
     * mechanism enables the closing of the {@link IMediaPlayer} related to specific media content when the reference to
     * the player is unknown.
     *
     * @param media The media related to the media player to be removed
     */
    void removeMediaPlayer(@Nonnull IMedia media);
}
