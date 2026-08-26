package gov.tak.api.comms.takserver.mission.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Date;

/**
 * Models a group <i>(Also called 'channel')</i> from TAK Server's user management.
 *
 * @param name Name of the group.
 * @param description Description of the group.
 * @param direction Direction denoting this is for read/write access.
 * @param created Date of the group's creation.
 * @param type Group creation/origin type.
 * @param bitpos Assigned bit position - which bit in the "groups" vector this group belongs to.
 * @param active Denotes the group being active <i>on the server.</i>
 * <b>Does not map to what groups a user has marked as their active ones as tracked by {@link gov.tak.api.comms.takserver.IGroupManager}.</b>
 *
 * @since 5.0
 */
public record Group(@Nonnull String name, @Nonnull GroupDirection direction, @Nonnull Date created, @Nonnull GroupType type,
        int bitpos, boolean active, @Nullable String description) implements Comparable<Group>
{
    @Override
    public int compareTo(Group o)
    {
        return name.compareTo(o.name);
    }

    /**
     * Create a copy of the {@link Group} with a new {@link GroupDirection}
     * @param direction The new direction to set
     * @return A new Group with a set direction
     * @since 5.2
     */
    @Nonnull
    public Group withDirection(@Nonnull GroupDirection direction)
    {
        return new Group(name, direction, created, type, bitpos, active, description);
    }
}
