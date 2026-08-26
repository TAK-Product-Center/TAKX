package gov.takx.core.mapdata;

import gov.takx.api.globe.render.ITrackPointIds;

/**
 * Common base for track/map point data, just holding the RDM id, MO id, and message type.
 *
 * @since 1.49
 */
public class TrackPointIds implements ITrackPointIds
{
    public final int rdmId;
    public final int moId;

    public TrackPointIds(int rdmId, int moId)
    {
        this.rdmId = rdmId;
        this.moId = moId;
    }

    @Override
    public int getRdmId()
    {
        return rdmId;
    }

    @Override
    public int getMapObjectId()
    {
        return moId;
    }
}
