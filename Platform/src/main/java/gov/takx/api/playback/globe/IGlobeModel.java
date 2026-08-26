package gov.takx.api.playback.globe;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.ITimeLocation;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Contract for shared model handling all devices that are being shown in a playback lane on any globe.
 *
 * @since 1.47
 */
@NoPluginImplementations
public interface IGlobeModel extends IDisposable
{
    /**
     * Get a list of all Map Objects that are "devices."  Only devices that are displayable (i.e., they pass
     * the current security filter) will be returned.
     *
     * @return List of devices, may be empty
     */
    List<IMapObject> getAllDevices();

    /**
     * Determine if a Map Object is eligible for inclusion in playback. It must:
     * <ul>
     *     <li>NOT be transient</li>
     *     <li>Be a device</li>
     *     <li>Support styled tracks</li>
     *     <li>Have at least one renderable structure defined</li>
     * </ul>
     *
     * @param mapObject the MO to test
     * @return {@code true} if it is eligible for inclusion in playback
     * @since 2.18 (added to API in 5.6)
     */
    boolean eligibleForPlayback(@Nonnull IMapObject mapObject);

    /**
     * Add a device to the model.  This registers a "use" of the device.  The globe model will maintain the
     * underlying tracks model for the device until an equal number of {@link #removeDevice(IMapObject)} calls
     * are made.
     * <p/>
     * As long as the use count for a device is non-zero, the following will be true:
     * <ul>
     * <li>{@link #getDevicesList()} will contain the MO</li>
     * <li>{@link #isDeviceInPlayback(IMapObject)} will return {@code true}</li>
     * <li>{@link #getDeviceTracksModel(IMapObject)} will return a non-null result</li>
     * </ul>
     *
     * @param device            Map Object (device) of interest
     * @param setDevicePlayback put the device in playback mode. Should be false when playback is live
     * @return The tracks model for that device
     * @since 2.18
     */
    IDeviceTracksModel addDevice(IMapObject device, boolean setDevicePlayback);

    /**
     * Add a device to the model.  This registers a "use" of the device.  The globe model will maintain the
     * underlying tracks model for the device until an equal number of {@link #removeDevice(IMapObject)} calls
     * are made.
     * <p/>
     * As long as the use count for a device is non-zero, the following will be true:
     * <ul>
     * <li>{@link #getDevicesList()} will contain the MO</li>
     * <li>{@link #isDeviceInPlayback(IMapObject)} will return {@code true}</li>
     * <li>{@link #getDeviceTracksModel(IMapObject)} will return a non-null result</li>
     * </ul>
     *
     * @param device Map Object (device) of interest
     * @return The tracks model for that device
     */
    IDeviceTracksModel addDevice(IMapObject device);

    /**
     * Remove the specified device, the associated tracks model will be discarded.
     *
     * @param device Device no longer being used
     * @return Always {@code true}
     */
    boolean removeDevice(IMapObject device);

    /**
     * @return The list of all devices that are currently being used in playback.
     * @see #addDevice(IMapObject)
     */
    List<IMapObject> getDevicesList();

    /**
     * Determine if a device has been added to the model.
     *
     * @param device Map Object (device) of interest
     * @return {@code true} if the device is in the model, {@code false} otherwise
     */
    boolean isDeviceInPlayback(IMapObject device);

    /**
     * Get the device tracks model for the Map Object with the given key.
     *
     * @param device Map Object (device) of interest
     * @return Tracks model for that Map Object
     */
    IDeviceTracksModel getDeviceTracksModel(IMapObject device);

    ITimeLocation getPreviousTrackPointInPlayback(long time);

    ITimeLocation getNextTrackPointInPlayback(long playbackTime);
}
