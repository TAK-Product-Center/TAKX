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
 * Endpoint caller to create a read-only access token to a {@link Mission}.
 *
 * @since 3.5
 */
public class CreateMissionReadAccessToken extends AbstractEndpointCaller<String>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("password", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to create a token for.
     */
    public CreateMissionReadAccessToken(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> {
            try (Response response = api.createReadOnlyAccessToken(headers, params, missionName))
            {
                return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ex)
            {
                throw new IllegalStateException("Failed to convert response body to token string", ex);
            }
        }, PARAM_MAP);
    }

    /**
     * Sets the password of the mission, which is required for creating tokens of password-protected missions.
     *
     * @param password Mission's password.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMissionReadAccessToken withPassword(@Nonnull String password)
    {
        return setParam("password", password);
    }
}
