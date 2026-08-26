package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.RoleType;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to copy an existing {@link Mission} instance on the server.
 *
 * @since 3.5
 */
public class CopyMission extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("copyName", ""),
            paramOptional("copyPath", ""),
            paramOptional("password", ""),
            paramOptional("defaultRole", RoleType.MISSION_SUBSCRIBER.name()));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to copy.
     */
    public CopyMission(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(unpack((headers, params) -> api.copyMission(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * Sets the name of the copied mission instance.
     *
     * @param copyName New name of the copied mission.
     *
     * @return Self.
     */
    @Nonnull
    public CopyMission withCopyName(@Nonnull String copyName)
    {
        return setParam("copyName", copyName);
    }

    /**
     * Sets the {@link Mission#path()} of the copied mission instance.
     *
     * @param copyPath New value of the mission path.
     *
     * @return Self.
     */
    @Nonnull
    public CopyMission withCopyPath(@Nonnull String copyPath)
    {
        return setParam("copyPath", copyPath);
    }

    /**
     * Adds a password to the copied mission. preventing random access to the mission without knowledge of the password.
     *
     * @param password The password to access the mission.
     *
     * @return Self.
     */
    @Nonnull
    public CopyMission withPassword(@Nonnull String password)
    {
        return setParam("password", password);
    }

    /**
     * Sets the default role of users of the copied mission.
     * By default, users are treated as {@link RoleType#MISSION_SUBSCRIBER subscribers}.
     *
     * @param defaultRole Initial role to assign to users of the copied mission.
     *
     * @return Self.
     */
    @Nonnull
    public CopyMission withDefaultRole(@Nonnull RoleType defaultRole)
    {
        return setParam("defaultRole", defaultRole);
    }
}
