package gov.takx.api.playback;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.api.playback.globe.IDeviceTracksModelListener;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.api.playback.globe.IGlobePlaybackLane;
import gov.takx.api.playback.ui.IPlaybackControls;
import gov.takx.api.playback.ui.IPlaybackStage;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

import java.util.List;

/**
 * The playback system allows the user (or a plugin) to show a timeline with associated
 * {@link IPlaybackLane Playback Lanes}. Each playback lane is a visual representation
 * of the data managed by the lane and its correlation to the playback timeline. For example, the
 * {@link IGlobePlaybackLane Globe Playback Lane} shows the track points for a selection of Map Objects. Plugins can
 * provide custom playback lanes to show arbitrary data, such as a video, or images. The playback timeline also
 * represents the current "live" representation of the state of TAKX when it is in live mode. In this live mode the
 * user will see the timeline march to the left and will see live data appear from the right, provided that the user has
 * added the live sensor to the "devices" lane (previously known as the globe lane).
 * <p/>
 * The top-level UI component managing this display is the {@link IPlaybackStage}.
 * From there, you can perform operations like adding custom controls
 * (see {@link IPlaybackControls#addCustomControlComponent(javax.swing.JComponent)
 * IPlaybackControls.addCustomControlComponent(JComponent)}) or reset the scale factor on the timeline display.
 * <p/>
 * The playback timeline, represented internally as a {@link ITimeSpan}, maintains a start time, an end time, and a
 * current playback position (cursor). The UI offers the user a number of mouse gestures to move/change the timeline
 * (by panning or scaling). Plugins can perform the majority of these same operations using methods on the Time Span.
 * <p/>
 * Plugins can fully participate in the playback operations. This API provides methods to:
 * <ul>
 * <li>Add/remove playback lanes, see {@link #addPlaybackLanes(IPlaybackLane...)} and {@link #removePlaybackLanes(IPlaybackLane...)}</li>
 * <li>Register to receive playback events, see {@link #addPlaybackEventListener(IPlaybackEventListener)}</li>
 * <li>Query playback data, like {@link #getPlaybackConfiguration()}, {@link #getTimeSpan()},
 * {@link #getGlobeModel()}, {@link #getPlaybackLanes()}, {@link #getCurrentPlaybackPosition()}, and {@link #isPlaying()}</li>
 * <li>control playback, like {@link #play()}, {@link #pause()}, {@link #stop()}, {@link #goLive()} , {@link #stopLive()},
 * and {@link #setCurrentPlaybackPosition(long)}.</li>
 * </ul>
 * <p/>
 * The {@link PlaybackEvent} summarizes the most commonly used events in the playback system. There are other, more
 * fine-grained listener interfaces available on most of the model objects within this system.
 * See {@link ITimeSpan#addListener(ITimeSpanListener)},
 * {@link IPlaybackConfiguration#addPlaybackConfigurationListener(IPlaybackConfigurationListener)}, and
 * {@link IDeviceTracksModel#addListener(IDeviceTracksModelListener)}
 *
 */
@InjectableService
@NoPluginImplementations
public interface IPlaybackManager
{
    /**
     * @return The global time span for playback
     */
    ITimeSpan getTimeSpan();

    /**
     * Adjust the global time span so that it fully encompasses the time boundaries of all the playback lanes.
     *
     * @see IPlaybackLane#getTimeBoundary()
     */
    void fitTimeSpanToLanes();

    /**
     * Adjust the global time span so that it fully encompasses the time boundaries of the MapObject supplied
     *
     * @see IPlaybackLane#getTimeBoundary()
     * @since 2.23
     */
    void fitTimeSpanToMapObject(IMapObject mo);

    /**
     * @return The playback stage, which is the primary UI for playback
     */
    IPlaybackStage getPlaybackStage();

    /**
     * @return The shared globe model that handles the track models for all devices
     */
    IGlobeModel getGlobeModel();

    /**
     * @return The playback configuration
     */
    IPlaybackConfiguration getPlaybackConfiguration();

    /**
     * Show the playback panel in the UI.
     */
    void setPlaybackVisible();

    /**
     * Hide the playback panel in the UI.
     */
    void setPlaybackInvisible();

    /**
     * Add a custom component to the playback control panel.  It is expected that the contents of the component
     * will fit nicely in a toolbar style environment, meaning it shouldn't be massively tall or wide.
     *
     * @param component Component to add
     */
    void addCustomControlComponent(JComponent component);

    /**
     * Remove a previously added custom component from the control panel.
     *
     * @param component Component to remove
     */
    void removeCustomControlComponent(JComponent component);

    /**
     * Add a set of playback lanes to the configuration.  The global time span will be updated to ensure that it
     * contains the time boundary of this new lane.
     *
     * @param playbackLanes The lanes to add
     */
    void addPlaybackLanes(IPlaybackLane... playbackLanes);

    /**
     * Remove a set of playback lanes from the configuration.  The global time span will be updated to show the maximum
     * time boundary of the remaining playback lanes.
     * <p/>
     * It is assumed that the caller of this method will take care of disposing of any resources held by the
     * removed playback lanes.
     *
     * @param playbackLanes The lane to add
     */
    void removePlaybackLanes(IPlaybackLane... playbackLanes);

    /**
     * Replace an existing playback lane with a new one.  The global time span will be updated to ensure that it
     * contains the time boundary of this new lane.
     * <p/>
     * This is a shortcut to {@link IPlaybackConfiguration#replacePlaybackLane(IPlaybackLane, IPlaybackLane) getPlaybackConfiguration().replacePlaybackLane(oldLane, newLane)}
     *
     * @param oldLane The lane to replace
     * @param newLane The replacement lane
     */
    void replacePlaybackLane(IPlaybackLane oldLane, IPlaybackLane newLane);

    /**
     * This is a shortcut to {@link IPlaybackConfiguration#getPlaybackLanes() getPlaybackConfiguration().getPlaybackLanes()}
     *
     * @return A list of all the playback lanes installed
     */
    List<IPlaybackLane> getPlaybackLanes();

    /**
     * Get the playback lane used for visualization of device track data.
     *
     * @return the device playback lane
     * @since 2.18
     */
    IGlobePlaybackLane getGlobePlaybackLane();

    /**
     * Add a device to the playback system.  All the track points for the device will be added and the timeline
     * will be updated with the new endpoints.
     *
     * @param device The device to add to the playback system
     * @return The tracks model for the given device
     */
    IDeviceTracksModel addDevice(IMapObject device);

    /**
     * Remove the device and all of its track data from the playback system.
     *
     * @param device Device to remove
     * @return {@code true} if the device was removed, {@code false} if the device was not in the playback system
     */
    boolean removeDevice(IMapObject device);

    /**
     * @return The list of map objects (devices) that are in the playback list
     */
    List<IMapObject> getDeviceList();

    /**
     * Test to see if a device is in the playback set. Is it being visualized on the timeline.
     *
     * @param device Device of interest
     * @return {@code true} if the map object is currently in the playback device list
     */
    boolean isDeviceInPlayback(IMapObject device);

    /**
     * Get the current location of the given map object (device).  If the location is undefined,
     * then {@link gov.tak.api.maps.coords.GeoPoint#UNKNOWN_POINT} is returned.
     *
     * @param mapObject Device of interest
     * @return Current location on globe (at the present playback position), or {@link gov.tak.api.maps.coords.GeoPoint#UNKNOWN_POINT}
     */
    @Nonnull
    ILocation getCurrentLocation(IMapObject mapObject);

    /**
     * Move the playback position to the previous track point on the timeline.
     */
    void stepBackOneTrackPoint();

    /**
     * Move the playback position to the next track point on the timeline.
     */
    void stepForwardOneTrackPoint();

    /**
     * Pause the playback, playback position is unchanged.
     */
    void pause();

    /**
     * Start playback from the current position.
     */
    void play();

    /**
     * Stop playback and reset position to start of displayed timeline. As opposed to {@link #stopLive()} this method
     * stops the current playback. Note that it will also stop the live mode and simply move the time cursor to the
     * beginning of the displayed timeline.
     */
    void stop();

    /**
     * Marks the end of a user scrubbing the timeline after a mouse release event.
     *
     * @since 2.26
     */
    void stopScrubbing();

    /**
     * Stop playback and go to live mode. Live mode is the default mode in TAKX. In this mode, the timeline will
     * continuously move across time. If live sensor data is coming in, and if that sensor is displayed on the timeline
     * then the user will see the live tracks as they appear on the timeline.
     *
     * @since 2.18
     */
    void goLive();

    /**
     * Stop live mode but if we are in playback don't stop that.
     *
     * @see #goLive()
     * @since 2.18
     */
    void stopLive();

    /**
     * To determine if the playback system is in live mode
     *
     * @return true if in live mode
     * @see #goLive()
     * @since 2.18
     */
    boolean isLive();

    /**
     * @return {@code true} if playback is currently running
     */
    boolean isPlaying();

    /**
     * Get the current playback position (time) in ms.
     *
     * @return current playback position
     */
    long getCurrentPlaybackPosition();

    /**
     * Set the current playback position (time) in ms.
     *
     * @param currentPosition New playback time
     */
    void setCurrentPlaybackPosition(long currentPosition);

    /**
     * Set the current playback speed.
     *
     * @param speed new playback speed.
     */
    void setPlaybackSpeed(int speed);

    /**
     * @return the current playback speed
     */
    int getPlaybackSpeed();

    /**
     * Add a listener to the aggregate event stream.
     *
     * @param listener Listener to register
     */
    void addPlaybackEventListener(IPlaybackEventListener listener);

    /**
     * Remove a listener from the aggregate event stream.
     *
     * @param listener Listener to remove
     */
    void removePlaybackEventListener(IPlaybackEventListener listener);
}
