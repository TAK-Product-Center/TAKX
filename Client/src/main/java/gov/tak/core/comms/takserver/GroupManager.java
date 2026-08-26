package gov.tak.core.comms.takserver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.tak.api.comms.takserver.IActiveGroupSet;
import gov.tak.api.comms.takserver.IActiveGroupsListener;
import gov.tak.api.comms.takserver.IGroupManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.mission.GroupEndpoints;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.tak.api.comms.takserver.mission.data.GroupDirection;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.CotEventImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.takserver.TakServerConnectionChanged;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.file.FilePath;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gov.tak.api.cot.CotTypes.GROUP_CHANGE;
import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;

/**
 * Service implementation of {@link IGroupManager}.
 *
 * @since 5.0
 */
@ApplicationScoped
public class GroupManager implements IGroupManager, CotEventImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String GROUPS_DIR = "Groups";

    private final Map<String, ServerGroupManager> serverGroupManagers = new ConcurrentHashMap<>();
    private final List<IActiveGroupsListener> activeGroupsListeners = new CopyOnWriteArrayList<>();
    private final TakServerApiClient takServerApiClient;
    private final ICoreExecutorService executorService;
    private final File groupsRoot;

    @Inject
    public GroupManager(TakServerApiClient takServerApiClient, ICoreExecutorService executorService, CotImporterManager importerManager,
                        ITakServerConnectionManager connectionManager, @VFSManagerScope(VFSScope.Global) IVirtualFileSystemManager vfsManager)
    {
        this.takServerApiClient = takServerApiClient;
        this.executorService = executorService;
        groupsRoot = vfsManager.getRoot(new FilePath(FILES_DIR, GROUPS_DIR).toString());

        // Initiate server managers for known connections that are valid and operational.
        // Any connections that are offline (disabled or invalid) can start their group tracking handling later
        // when the connection opening event is observed.
        for (ITakServerConnection connection : connectionManager.getTakServerConnections())
        {
            if (connection.isConnected()) serverGroupManagers.computeIfAbsent(key(connection), k -> new ServerGroupManager(connection));
        }

        // Register 't-x-g-c' handling for group change notifications.
        // These occur when you are signed in on a server with multiple clients under the same credentials and
        // change your groups on one client. The server tells each client it has effectively acknowledged the re-assignment.
        importerManager.registerImporter(this);
    }

    @SuppressWarnings("unused")
    private void onConnectionOpened(@Observes(notifyObserver = Reception.IF_EXISTS) TakServerConnectionOpened openedEvent)
    {
        initializeForConnection(openedEvent.getConnection());
    }

    @SuppressWarnings("unused")
    private void onConnectionChanged(@Observes(notifyObserver = Reception.IF_EXISTS) TakServerConnectionChanged changedEvent)
    {
        initializeForConnection(changedEvent.getConnection());
    }

    @SuppressWarnings("unused")
    private void onConnectionClosed(@Observes(notifyObserver = Reception.IF_EXISTS) TakServerConnectionClosed closedEvent)
    {
        ServerGroupManager manager = serverGroupManagers.get(key(closedEvent.getConnection()));
    }

    @SuppressWarnings("unused")
    private void onConnectionRemoved(@Observes(notifyObserver = Reception.IF_EXISTS) TakServerConnectionRemoved removedEvent)
    {
        // When a connection is removed, clear the per-server data.
        ITakServerConnection connection = removedEvent.getConnection();
        ServerGroupManager manager = serverGroupManagers.get(key(connection));
        if (manager != null) manager.delete();
    }

    @SuppressWarnings("unused")
    private void handleShutdown(@Observes(notifyObserver = Reception.IF_EXISTS) ShuttingDown event)
    {
        // Save group model when closing.
        for (ServerGroupManager manager : serverGroupManagers.values())
        {
            manager.persist();
        }
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        // Skip if not a group change event.
        String type = event.getType();
        if (!type.equals(GROUP_CHANGE)) return ImportResult.IGNORE;

        // The content is empty, we are just being notified we need to refresh our groups.
        // TODO TAKX-4354: Only update for the relevant server
        serverGroupManagers.values().forEach(ServerGroupManager::getGroups); // Get operation will notify listeners
        return ImportResult.SUCCESS;
    }

    private void initializeForConnection(@Nonnull ITakServerConnection connection)
    {
        // Skip if the per-server manager for the given connection exists.
        String key = key(connection);
        if (serverGroupManagers.containsKey(key)) return;

        // Initiate a groups fetch so later use can rely on the cached value for 'quicker' response times.
        serverGroupManagers.put(key, new ServerGroupManager(connection));
        getGroupsAreSupported(connection);
        getGroups(connection);
    }

    @Nonnull
    @Override
    public IActiveGroupSet getCurrentActiveGroups(@Nonnull ITakServerConnection connection)
    {
        ServerGroupManager manager = serverGroupManagers.get(key(connection));
        if (manager == null)
        {
            logger.error("Cannot check active groups for server '{}' as it is not tracked", connection.getUri());
            return new EmptyActiveGroupSet(connection);
        }

        return manager.getActiveGroups();
    }

    @Override
    public boolean areActiveGroupsKnownForConnection(@Nonnull ITakServerConnection connection)
    {
        ServerGroupManager manager = serverGroupManagers.get(key(connection));
        if (manager == null) return false;

        return manager.state.areActiveGroupsKnown();
    }

    @Override
    public void addActiveGroupsListener(@Nonnull IActiveGroupsListener listener)
    {
        activeGroupsListeners.add(listener);
    }

    @Override
    public boolean removeActiveGroupsListener(@Nonnull IActiveGroupsListener listener)
    {
        return activeGroupsListeners.remove(listener);
    }

    @Nonnull
    @Override
    public CompletableFuture<GroupSupport> getGroupsAreSupported(@Nonnull ITakServerConnection connection)
    {
        ServerGroupManager manager = serverGroupManagers.get(key(connection));
        if (manager == null)
        {
            logger.error("Cannot check group support for server '{}' as it is not tracked", connection.getUri());
            ApiException error = new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER);
            return CompletableFuture.completedFuture(new GroupSupport(null, error));
        }

        return manager.getGroupsAreSupported();
    }

    @Nonnull
    @Override
    public CompletableFuture<GroupsResult> getGroups(@Nonnull ITakServerConnection connection)
    {
        ServerGroupManager manager = serverGroupManagers.get(key(connection));
        if (manager == null)
        {
            logger.error("Cannot get groups for server '{}' as it is not tracked", connection.getUri());
            ApiException error = new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER);
            return CompletableFuture.completedFuture(new GroupsResult(null, error));
        }

        return manager.getGroups();
    }

    @Nonnull
    @Override
    public CompletableFuture<AssignmentResult> setActiveGroups(@Nonnull ITakServerConnection connection, @Nonnull List<Group> groups)
    {
        return setActiveGroups(connection, groups.stream().mapToInt(Group::bitpos).toArray());
    }

    @Nonnull
    @Override
    public CompletableFuture<AssignmentResult> setActiveGroups(@Nonnull ITakServerConnection connection, @Nonnull int... groupBits)
    {
        ServerGroupManager manager = serverGroupManagers.get(key(connection));
        if (manager == null)
        {
            logger.error("Cannot set active groups for server '{}' as it is not tracked", connection.getUri());
            ApiException error = new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER);
            return CompletableFuture.completedFuture(new AssignmentResult(error));
        }

        return manager.setActiveGroups(groupBits);
    }

    private void notifyActiveGroupsChanged(@Nonnull ITakServerConnection connection, @Nonnull IActiveGroupSet oldSet,
                                           @Nonnull IActiveGroupSet newSet)
    {
        for (IActiveGroupsListener listener : activeGroupsListeners)
        {
            try
            {
                listener.onActiveGroupsChanged(connection, oldSet, newSet);
            } catch (Throwable t)
            {
                logger.error("Listener of type '{}' encountered an error", listener.getClass().getSimpleName(), t);
            }
        }
    }

    @Nonnull
    private static <T> T wrapError(@Nonnull Throwable error, @Nonnull Function<ApiException, T> resultConstructor)
    {
        // Unwrap the completion-exception to the cause.
        if (error.getCause() instanceof ApiException apiEx) return resultConstructor.apply(apiEx);
        return resultConstructor.apply(new ApiException(error.getCause()));
    }

    /**
     * @param connection Connection to generate a key for.
     * @return Lookup key for the given connection.
     * @since 5.2
     */
    @Nonnull
    private static String key(@Nonnull ITakServerConnection connection)
    {
        // A connection should consider not only the URL of the remote server, but also the user
        // when creating a unique key. Some real-world use includes multiple people sharing the same
        // system and switching sign-ins. Ensuring data is unique per server/user combo allows for
        // such cases.
        var user = Objects.requireNonNullElse(connection.getConnectionConfig().getUsername(), "default");
        return connection.getUri().toString() + ":" + user;
    }

    /**
     * State for group information associated with a single {@link ITakServerConnection}.
     */
    private class ServerGroupManager
    {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private final GroupEndpoints endpoints;
        private final ITakServerConnection connection;
        private IActiveGroupSet cachedActiveGroupSet;
        private State state = new State();

        static
        {
            OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        }

        private ServerGroupManager(@Nonnull ITakServerConnection connection)
        {
            this.connection = connection;
            cachedActiveGroupSet = new EmptyActiveGroupSet(connection);
            endpoints = takServerApiClient.groupEndpoints(connection);
            restore();
        }

        /**
         * @return File location to persist group information about the current connection.
         */
        @Nonnull
        private File getCacheModelFile()
        {
            return new File(groupsRoot, connection.getId() + ".json");
        }

        /**
         * Deletes the group model data.
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void delete()
        {
            File cacheModel = getCacheModelFile();
            if (cacheModel.exists()) cacheModel.delete();
        }

        /**
         * Restores group model data.
         */
        private void restore()
        {
            File cachedModel = getCacheModelFile();
            if (cachedModel.exists())
            {
                try
                {
                    state = OBJECT_MAPPER.readValue(cachedModel, State.class);
                    recalculateActiveGroups();
                } catch (IOException ex)
                {
                    logger.error("Failed to restore group management state for server '{}'", connection.getUri(), ex);
                }
            }
        }

        /**
         * Persists group model data.
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void persist()
        {
            try
            {
                if (!groupsRoot.exists()) groupsRoot.mkdirs();
                OBJECT_MAPPER.writeValue(getCacheModelFile(), state);
            } catch (IOException ex)
            {
                logger.error("Failed to persist group management state for server '{}'", connection.getUri(), ex);
            }
        }

        /**
         * Called after groups are received from the server.
         *
         * @param newGroups New groups offered by the server.
         */
        private void onReceiveGroups(@Nonnull List<Group> newGroups)
        {
            // Update selected bits first.
            //
            // When operating from a 't-x-g-c' change we need to have the selected
            // bits here be different from what would be reconstructed from our state's cache
            // in order to fire off the active group listeners.
            int[] selectedBits = newGroups.stream()
                    .filter(Group::active)
                    .mapToInt(Group::bitpos)
                    .toArray();
            onApplyGroups(selectedBits);

            // Then update our new cached groups.
            state.cachedGroups = newGroups;
        }

        /**
         * Called when a user's request for changing their active bits completes.
         *
         * @param newGroupBits New group bits of the users active selection.
         */
        private void onApplyGroups(@Nonnull int[] newGroupBits)
        {
            state.cachedGroupBits = newGroupBits;
            recalculateActiveGroups();
        }

        /**
         * Recalculates the current active groups.
         * <br>
         * This value will be what is used for {@link IGroupManager#getCurrentActiveGroups(ITakServerConnection)}.
         */
        private void recalculateActiveGroups()
        {
            List<Group> cachedGroups = state.cachedGroups;
            int[] cachedGroupBits = state.cachedGroupBits;
            if (cachedGroups == null || cachedGroupBits == null) return;

            // Only contain groups matching the active bits
            SortedSet<Group> activeGroups = cachedGroups.stream()
                    .filter(group -> {
                        int target = group.bitpos();
                        for (int bit : cachedGroupBits)
                        {
                            if (bit == target) return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toCollection(TreeSet::new));

            // Only update when the containers are not equal (IE, the recalculated instance does not hold the same groups as the prior instance)
            IActiveGroupSet oldSet = cachedActiveGroupSet;
            IActiveGroupSet newSet = activeGroups.isEmpty() ? new EmptyActiveGroupSet(connection) : new ActiveGroupSet(connection, Collections.unmodifiableSortedSet(activeGroups));
            if (!newSet.equals(oldSet))
            {
                cachedActiveGroupSet = newSet;
                notifyActiveGroupsChanged(connection, oldSet, newSet);
            }
        }

        @Nonnull
        private CompletableFuture<GroupSupport> getGroupsAreSupported()
        {
            // Check if we've already fetched group support state.
            Boolean cachedSupport = state.supportEnabled;
            if (cachedSupport != null) return CompletableFuture.completedFuture(new GroupSupport(cachedSupport, null));

            // Fetch and cache group support state.
            return endpoints.getAreGroupsSupported().asyncGet(executorService).thenApply(support -> {
                state.supportEnabled = support;
                return new GroupSupport(support, null);
            }).exceptionally(error -> wrapError(error, e -> new GroupSupport(null, e)));
        }

        @Nonnull
        private CompletableFuture<GroupsResult> getGroups()
        {
            // Fetch and cache the groups from the given connection.
            return endpoints.getAllGroups().asyncGet(executorService)
                    .thenApply(groups -> {
                        // Cache results for this session. If a group appears twice (IN & OUT) create new group w/ BOTH direction so no info is lost
                        Map<String, Group> sortedGroups = groups.stream().collect(Collectors.toMap(Group::name, t -> t, (a, b) -> a.withDirection(GroupDirection.BOTH)));
                        List<Group> uniqueGroups = sortedGroups.values().stream().sorted().toList();
                        onReceiveGroups(uniqueGroups);
                        return new GroupsResult(uniqueGroups, null);
                    })
                    .exceptionally(error -> wrapError(error, e -> new GroupsResult(null, e)));
        }

        @Nonnull
        public CompletableFuture<AssignmentResult> setActiveGroups(int[] groupBits)
        {
            return endpoints.setActiveGroups(groupBits).asyncGet(executorService)
                    .thenApply(unused -> {
                        onApplyGroups(groupBits);
                        return new AssignmentResult(null);
                    })
                    .exceptionally(error -> wrapError(error, AssignmentResult::new));
        }

        @Nonnull
        public IActiveGroupSet getActiveGroups()
        {
            // Active group set is updated when:
            // - Groups are fetched.
            // - Active bits change.
            return cachedActiveGroupSet;
        }

        /**
         * Static wrapper for state of a {@link ServerGroupManager}. Exists for easy serialization.
         */
        private static class State
        {
            private List<Group> cachedGroups;
            private int[] cachedGroupBits;
            private Boolean supportEnabled;

            /**
             * @return {@code true} when the active groups are known. This is the case when we know what groups are available, and what group bits are selected.
             */
            private boolean areActiveGroupsKnown()
            {
                return cachedGroups == null || cachedGroupBits == null;
            }
        }
    }

    /**
     * Group set of no groups.
     *
     * @param connection Connection to wrap.
     */
    private record EmptyActiveGroupSet(@Nonnull ITakServerConnection connection) implements IActiveGroupSet
    {
        @Nonnull
        @Override
        public ITakServerConnection getConnection()
        {
            return connection;
        }

        @Nonnull
        @Override
        public SortedSet<Group> getActiveGroups()
        {
            return Collections.emptySortedSet();
        }

        @Override
        public boolean isActive(@Nonnull Group group)
        {
            return false;
        }

        @Override
        public boolean isActive(@Nonnull String group)
        {
            return false;
        }

        @Override
        public boolean isActive(int groupBit)
        {
            return false;
        }
    }

    /**
     * Group set of zero or more items.
     *
     * @param connection Connection to the server the groups are applicable to.
     * @param groups Groups the user is a member of.
     */
    private record ActiveGroupSet(@Nonnull ITakServerConnection connection, @Nonnull SortedSet<Group> groups) implements IActiveGroupSet
    {
        @Nonnull
        @Override
        public ITakServerConnection getConnection()
        {
            return connection;
        }

        @Nonnull
        @Override
        public SortedSet<Group> getActiveGroups()
        {
            return groups;
        }

        @Override
        public boolean isActive(@Nonnull Group group)
        {
            return isActive(group.bitpos());
        }

        @Override
        public boolean isActive(@Nonnull String group)
        {
            return groups.stream().anyMatch(g -> g.name().equals(group));
        }

        @Override
        public boolean isActive(int groupBit)
        {
            return groups.stream().anyMatch(g -> g.bitpos() == groupBit);
        }
    }
}
