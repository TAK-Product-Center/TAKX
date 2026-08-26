package gov.tak.api.comms.takserver;

import jakarta.annotation.Nonnull;

/**
 * Listener for when a user's active groups on a TAK Server changes.
 *
 * @since 5.0
 */
public interface IActiveGroupsListener
{
    /**
     * Called when the {@link IGroupManager} tracks a change in a user's active groups on a TAK Server connection.
     *
     * @param connection Connection to the server where the set of active groups has changed.
     * @param oldSet Old set of active groups.
     * @param newSet New set of active groups.
     */
    void onActiveGroupsChanged(@Nonnull ITakServerConnection connection, @Nonnull IActiveGroupSet oldSet, @Nonnull IActiveGroupSet newSet);
}
