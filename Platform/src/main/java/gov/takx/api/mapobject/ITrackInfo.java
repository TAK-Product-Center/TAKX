package gov.takx.api.mapobject;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IRaptorDataMessage;

/**
 * Stores track "metadata" for an instance of an {@link IMapObject}, including the track count
 * and the geographic and time boundaries for those tracks.
 * <p>
 * From a plugin's perspective this is a value object, and should not be updated.
 * The {@link #updateForRdm(IRaptorDataMessage)} is for internal use only.
 *
 * @since 1.13
 */
@NoPluginImplementations
public interface ITrackInfo
{
    /**
     * Returns the number of tracks associated with an instance of an {@link IMapObject}
     *
     * @return The total number of tracks.
     */
    int getNumberOfTracks();

    /**
     * Returns the date of the oldest track in the set described by this ITrackInfo object.
     *
     * @return The date of the oldest track.
     */
    long getTrackStartDate();

    /**
     * Returns the date of the most recent track in the set described by this ITrackInfo object.
     *
     * @return The date of the most recent track.
     */
    long getTrackEndDate();

    /**
     * Returns the geographic boundaries of the tracks described by this ITrackInfo object.
     *
     * @return The geographic boundaries of the tracks.
     */
    Sector getTrackExtents();

    /**
     * Update our data with data from the given RDM.  This will increment the track count, update the extents
     * (if the RDM has a location) and update the start/end dates.
     *
     * @since 3.0
     */
    @Internal
    void updateForRdm(IRaptorDataMessage rdm);
}
