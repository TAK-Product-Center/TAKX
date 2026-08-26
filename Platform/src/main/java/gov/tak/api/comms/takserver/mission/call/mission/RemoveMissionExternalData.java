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
 * Endpoint caller to remove an {@link ExternalMissionData} item from a {@link Mission} instance.
 *
 * @since 3.5
 */
public class RemoveMissionExternalData extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("notes", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to remove.
     */
    public RemoveMissionExternalData(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String id)
    {
        super(devoid((headers, params) -> api.removeExternalMissionData(headers, params, missionName, id)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * @param notes Notes to associate with the changelog entry for removing the data.
     *
     * @return Self.
     */
    @Nonnull
    public RemoveMissionExternalData withNotes(@Nonnull String notes)
    {
        return setParam("notes", notes);
    }
}
