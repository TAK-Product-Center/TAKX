package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.Component;
import java.util.Optional;

/**
 * Graphical player for displaying media to the user and providing control over playback
 *
 * @since 2.8
 */
@Experimental
public interface IMediaPlayer
{
    /**
     * Load the IMedia for the player to display (found in the MediaPlayerOptions)
     *
     * @param media The media to display
     */
    void loadMedia(@Nonnull IMedia media);

    /**
     * Unload the currently-loaded media
     */
    void unloadMedia();

    /**
     * Get the currently-loaded media
     *
     * @return The currently-loaded media, if any
     */
    Optional<IMedia> getMedia();

    /**
     * Get the options for this media player
     *
     * @return the options
     * @since 2.18
     */
    Optional<MediaPlayerOptions> getOptions();

    /**
     * Begin media playback
     */
    void play();

    /**
     * Pause media playback
     */
    void pause();

    /**
     * Blank the player
     */
    void blank();

    /**
     * Seek to a specified position
     *
     * @param timestamp The position to seek to, in seconds relative to the start
     */
    void seek(double timestamp);

    /**
     * Begin capturing the media to a file given in the path. Most useful when TAKX is displaying live media.
     *
     * @param path The destination for the captured media.
     * @since 5.1.0
     */
    void capture(String path);

    /**
     * Stop capturing the media to a file given in the path.
     *
     * @param path The destination for the captured media.
     * @since 5.1.0
     */
    void stopCapture(String path);

    /**
     * Check if playback is active
     *
     * @return True if playback is active
     */
    boolean isPlaying();

    /**
     * Add a listener for media player events
     *
     * @param listener Listener to add
     */
    void addListener(IMediaPlayerListener listener);

    /**
     * Remove an IMediaPlayerListener
     *
     * @param listener Listener to remove
     */
    void removeListener(IMediaPlayerListener listener);

    /**
     * Get the Swing component from the player. This should be the component that will be displayed to the user.
     *
     * @return the component
     * @since 2.37
     */
    Component getComponent();

    /**
     * Set the border color of the player panel
     *
     * @param color the color
     * @since 2.37
     */
    void setBorderColor(Color color);
}
