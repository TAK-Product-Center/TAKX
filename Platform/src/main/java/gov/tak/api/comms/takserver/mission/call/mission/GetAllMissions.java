package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to fetch all available {@link Mission} instances.
 *
 * @see GetMission Fetch a single mission by its name.
 * @since 3.5
 */
public class GetAllMissions extends AbstractEndpointCaller<List<Mission>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("passwordProtected", false),
            paramOptional("defaultRole", false),
            paramOptional("tool", "public"));

    /**
     * @param api API reference.
     */
    public GetAllMissions(@Nonnull FeignMissionApi api)
    {
        super(api::getAllMissions, PARAM_MAP);
    }

    /**
     * Filter missions by password protection status.
     *
     * @param passwordProtected {@code true} to only yield password protected missions in the results.
     *
     * @return Self.
     */
    @Nonnull
    public GetAllMissions withPasswordProtected(boolean passwordProtected)
    {
        return setParam("passwordProtected", passwordProtected);
    }

    /**
     * Manipulate result format to include a default role value,
     * even if no value is assigned to the mission on the server's side.
     *
     * @param defaultRole {@code true} to yield missions with the default role of {@code MISSION_SUBSCRIBER} <i>(For older clients)</i>.
     *
     * @return Self.
     */
    @Nonnull
    public GetAllMissions withDefaultRole(boolean defaultRole)
    {
        return setParam("defaultRole", defaultRole);
    }

    /**
     * Filter missions by the name of the tool that made them.
     *
     * @param tool Tool name to filter mission results by.
     *
     * @return Self.
     */
    @Nonnull
    public GetAllMissions withTool(@Nonnull String tool)
    {
        return setParam("tool", tool);
    }
}
