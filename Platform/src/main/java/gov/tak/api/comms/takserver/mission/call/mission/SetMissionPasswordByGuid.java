package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to set a password on a {@link Mission} instance.
 *
 * @see RemoveMissionPasswordByGuid
 * @since 5.7
 */
public class SetMissionPasswordByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("creatorUid", ""),
            paramOptional("password", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to update.
     */
    public SetMissionPasswordByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super(devoid((headers, params) -> api.setMissionPasswordByGuid(headers, params, missionGuid)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * Sets new password of the mission.
     *
     * @param password New password to assign.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionPasswordByGuid withPassword(@Nonnull String password)
    {
        return setParam("password", password);
    }
}
