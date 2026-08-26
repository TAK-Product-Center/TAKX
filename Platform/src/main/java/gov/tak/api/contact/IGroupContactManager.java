package gov.tak.api.contact;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.IChatOperationCallback;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Contract for the {@link gov.tak.api.contact.IGroupContact} manager.
 *
 * @since 3.5
 */
@IncubatingApi(since = "3.5")
@InjectableService
@NoPluginImplementations
public interface IGroupContactManager
{
    /**
     * Requests that a new group contact be created for the given chat service protocol (e.g. GeoChat, XMPP, etc.). All
     * requested group members must support the given chat service protocol, and group members cannot be empty.
     *
     * @param groupName The name of the group to create (i.e. the display name of the IGroupContact that will be created)
     * @param groupMembers The group members that should be affiliated with the created group contact
     * @param parentGroup The parent group contact, or {@code null} if the group being created is not a subgroup
     * @param protocol The chat service protocol that should be used to create the group contact
     * @param callback Optional callback that is notified of the group creation request result
     * @throws IllegalArgumentException if group members is empty or a given group member does not support the requested protocol
     */
    void createGroup(@Nonnull String groupName, @Nonnull Set<IContact> groupMembers,
                     @Nullable IGroupContact parentGroup, @Nonnull String protocol,
                     @Nullable IChatOperationCallback callback)
            throws IllegalArgumentException;

    /**
     * Requests that a group contact be updated to contain the provided set of group members.
     *
     * @param groupContact The group identifier
     * @param groupMembers The updated set of group members
     * @param callback Optional callback that is notified with the group update status
     */
    void updateGroup(@Nonnull IGroupContact groupContact, @Nonnull Set<IContact> groupMembers,
                     @Nullable IChatOperationCallback callback);

    /**
     * Requests that a group contact be deleted.
     *
     * @param groupContact The group identifier
     * @param callback Optional callback that is notified with the group deletion request result
     */
    void deleteGroup(@Nonnull IGroupContact groupContact, @Nullable IChatOperationCallback callback);

    /**
     * Returns the {@link IGroupContact} with the specified UID, or {@code null} if not found. This method will
     * <em>not</em> return a default group, like "Groups", "All Chat Rooms", etc.
     *
     * @return The {@link IGroupContact} with the specified UID, or {@code null} if not found
     * @since 3.5
     */
    @Nullable
    IGroupContact getGroupContact(@Nonnull String uid);

    /**
     * Returns the {@link IGroupContact}s for all user-created groups. The results will <em>not</em> include
     * contacts for default groups like "Groups", "All Chat Rooms", etc.
     *
     * @return The {@link IGroupContact}s for all user-created groups
     */
    @Nonnull
    List<IGroupContact> getGroupContacts();

    /**
     * @return the {@link IGroupContact} for the top-level "Groups" group
     * @since 3.5
     */
    @Nullable
    IGroupContact getRootGroupContact();

    /**
     * @param listener The listener to add
     */
    void addListener(@Nonnull IGroupContactManagerListener listener);

    /**
     * @param listener The listener to remove
     */
    void removeListener(@Nonnull IGroupContactManagerListener listener);
}