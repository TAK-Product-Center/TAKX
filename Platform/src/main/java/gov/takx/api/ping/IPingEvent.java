package gov.takx.api.ping;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.commons.swing.GraphicsUtils;

import java.awt.Color;

/**
 * Information about a ping event.
 * <p>
 * Plugins should never attempt to create an object that implements this interface.
 *
 */
@NoPluginImplementations
public interface IPingEvent
{
    /**
     * Get latitude of ping.
     *
     * @return Latitude.
     */
    double getLatitude();

    /**
     * Get the longitude of the ping.
     *
     * @return Longitude.
     */
    double getLongitude();

    /**
     * Get the color of the ping visualization.
     *
     * @return Color of the ping.
     */
    Color getColor();

    /**
     * Get the amount of time that the ping should appear on the screen.
     *
     * @return Duration of the ping, in milliseconds.
     */
    int getDuration();

    /**
     * Was this ping event triggered by a plugin? If not, then the ping was created manually by the user.
     *
     * @return True if the ping was created by a plugin.
     */
    boolean isPluginEvent();

    /**
     * @return {@code true} if the ping came from a remote source
     */
    boolean isRemoteEvent();

    /**
     * @return {@code true} if audio should be played, {@code false} if it should not
     * @since 1.19, changed name in 2.0
     */
    boolean isPlayAudio();

    /**
     * @return {@code true} if the ping shape should be flipped vertically, {@code false} if it should not
     * @since 1.19, changed name in 2.0
     */
    boolean isVerticalFlip();

    /**
     * Returns the shape of this ping. By default, it's a circle.
     *
     * @return The shape of this ping
     * @since 2.0
     */
    GraphicsUtils.Shape getShape();
}
