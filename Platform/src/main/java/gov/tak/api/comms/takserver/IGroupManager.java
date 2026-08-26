package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for a service that manages {@link Group} information for connected TAK Servers.
 *
 * @since 5.0
 */
@InjectableService
@NoPluginImplementations
public interface IGroupManager
{
    /**
     * Yields a snapshot of which groups are active.
     * <br>
     * There is no direct API endpoint to get this information, so its on
     * clients to track this information. Checking this value or using a {@link IActiveGroupsListener} will
     * keep you up-to-date on the current set of active groups.
     *
     * @param connection Connection to the TAK Server to get active groups of.
     *
     * @return Snapshot of a user's active group on the given server.
     * If the connection's active groups are not known, no groups will be contained in the returned group set.
     *
     * @see #areActiveGroupsKnownForConnection(ITakServerConnection) To check if the active groups are known for a given server.
     * @see #addActiveGroupsListener(IActiveGroupsListener) To be notified of new active group sets.
     */
    @Nonnull
    IActiveGroupSet getCurrentActiveGroups(@Nonnull ITakServerConnection connection);

    /**
     * Which groups are active is not known until we have gotten the available groups from the server and
     * the user has selected their active groups at least once. On our local client this is enough to
     * track what groups the user wants to be active. This however assumes the user is only using TAKX.
     * <p/>
     * When {@link #setActiveGroups(ITakServerConnection, int...)} setting the active groups, after the request is
     * acknowledged, TAK Server should broadcast a {@link gov.tak.api.cot.CotTypes#GROUP_CHANGE} CoT event to all clients
     * that the same user is logged in with, except the calling client. If another TAK client is responsible for changing
     * the active groups we should receive this event. Once received, we can know what groups are active without the local
     * client needing to specify active groups.
     *
     * @param connection Connection to the TAK Server to check for if the active group selection is known.
     *
     * @return {@code true} when the active groups are known for this server.
     *
     * @see #addActiveGroupsListener(IActiveGroupsListener) To be notified of when the first active group set is determined <i>(First call of the listener method)</i>.
     */
    boolean areActiveGroupsKnownForConnection(@Nonnull ITakServerConnection connection);

    /**
     * Adds a listener that is notified when {@link #getCurrentActiveGroups(ITakServerConnection) the active groups}
     * on a TAK Server connection change.
     *
     * @param listener Listener to add.
     */
    void addActiveGroupsListener(@Nonnull IActiveGroupsListener listener);

    /**
     * @param listener Listener to remove.
     *
     * @return {@code true} when the listener was removed. {@code false} if it was not present.
     */
    boolean removeActiveGroupsListener(@Nonnull IActiveGroupsListener listener);

    /**
     * Not all servers are configured to use groups. In fact, some servers may even have groups defined, which
     * are still accessible via {@link #getGroups(ITakServerConnection)} but have their overall use disabled.
     *
     * @param connection Connection to query for group support.
     *
     * @return Future holding the state of group support.
     */
    @Nonnull
    CompletableFuture<GroupSupport> getGroupsAreSupported(@Nonnull ITakServerConnection connection);

    /**
     * When requesting the collection of groups on a server, you are only told what groups your account is
     * authorized to access. There may be other groups on the server that you do not have access to.
     *
     * @param connection Connection to query for groups.
     *
     * @return Future holding all user-accessible groups on the server.
     */
    @Nonnull
    CompletableFuture<GroupsResult> getGroups(@Nonnull ITakServerConnection connection);

    /**
     * Requests changing the user's active groups. The values they can specify should only be from groups
     * accessible to them <i>(See: {@link #getGroups(ITakServerConnection)})</i>
     * <p/>
     * Changing what groups are active for a user acts as a whitelist. Items from TAK Server that do not belong
     * to their active groups should be hidden from view. This includes things such as globe markers
     * of active users, data sync feeds their map items, etc.
     *
     * @param connection Connection to assign groups on.
     * @param groups Groups to assign.
     *
     * @return Future holding the assignment results.
     */
    @Nonnull
    CompletableFuture<AssignmentResult> setActiveGroups(@Nonnull ITakServerConnection connection, @Nonnull List<Group> groups);

    /**
     * Requests changing the user's active groups. The values they can specify should only be from groups
     * accessible to them <i>(See: {@link #getGroups(ITakServerConnection)})</i>
     * <p/>
     * Changing what groups are active for a user acts as a whitelist. Items from TAK Server that do not belong
     * to their active groups should be hidden from view. This includes things such as globe markers
     * of active users, data sync feeds their map items, etc.
     *
     * @param connection Connection to assign groups on.
     * @param groupBits Groups to assign, by {@link Group#bitpos()}.
     *
     * @return Future holding the assignment results.
     */
    @Nonnull
    CompletableFuture<AssignmentResult> setActiveGroups(@Nonnull ITakServerConnection connection, @Nonnull int... groupBits);

    /**
     * Holds information about the request of checking group support.
     *
     * @param supported {@code true} indicates the server actively uses groups.
     * {@code false} indicates the server does not use groups. Will be {@code null} if an {@code error} is present.
     * @param error The error that prevented getting support state. {@code null} if getting support state was a success.
     */
    record GroupSupport(@Nullable Boolean supported, @Nullable ApiException error) implements IPossiblyErroneous
    {
    }

    /**
     * Holds information about the request of group details.
     *
     * @param groups Groups on the server. Will be {@code null} if an {@code error} is present.
     * @param error The error that prevented getting group information. {@code null} if fetching the groups was a success.
     */
    record GroupsResult(@Nullable List<Group> groups, @Nullable ApiException error) implements IPossiblyErroneous
    {
    }

    /**
     * Holds information about the request of self-assigning groups.
     *
     * @param error The error that prevented assigning groups. {@code null} if assigning the groups was a success.
     */
    record AssignmentResult(@Nullable ApiException error) implements IPossiblyErroneous
    {
    }
}
