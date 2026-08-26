package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to upload a mission package <i>(ZIP file)</i> to a {@link Mission}.
 *
 * @since 3.5
 */
public class AddMissionPackage extends AbstractEndpointCaller<List<MissionChange>>
        implements IClientAssociatedEndpointCaller<List<MissionChange>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param missionPackage Raw contents of mission package ZIP archive.
     */
    public AddMissionPackage(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull byte[] missionPackage)
    {
        // TODO: This endpoint intentionally returns non-200 response codes in some cases with response bodies.
        //  - Do we want to handle this case specially? Its the *only* case of this as far as I'm aware...
        super((headers, params) -> api.addMissionPackage(headers, params, missionName, missionPackage), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
