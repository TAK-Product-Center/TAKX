package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to get the list of all {@link Mission missions} a user is invited to.
 *
 * @since 3.5
 */
public class GetMissionInvitationsForUser extends AbstractEndpointCaller<List<MissionInvitation>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("clientUid", ""));

    /**
     * @param api API reference.
     */
    public GetMissionInvitationsForUser(@Nonnull FeignMissionApi api)
    {
        super(api::getMissionInvitationsForUser, PARAM_MAP);
    }

    /**
     * @param clientUid The {@link ClientEndpoint#uid()} to lookup invitations for.
     *
     * @return Self.
     */
    @Nonnull
    public GetMissionInvitationsForUser withClientUid(@Nonnull String clientUid)
    {
        return setParam("clientUid", clientUid);
    }
}
