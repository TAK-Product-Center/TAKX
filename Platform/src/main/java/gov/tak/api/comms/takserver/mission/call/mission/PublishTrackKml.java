package gov.tak.api.comms.takserver.mission.call.mission;

import feign.Response;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.feign.FeignTracksApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to publish an {@link gov.takx.api.breadcrumb.ITrack} to a TAK server.
 *
 * @since 5.5
 */
public class PublishTrackKml extends AbstractEndpointCaller<Response>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("uid", ""),
            param("callsign", ""),
            param("cotType", ""),
            param("groupName", ""),
            param("groupRole", ""));

    /**
     * @param api the API
     * @param kmlFile The track KML file bytes to publish
     */
    public PublishTrackKml(@Nonnull FeignTracksApi api, @Nonnull byte[] kmlFile)
    {
        super((headers, queryMap) -> api.publishTrack(headers, queryMap, kmlFile), PARAM_MAP);
    }

    /**
     * @param uid the track owner marker's UID
     * @return this
     */
    public PublishTrackKml withUid(@Nonnull String uid)
    {
        setParam("uid", uid);
        return this;
    }

    /**
     * @param callsign The track owner's callsign
     * @return this
     */
    public PublishTrackKml withCallsign(@Nonnull String callsign)
    {
        setParam("callsign", callsign);
        return this;
    }

    /**
     * @param cotType The track owner's CoT type
     * @return this
     */
    public PublishTrackKml withCotType(@Nonnull String cotType)
    {
        setParam("cotType", cotType);
        return this;
    }

    /**
     * @param groupName The track owner's group
     * @return this
     */
    public PublishTrackKml withGroupName(@Nonnull String groupName)
    {
        setParam("groupName", groupName);
        return this;
    }

    /**
     * @param groupRole The track owner's group role
     * @return this
     */
    public PublishTrackKml withGroupRole(@Nonnull String groupRole)
    {
        setParam("groupRole", groupRole);
        return this;
    }
}
