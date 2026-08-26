package gov.takx.api.globe.render;

/**
 * Contract for a holder of track point ids (RDM and MO).
 *
 * @since 2.29
 */
public interface ITrackPointIds
{
    /**
     * @return The RDM Id
     */
    int getRdmId();

    /**
     * @return The MapObject Id
     */
    int getMapObjectId();
}
