package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ExternalMissionData;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to notify a {@link Mission} containing {@link ExternalMissionData} that the remote data has updated.
 *
 * @since 3.5
 */
public class NotifyChangedMissionExternalData extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("notes", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to notify of an update.
     * @param token Token assigned to the {@link MissionChange} when the remote server
     * creates a change-log entry. Otherwise, seemingly unused.
     */
    public NotifyChangedMissionExternalData(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                            @Nonnull String id, @Nonnull String token)
    {
        super(devoid((headers, params) -> api.notifyExternalDataChanged(headers, params, missionName, id, token)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * @param notes Notes to associate with the changelog entry for notifying an external change of the data.
     *
     * @return Self.
     */
    @Nonnull
    public NotifyChangedMissionExternalData withNotes(@Nonnull String notes)
    {
        return setParam("notes", notes);
    }
}
