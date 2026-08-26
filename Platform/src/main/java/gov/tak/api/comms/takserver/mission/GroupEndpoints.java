package gov.tak.api.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.call.groups.GetAllGroups;
import gov.tak.api.comms.takserver.mission.call.groups.GetAreGroupsSupported;
import gov.tak.api.comms.takserver.mission.call.groups.SetActiveGroups;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.tak.api.comms.takserver.mission.feign.FeignGroupsApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignGroupsApi}.
 *
 * @since 5.0
 */
public class GroupEndpoints
{
    private final FeignGroupsApi cotApi;

    /**
     * @param apiProvider Provider of client instances.
     * @param connection TAK server connection to use to initialize an API client with.
     */
    public GroupEndpoints(@Nonnull ApiProvider apiProvider,
                          @Nonnull ITakServerConnection connection)
    {
        cotApi = apiProvider.getGroupsApi(connection);
    }

    /**
     * @return Endpoint to query if groups are supported on the current connection.
     */
    @Nonnull
    public GetAreGroupsSupported getAreGroupsSupported()
    {
        return new GetAreGroupsSupported(cotApi);
    }

    /**
     * @return Endpoint to query all {@link Group groups} on the current connection.
     */
    @Nonnull
    public GetAllGroups getAllGroups()
    {
        return new GetAllGroups(cotApi);
    }

    /**
     * @param groups Groups to self-assign.
     *
     * @return Endpoint to self-assign groups to the current user on the current connection.
     */
    @Nonnull
    public SetActiveGroups setActiveGroups(@Nonnull List<Group> groups)
    {
        return new SetActiveGroups(cotApi, groups);
    }

    /**
     * @param groupsBits Bits of groups to self-assign. See {@link Group#bitpos()}.
     *
     * @return Endpoint to self-assign groups to the current user on the current connection.
     */
    @Nonnull
    public SetActiveGroups setActiveGroups(@Nonnull int... groupsBits)
    {
        return new SetActiveGroups(cotApi, groupsBits);
    }

    /**
     * @param groupsBit Bit value of group to self-assign. See {@link Group#bitpos()}.
     *
     * @return Endpoint to self-assign a single group to the current user on the current connection.
     */
    @Nonnull
    public SetActiveGroups setActiveGroup(int groupsBit)
    {
        return setActiveGroups(groupsBit);
    }
}
