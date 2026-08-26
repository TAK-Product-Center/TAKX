package gov.tak.api.comms.takserver.mission.call.mission;

import feign.Response;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.nio.charset.StandardCharsets;

/**
 * Endpoint caller to get the latest CoT events as an XML string from a {@link Mission}.
 *
 * @since 3.5
 */
public class GetLatestMissionCotEvents extends AbstractEndpointCaller<String>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get the cot events of.
     */
    public GetLatestMissionCotEvents(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> {
            try (Response response = api.getLatestMissionCotEvents(headers, params, missionName))
            {
                return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ex)
            {
                throw new IllegalStateException("Failed to convert response body to CoT string", ex);
            }
        });
    }
}
