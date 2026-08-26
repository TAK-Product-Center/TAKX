package gov.tak.api.comms.takserver.mission.call.groups;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.tak.api.comms.takserver.mission.feign.FeignGroupsApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to assign groups for the current user on a TAK Server.
 *
 * @since 5.0
 */
public class SetActiveGroups extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("clientUid", ""));

    /**
     * @param api API reference.
     * @param groups Groups to assign.
     */
    public SetActiveGroups(@Nonnull FeignGroupsApi api, @Nonnull List<Group> groups)
    {
        super(devoid((headers, params) -> api.setActiveGroups(headers, params, groups)), PARAM_MAP);
    }

    /**
     * @param api API reference.
     * @param groups Bits of groups to assign. See {@link Group#bitpos()}.
     */
    public SetActiveGroups(@Nonnull FeignGroupsApi api, @Nonnull int[] groups)
    {
        super(devoid((headers, params) -> api.setActiveGroups(headers, params, groups)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "clientUid";
    }
}
