package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to set an expiration time on a {@link Mission} instance.
 *
 * @since 3.5
 */
public class SetMissionExpiration extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("expiration", -1L));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     */
    public SetMissionExpiration(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.setMissionExpiration(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * Sets the expiration timestamp.
     *
     * @param expiration Timestamp in epoch millis for the mission to delete itself.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionExpiration withExpirationMillis(long expiration)
    {
        return setParam("expiration", expiration);
    }
}
