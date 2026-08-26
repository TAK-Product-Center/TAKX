package gov.takx.api.ping;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.commons.swing.GraphicsUtils;

import java.awt.Color;

/**
 * The ping service generates ping events, which are transient events intended to draw the user's attention
 * to a geographic location. A ping might be represented by a pulsing dot that disappears after a few seconds.
 *
 * @since 1.4
 */
@InjectableService
@NoPluginImplementations
public interface IGeoPingService
{
    Color DEFAULT_COLOR = Color.BLUE;

    int DEFAULT_DURATION = 10000;

    /**
     * Trigger a ping event, with a custom color and duration.
     *
     * @param location The location to ping.
     * @param color    The color to use for ping visualization.
     * @param duration How long to display the ping, in milliseconds.
     */
    void ping(ILocation location, Color color, int duration);

    /**
     * Trigger a ping event, with a custom color and duration.
     *
     * @param lat      Latitude of the ping.
     * @param lon      Longitude of the ping.
     * @param color    The color to use for ping visualization.
     * @param duration How long to display the ping, in milliseconds.
     */
    void ping(double lat, double lon, Color color, int duration);

    /**
     * Trigger a user-generated ping event, with a custom color and duration.
     *
     * @param lat      Latitude of the ping.
     * @param lon      Longitude of the ping.
     * @param color    The color to use for ping visualization.
     * @param duration How long to display the ping, in milliseconds.
     */
    void userPing(double lat, double lon, Color color, int duration);

    /**
     * Trigger a user-generated ping event, with a full specification on color, shape, audio, etc.
     *
     * @param lat          Latitude of the ping.
     * @param lon          Longitude of the ping.
     * @param color        The color to use for ping visualization.
     * @param duration     How long to display the ping, in milliseconds.
     * @param playAudio    Pass {@code true} to play the "ping" sound
     * @param shape        The shape of the visual ping to use
     * @param verticalFlip Pass {@code true} to vertically flip the shape
     */
    void userPing(double lat, double lon, Color color, int duration, boolean playAudio, GraphicsUtils.Shape shape, boolean verticalFlip);

    /**
     * Trigger a ping event, with the default color and duration.
     *
     * @param location The location to ping.
     */
    void ping(ILocation location);

    /**
     * Trigger a ping event, with the default color and duration.
     *
     * @param lat Latitude of the ping.
     * @param lon Longitude of the ping.
     */
    void ping(double lat, double lon);

    /**
     * Send a ping event.
     *
     * @param event Ping to send to listeners.
     */
    void ping(IPingEvent event);

    /**
     * Add a listener for ping events.
     *
     * @param listener Listener to add.
     */
    void addListener(IPingListener listener);

    /**
     * Remove a ping listener.
     *
     * @param listener Listener to remove.
     */
    void removeListener(IPingListener listener);
}
