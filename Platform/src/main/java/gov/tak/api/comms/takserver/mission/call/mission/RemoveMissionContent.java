package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove a {@link Resource} entry from a {@link Mission}.
 *
 * @since 3.5
 */
public class RemoveMissionContent extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("creatorUid", ""),
            paramOptional("hash", ""),
            paramOptional("uid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     */
    public RemoveMissionContent(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(unpack((headers, params) -> api.removeMissionContent(headers, params, missionName)), PARAM_MAP);
    }

    /**
     * Sets the SHA256 hash of a {@link Resource} to remove.
     *
     * @param hash A {@link Resource#hash()} to remove from a {@link Mission#contents() Mission's contents}.
     *
     * @return Self.
     */
    @Nonnull
    public RemoveMissionContent withHash(@Nonnull String hash)
    {
        // The API only allows one item, so reset the uid parameter in case it was previously set.
        return unsetParam("uid")
                .setParam("hash", hash);
    }

    /**
     * Sets the UID of a {@link Resource} to remove.
     *
     * @param uid A {@link Resource#uid()} to remove from a {@link Mission#contents() Mission's contents}.
     *
     * @return Self.
     */
    @Nonnull
    public RemoveMissionContent withUid(@Nonnull String uid)
    {
        // The API only allows one item, so reset the hash parameter in case it was previously set.
        return setParam("uid", uid)
                .unsetParam("hash");
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
