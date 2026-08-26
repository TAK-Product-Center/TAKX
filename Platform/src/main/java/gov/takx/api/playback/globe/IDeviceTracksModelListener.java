package gov.takx.api.playback.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.platform.playback.TimeBoundary;

/**
 * Contract for listeners on events from an {@link IDeviceTracksModel}.
 *
 * @since 1.47
 */
public interface IDeviceTracksModelListener
{
    /**
     * A track point has been added to the tracks model for the indicated device (Map Object).  Further, the time
     * stamp on the track point is within the current playback time line (between the time span's start and end).
     * This event is not fired if a track point is added to the set but its time stamp is not within the playback
     * time span.
     *
     * @param device     Device to which a track point was added
     * @param trackPoint The track point added
     */
    void trackPointAdded(IMapObject device, ITimeLocation trackPoint);

    /**
     * The start/end times of the tracks for a given device (Map Object) have changed.
     *
     * @param device   Device who's track time boundary has changed
     * @param boundary The new time boundary of the tracks for this device
     */
    void deviceTimesChanged(IMapObject device, TimeBoundary boundary);
}
