package gov.takx.core.mapdata;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.tracks.ITrackStyleDefinition;

/**
 * This class provides a minimal DTO for holding track point data so that it can be incorporated into a rendering
 * model.  It is meant to be the absolute minimum of data needed to represent a track point in the model. Note
 * that this class has no "elevation" as the current rendering model doesn't use it (since it is a "top down" renderer).
 * <p/>
 * Data in this class is accessed so frequently that method call overhead is a factor.  So, I've made the members
 * public and removed the getters.
 *
 * @since 1.13
 */
public final class TrackPoint extends TrackPointIds
{
    public final double latitude;
    public final double longitude;
    public final long time;
    public int rdmTsdId; // can change over time

    /**
     * Constructor (generally invoked directly from the persistence system).
     *
     * @param rdmId        ID of the RDM this track point represents
     * @param rdmTsdId     ID of the TSD assigned to the RDM, may be {@code null}
     * @param moId         ID of the Map Object that owns this track point
     * @param latitude     Latitude of track point
     * @param longitude    Longitude of track point
     * @param relevantTime The relevant time from the RDM
     */
    @SuppressWarnings("unused") // Used indirectly by hibernate, see RenderingModel.populateFromMapObject
    public TrackPoint(int rdmId, Integer rdmTsdId, int moId, double latitude, double longitude, long relevantTime)
    {
        super(rdmId, moId);

        this.rdmTsdId = rdmTsdId == null ? 0 : rdmTsdId;
        this.latitude = latitude;
        this.longitude = longitude;
        time = relevantTime;
    }

    /**
     * Construct a TrackPoint from a MapPoint.
     *
     * @param mapPoint The MapPoint to use for source data
     * @param rdmTsdId The RDM TSD Id, may be {@code null}
     */
    public TrackPoint(MapPoint mapPoint, Integer rdmTsdId)
    {
        super(mapPoint.rdmId, mapPoint.moId);

        this.rdmTsdId = rdmTsdId == null ? 0 : rdmTsdId;
        latitude = mapPoint.getLatitude();
        longitude = mapPoint.getLongitude();
        time = mapPoint.getTime();
    }

    /**
     * Construct a TrackPoint from the data within an RDM.
     *
     * @param rdm RDM to extract from
     */
    public TrackPoint(IRaptorDataMessage rdm)
    {
        super(rdm.getIdAsInt(), rdm.getMapObject().getId());

        final ITrackStyleDefinition tsd = rdm.getTrackStyleDefinition();
        rdmTsdId = tsd == null ? 0 : tsd.getId();
        latitude = rdm.getLatitude();
        longitude = rdm.getLongitude();
        time = rdm.getRelevantTime();
    }

    /**
     * Set the TSD Id assigned to the RDM this track point represents, may be {@code null}.
     *
     * @param rdmTsdId new TSD ID
     */
    public void setRdmTsdId(Integer rdmTsdId)
    {
        this.rdmTsdId = rdmTsdId == null ? 0 : rdmTsdId;
    }

    @Override
    public String toString()
    {
        return String.format("TrackPoint{rdmId=%d, rdmTsd=%s, moId=%d, lat=%f, lon=%f}", rdmId, rdmTsdId, moId, latitude, longitude);
    }
}
