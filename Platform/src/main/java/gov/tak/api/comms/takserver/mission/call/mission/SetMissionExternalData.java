package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ExternalMissionData;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to associate some {@link ExternalMissionData} with a {@link Mission} instance.
 *
 * @since 3.5
 */
public class SetMissionExternalData extends AbstractEndpointCaller<ExternalMissionData>
        implements IClientAssociatedEndpointCaller<ExternalMissionData>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param externalMissionData External data to associate with the mission.
     */
    public SetMissionExternalData(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                  @Nonnull ExternalMissionData externalMissionData)
    {
        super((headers, params) -> api.setExternalMissionData(headers, params, missionName, externalMissionData), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
