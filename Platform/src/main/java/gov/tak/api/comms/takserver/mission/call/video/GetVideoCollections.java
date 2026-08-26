package gov.tak.api.comms.takserver.mission.call.video;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.VideoConnections;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to get {@link VideoConnections}.
 * @since 5.0
 */
public class GetVideoCollections extends AbstractEndpointCaller<VideoConnections>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("protocol", ""));

    public GetVideoCollections(@Nonnull FeignVideoApi api)
    {
        super(api::getVideoCollections, PARAM_MAP);
    }

    @Nonnull
    public GetVideoCollections withProtocolWhitelist(@Nonnull String protocol)
    {
        return setParam("protocol", protocol);
    }
}
