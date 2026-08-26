package gov.tak.api.comms.takserver.mission.call.mission;

import feign.Response;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Endpoint caller to fetch a {@link Mission} instance by name in KML format.
 *
 * @since 3.5
 */
public class GetMissionKml extends AbstractEndpointCaller<String>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("download", false));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to fetch.
     */
    public GetMissionKml(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> {
            try (Response response = api.getMissionKML(headers, params, missionName))
            {
                return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ex)
            {
                throw new IllegalStateException("Failed to convert response body to KML string", ex);
            }
        }, PARAM_MAP);
    }

    /**
     * Changes the response to assign include a generated file name.
     *
     * @param download Set to {@code true} change the response's file name to {@code kml$MISSION_NAME$.kml}.
     *
     * @return Self.
     */
    @Nonnull
    public GetMissionKml withDownload(boolean download)
    {
        // TODO: Test this with live server
        return setParam("download", download);
    }
}
