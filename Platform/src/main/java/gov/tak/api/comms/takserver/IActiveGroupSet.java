package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.Group;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.SortedSet;

/**
 * Outline of a user's selected active groups on a single TAK Server.
 *
 * @since 5.0
 */
@NoPluginImplementations
public interface IActiveGroupSet
{
    /**
     * @return Connection to the associated TAK Server.
     */
    @Nonnull
    ITakServerConnection getConnection();

    /**
     * @return Groups the user is a member of.
     */
    @Nonnull
    SortedSet<Group> getActiveGroups();

    /**
     * @param group Group to check if it is active within this set.
     *
     * @return {@code true} when this set contains the given group.
     */
    boolean isActive(@Nonnull Group group);

    /**
     * @param group Name of group to check if it is active within this set.
     *
     * @return {@code true} when this set contains the given group.
     */
    boolean isActive(@Nonnull String group);

    /**
     * @param groupBit {@link Group#bitpos()} of group to check if it is active within this set.
     *
     * @return {@code true} when this set contains the given group.
     */
    boolean isActive(int groupBit);
}
