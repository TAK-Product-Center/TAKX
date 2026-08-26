package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.awt.Component;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Graphical player for displaying media to the user and providing control over playback.
 *
 * @since 2.8
 */
@Experimental
public interface IMediaPlayer
{
    /**
     * Load the {@link IMedia media} for the player to display.
     *
     * @param media The media to display
     */
    void loadMedia(@Nonnull IMedia media);

    /**
     * Unload the currently-loaded media.
     */
    void unloadMedia();

    /**
     * @return The currently-loaded media, if any
     */
    @Nonnull
    Optional<IMedia> getMedia();

    /**
     * @return the options for this media player
     * @since 2.18
     */
    @Nonnull
    Optional<MediaPlayerOptions> getOptions();

    /**
     * Begin media playback.
     */
    void play();

    /**
     * Pause media playback.
     */
    void pause();

    /**
     * Blank the player.
     */
    void blank();

    /**
     * Seek to a specified position.
     *
     * @param timestamp The position to seek to, in seconds relative to the start
     */
    void seek(double timestamp);

    /**
     * Begin capturing the media to a file given in the path. Most useful when TAKX is displaying live media.
     *
     * @param path The destination for the captured media.
     * @throws IllegalStateException If there is no media loaded, or the media loaded is not {@link IPlayable}
     * @since 5.1.0
     */
    default void capture(@Nonnull String path) throws IllegalStateException
    {
        capture(path, null);
    }

    /**
     * Begin capturing the media to a file given in the path. Most useful when TAKX is displaying live media.
     *
     * @param path The destination for the captured media.
     * @param exceptionHandler Handler to receive any exceptions thrown by the recording process, {@code null} for
     *                         default handling, which will simply log the exceptions.
     * @throws IllegalStateException If there is no media loaded, or the media loaded is not {@link IPlayable}
     * @since 5.8
     */
    void capture(@Nonnull String path, @Nullable BiConsumer<String, Exception> exceptionHandler) throws IllegalStateException;

    /**
     * Stop capturing the media to a file given in the path.
     *
     * @param path The destination for the captured media.
     * @since 5.1.0
     */
    void stopCapture(@Nonnull String path);

    /**
     * @return {@code true} if playback is active
     */
    boolean isPlaying();

    /**
     * Add a listener for media player events.
     */
    void addListener(@Nonnull IMediaPlayerListener listener);

    /**
     * Remove a previously added listener.
     */
    void removeListener(@Nonnull IMediaPlayerListener listener);

    /**
     * Get the Swing component from the player. This is the component that will be displayed to the user.
     *
     * @apiNote The returned component MUST implement this {@link IMediaPlayer} interface.  Typically, this is done
     *          by having the player be a panel and then return {@code this} from this method.<br>
     *          If you are extending {@link AMediaPlayer} this is done for you.
     * @return the component
     * @since 2.37
     */
    @Nonnull
    Component getComponent();

    /**
     * Set the border color of the player panel.
     *
     * @param color the color, pass {@code null} to set the panel border to {@code null}
     * @since 2.37
     */
    void setBorderColor(@Nullable Color color);

    /**
     * Destroy this media player, releasing all resources.
     * @since 5.8
     */
    default void destroy()
    {
        // nothing by default
    }
}
