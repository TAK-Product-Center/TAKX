package gov.tak.core.comms.takserver;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.comms.takserver.DeleteOption;
import gov.tak.api.comms.takserver.IActiveGroupSet;
import gov.tak.api.comms.takserver.IActiveGroupsListener;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.IDataSyncMissionDownloadListener;
import gov.tak.api.comms.takserver.IDataSyncMissionListener;
import gov.tak.api.comms.takserver.IDataSyncSubscribedMissionListener;
import gov.tak.api.comms.takserver.IGroupManager;
import gov.tak.api.comms.takserver.IServerContactManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.MissionDownloadStatus;
import gov.tak.api.comms.takserver.PublishOption;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionChangeType;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.api.cot.importer.CotEventImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.tak.core.comms.takserver.mission.MissionManager;
import gov.tak.core.comms.takserver.mission.MissionResources;
import gov.tak.core.comms.takserver.mission.MissionSync;
import gov.tak.core.comms.takserver.mission.MissionTransfers;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.takserver.TakServerConnectionChanged;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMapObjectManagerListener;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.lang.ErrorThrowingFunction;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.MissionMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.tak.api.cot.CotTypes.MISSION_CHANGED_CONTENT;
import static gov.tak.api.cot.CotTypes.MISSION_CREATED;
import static gov.tak.api.cot.CotTypes.MISSION_DELETED;
import static gov.tak.api.cot.CotTypes.MISSION_PREFIX;
import static gov.tak.api.util.StandardAttributeNames.ATTACHED_FILES;
import static gov.tak.api.util.StandardAttributeNames.COT_TYPE;
import static gov.tak.api.util.StandardAttributeNames.DATA_SYNC_PENDING_ATTACHMENTS;
import static gov.tak.api.util.StandardAttributeNames.DRAGGING;
import static gov.tak.api.util.StandardAttributeNames.IMPORTING;
import static gov.tak.api.util.StandardAttributeNames.MIL_SYM;
import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;
import static gov.takx.api.shapes.ShapeProperties.ANGLE;
import static gov.takx.api.shapes.ShapeProperties.FILL_COLOR;
import static gov.takx.api.shapes.ShapeProperties.LINE_COLOR;
import static gov.takx.api.shapes.ShapeProperties.LINE_WIDTH;
import static gov.takx.api.shapes.ShapeProperties.LOCATION;
import static gov.takx.api.shapes.ShapeProperties.LOCATIONS;
import static gov.takx.api.shapes.ShapeProperties.NAME;
import static gov.takx.api.shapes.ShapeProperties.OPACITY;
import static gov.takx.api.shapes.ShapeProperties.RADIUS;
import static gov.takx.api.shapes.ShapeProperties.REMARKS;
import static gov.takx.api.shapes.ShapeProperties.SEMI_MAJOR_AXIS;
import static gov.takx.api.shapes.ShapeProperties.SEMI_MINOR_AXIS;
import static gov.takx.api.shapes.ShapeProperties.VERTICAL_HEIGHT;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_DELETE_REMOVED;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_PUBLISH_OPTIONS;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Service implementation of {@link IDataSyncManager}.
 *
 * @since 5.0
 */
@ApplicationScoped
public class DataSyncManager implements IExtendedDataSyncManager, CotEventImporter, IActiveGroupsListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DATA_SYNC_DIR = "DataSync";

    private final List<IDataSyncMissionListener> dataSyncMissionListeners = new CopyOnWriteArrayList<>();
    private final List<IDataSyncSubscribedMissionListener> dataSyncSubscribedMissionListeners = new CopyOnWriteArrayList<>();
    private final List<IDataSyncMissionDownloadListener> dataSyncMissionDownloadListeners = new CopyOnWriteArrayList<>();
    private final List<IDataSyncRestorationListener> dataSyncRestorationListeners = new CopyOnWriteArrayList<>();
    private final Map<String, MissionSync> missionSyncs = new ConcurrentHashMap<>();
    private final ErrorThrowingFunction<ITakServerConnection, MissionSync, IOException> factory;
    private final IVirtualFileSystemManager vfsManager;
    private final ITakServerConnectionManager connectionManager;
    private final ICotExporterManager cotExporterManager;
    private final IMapObjectFinder mapObjectFinder;
    private final IMapObjectManager mapObjectManager;
    private final TakServerApiClient takServerApiClient;
    private final IServerContactManager contactManager;
    private PublishOption publishMode;
    private DeleteOption deleteMode;

    @Inject
    DataSyncManager(@VFSManagerScope(VFSScope.Global) IVirtualFileSystemManager vfsManager,
                    ITakServerConnectionManager connectionManager,
                    ICotExporterManager cotExporterManager,
                    IMapObjectFinder mapObjectFinder,
                    IMapObjectManager mapObjectManager,
                    CotImporterManager importerManager,
                    TakServerApiClient takServerApiClient,
                    IServerContactManager contactManager,
                    IGroupManager groupManager,
                    Instance<MissionSync> missionSyncProvider,
                    @PlatformUnderTest boolean underTest)
    {
        this.vfsManager = vfsManager;
        this.connectionManager = connectionManager;
        this.cotExporterManager = cotExporterManager;
        this.mapObjectFinder = mapObjectFinder;
        this.mapObjectManager = mapObjectManager;
        this.takServerApiClient = takServerApiClient;
        this.contactManager = contactManager;
        this.factory = connection -> missionSyncProvider.get().withConnection(connection);

        // Publish/delete modes are set to 'always' when under testing.
        // Otherwise, will update to match user's preference.
        if (!underTest)
        {
            publishMode = DATA_SYNC_PUBLISH_OPTIONS.getValueAs(PublishOption.class);
            deleteMode = DATA_SYNC_AUTO_DELETE_REMOVED.getValueAs(DeleteOption.class);
            DATA_SYNC_PUBLISH_OPTIONS.addPreferenceEntryChangeListener(change -> publishMode = change.getValueAs(PublishOption.class));
            DATA_SYNC_AUTO_DELETE_REMOVED.addPreferenceEntryChangeListener(change -> deleteMode = change.getValueAs(DeleteOption.class));
        } else
        {
            publishMode = PublishOption.Always;
            deleteMode = DeleteOption.Always;
        }

        // Initiate per-server sync managers for known connections.
        for (ITakServerConnection connection : connectionManager.getTakServerConnections())
        {
            initializeSyncManager(connection);
        }

        // Register self to handle channel selection updates.
        // Some mission availability can change when groups are changed.
        groupManager.addActiveGroupsListener(this);

        // Register self to handle data-sync CoT events sent by the server.
        importerManager.registerImporter(this);
    }

    /**
     * Register map object listeners after project services start.
     * These get cleared between projects <i>(and consequentially integration tests)</i>.
     * @since 5.7
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices event)
    {
        // Register map object property listeners to dispatch updates to missions containing the map objects.
        mapObjectManager.addMapObjectManagerListener(new IMapObjectManagerListener()
        {
            @Override
            public void clientMapObjectAdded(@Nonnull IMapObject mapObject)
            {
                String uid = mapObject.getUid();
                PropertyChangeListener listener = evt -> {
                    // Skip if we never want to publish changes
                    if (publishMode == PublishOption.Never) return;

                    // Skip if the map object property updates are coming from an in-progress import operation.
                    // This primarily occurs when the server re-broadcasts CoT events for map-items added to our missions.
                    // Its very important that we do not broadcast updates in this state so that we do not enter a republish loop with the server.
                    if (mapObject.getMetaBoolean(IMPORTING, false) || mapObject.getMetaInteger(DATA_SYNC_PENDING_ATTACHMENTS, 0) > 0) return;

                    // Skip if the user is dragging the map object (because interrupting them with a dialog would be rude).
                    if (mapObject.getMetaBoolean(DRAGGING, false)) return;

                    // Skip if the map object is not owned by any mission.
                    if (syncs().map(MissionSync::getMissionManager).noneMatch(manager -> manager.isMapItemUidOwned(uid))) return;

                    // Skip if the map object is being imported as part of a mission sync.
                    // We will do this when first downloading a mission since the server won't send us events for existing items in the feed.
                    // The import process triggers property updates though so we want to ignore those.
                    if (syncs().map(MissionSync::getMissionTransfers).anyMatch(transfers -> transfers.isImportingMissionItem(uid))) return;

                    // Collect publishing tasks for the map item across all feeds on all connected servers.
                    logger.debug("Map-item '{}' changed, will publish update to associated missions", mapObject.getName());
                    List<PendingPublish> serverPublishTasks = new ArrayList<>();
                    syncs().map(MissionSync::getMissionManager).forEach(manager -> {
                        // Collect which missions on this connection own the map object.
                        List<Mission> missions = manager.getMapItemUidOwners(uid);
                        if (!missions.isEmpty())
                        {
                            serverPublishTasks.add(new PendingPublish(missions, () -> {
                                for (Mission owningMission : missions) addMapItem(manager.getConnection(), owningMission.name(), mapObject);
                            }));
                        }
                    });
                    if (!serverPublishTasks.isEmpty())
                    {
                        // If we are configured to prompt the user before publishing, then ask them if they want to publish to these missions first.
                        if (publishMode == PublishOption.Prompt)
                        {
                            String message = "Publish " + mapObject.getName() + " to the following missions:\n - "
                                    + serverPublishTasks.stream()
                                            .flatMap(pending -> pending.destinations().stream())
                                            .map(Mission::name)
                                            .collect(Collectors.joining("\n - "));
                            if (JOptionPane.showConfirmDialog(JFrame.getFrames()[0], message, "Publish update", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                        }
                        serverPublishTasks.forEach(pending -> pending.publishTask().run());
                    }
                };

                // Register changes listener for CoT representable properties.
                List.of(ATTACHED_FILES, COT_TYPE, MIL_SYM).forEach(prop -> mapObject.getAttributeSet().addPropertyChangeListener(prop, listener));
                mapObject.addPropertyChangeListener(List.of(NAME, LOCATION, LOCATIONS, ANGLE, OPACITY, RADIUS, SEMI_MAJOR_AXIS, SEMI_MINOR_AXIS, REMARKS, VERTICAL_HEIGHT, FILL_COLOR, LINE_COLOR, LINE_WIDTH), listener);
            }

            @Override
            public void clientMapObjectRemoved(@Nonnull IMapObject mapObject)
            {
            }

            @Override
            public void mapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
            {
            }
        });
    }

    /**
     * @param connection Connection to get the sync manager of.
     * @return Sync manager associated with the given connection.
     * @since 5.7 (return type changed)
     */
    @Nullable
    private MissionSync initializeSyncManager(@Nonnull ITakServerConnection connection)
    {
        return missionSyncs.computeIfAbsent(key(connection), k -> {
            try
            {
                return factory.apply(connection);
            } catch (Exception ex)
            {
                // Should never realistically happen. The constructor is just field sets,
                // and 'setConnection' throws only when called twice.
                logger.error("Failed to re-establish connection: {}", connection, ex);
                return null;
            }
        });
    }

    /**
     * Clear the per-server managers. This is strictly used for testing to clear state between tests.
     */
    @VisibleForTesting
    void clearSyncManagers()
    {
        missionSyncs.clear();
    }

    /**
     * @return Root directory where synced mission information and content is stored.
     */
    @Nonnull
    public File getRootDir()
    {
        return vfsManager.getRoot(new FilePath(FILES_DIR, DATA_SYNC_DIR).toString());
    }

    /**
     * @return List of data sync mission listeners.
     * @since 5.2
     */
    @Nonnull
    public List<IDataSyncMissionListener> getDataSyncMissionListeners()
    {
        return Collections.unmodifiableList(dataSyncMissionListeners);
    }

    /**
     * @return List of data sync mission subscription listeners.
     * @since 5.2
     */
    @Nonnull
    public List<IDataSyncSubscribedMissionListener> getDataSyncSubscribedMissionListeners()
    {
        return Collections.unmodifiableList(dataSyncSubscribedMissionListeners);
    }

    /**
     * @return List of data sync mission download listeners.
     * @since 5.2
     */
    @Nonnull
    public List<IDataSyncMissionDownloadListener> getDataSyncMissionDownloadListeners()
    {
        return Collections.unmodifiableList(dataSyncMissionDownloadListeners);
    }

    /**
     * @return List of data sync restoration listeners.
     * @since 5.3
     */
    @Nonnull
    public List<IDataSyncRestorationListener> getDataSyncRestorationListeners()
    {
        return Collections.unmodifiableList(dataSyncRestorationListeners);
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event,
                             @PlatformUnderTest boolean underTest)
    {
        // Prevent writing data after tests complete.
        if (underTest) return;

        // Persist all per-server mission model information when closing
        for (MissionSync sync : missionSyncs.values()) sync.persist();
    }

    @SuppressWarnings("unused")
    protected void onServerConnected(@Observes TakServerConnectionOpened openedEvent)
    {
        // Look up the original connection reference in the connection manager.
        // The event's connection is a shallow copy. We want the original since only it will receive
        // updates of the connections enabled & connection states.
        ITakServerConnection eventConnection = openedEvent.getConnection();
        ITakServerConnection connection = connectionManager.findConnectionByUuid(eventConnection.getId());
        if (connection != null)
        {
            initializeSyncManager(connection);
        } else
        {
            logger.error("Could not resolve original connection instance for {}", eventConnection.getUri());
        }
    }

    @SuppressWarnings("unused")
    protected void onServerRemoved(@Observes TakServerConnectionRemoved removedEvent)
    {
        ITakServerConnection connection = removedEvent.getConnection();
        sync(connection).ifPresent(sync -> {
            // Remove base mission models, but keep anything that has been downloaded.
            // We will keep the remaining data around in case the connection is re-created.
            MissionManager missionManager = sync.getMissionManager();
            for (Mission mission : missionManager.getMissions()) missionManager.removeMissionByGuid(mission.guid());
        });
    }

    @SuppressWarnings("unused")
    protected void onServerChanged(@Observes TakServerConnectionChanged changedEvent)
    {
        ITakServerConnection connection = changedEvent.getConnection();
        String currentKey = key(connection);

        // For any server manager instance targeting the changed connection we must
        // ensure that the key to look it up is still valid. If not, then we need
        // to migrate the entry in the map to the new key.
        String keyToMigrate = missionSyncs.entrySet().stream()
                .filter(e -> e.getValue().getConnection() == connection)
                .filter(e -> !Objects.equals(currentKey, e.getKey()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (keyToMigrate != null)
        {
            MissionSync syncToMigrate = missionSyncs.remove(keyToMigrate);
            missionSyncs.put(currentKey, syncToMigrate);
        }
    }

    @SuppressWarnings("unused")
    private void handleShutdown(@Observes ShuttingDown event,
                                @PlatformUnderTest boolean underTest)
    {
        // Prevent writing data after tests complete.
        if (underTest) return;

        // Persist all server manager model information when closing
        for (MissionSync sync : missionSyncs.values()) sync.persist();
    }

    @Override
    public ImportResult importData(@Nonnull CotEvent event)
    {
        // Skip if not a mission event.
        String type = event.getType();
        if (!type.startsWith(MISSION_PREFIX)) return ImportResult.IGNORE;

        // Should have a <mission> outlining which one this event pertains to.
        CotDetail missionDetail = event.getDetail().getFirstChildByName("mission");
        if (missionDetail == null) return ImportResult.FAILURE;

        // Extract name/guid and author from the element.
        String missionName = missionDetail.getAttribute("name");
        String missionGuid = missionDetail.getAttribute("guid");

        // Handle different kinds of mission events:
        // - Create
        // - Delete
        // - Add Content (This is reused by the server for any kind of content update)
        if (MISSION_CREATED.equals(type))
        {
            logger.debug("New mission created: {}", missionName);

            // The event does not have any information on which server actually sent the event, how swell.
            // TODO TAKX-4354: Only update for the relevant server
            for (ITakServerConnection connection : connectionManager.getTakServerConnections())
            {
                sync(connection)
                        .map(MissionSync::getMissionTransfers)
                        .ifPresent(transfers -> transfers.updateBaseMissionModel(missionGuid, true));
            }
            return ImportResult.SUCCESS;
        } else if (MISSION_DELETED.equals(type))
        {
            logger.debug("Existing mission deleted: {}", missionName);

            // Skip if mission not recognized.
            MissionSync sync = findContainingSync(missionName, missionGuid);
            if (sync == null) return ImportResult.FAILURE;

            // Remove the base mission model.
            // If no such model exists there's nothing else to be done.
            Mission removed = sync.getMissionManager().removeMissionByGuid(missionGuid);
            if (removed == null) return ImportResult.SUCCESS;

            // Prompt user if they want to delete their local downloaded/subscribed mission.
            if (sync.getMissionManager().isDownloadedByGuid(missionGuid)) SwingUtilities.invokeLater(() -> {
                if (deleteMode == DeleteOption.Prompt)
                {
                    int result = JOptionPane.showConfirmDialog(null, "The feed '" + missionName + "' was deleted on the server.\nDo you want to delete local contents?");
                    if (result == JOptionPane.YES_OPTION)
                    {
                        sync.getMissionManager().removeDownloadedMissionByGuid(missionGuid);
                        deleteLocalResources(sync, removed);
                    }
                } else if (deleteMode == DeleteOption.Always)
                {
                    deleteLocalResources(sync, removed);
                }
            });
            return ImportResult.SUCCESS;
        } else if (type.startsWith(MISSION_CHANGED_CONTENT))
        {
            logger.debug("Existing mission updated: {}", missionName);

            // Skip if mission not recognized.
            MissionSync sync = findContainingSync(missionName, missionGuid);
            if (sync == null) return ImportResult.FAILURE;

            // Update the base model.
            sync.getMissionTransfers().updateBaseMissionModel(missionGuid);

            // If we aren't subscribed to the mission just update the base model.
            // This shouldn't occur if we have properly unsubscribed (networking race conditions aside).
            String token = sync.getMissionManager().getMissionSubscriptionTokenByGuid(missionGuid);
            if (token == null) return ImportResult.SUCCESS;

            // Track if this change declares which markers (if any) are modified.
            CotDetail missionChanges = missionDetail.getFirstChildByName("MissionChanges");
            if (missionChanges != null) for (CotDetail missionChange : missionChanges.getChildren())
            {
                String mapItemUid = Optional.ofNullable(missionChange.getFirstChildByName("contentUid"))
                        .map(CotDetail::getInnerText)
                        .orElse(null);
                if (mapItemUid == null) continue;

                // We found an element outlining the target marker, now lets see the kind of change we have,
                MissionChangeType changeType = Optional.ofNullable(missionChange.getFirstChildByName("type"))
                        .map(s -> MissionChangeType.valueOf(s.getInnerText()))
                        .orElse(null);

                // We only need to intervene for deletion changes.
                // When we have content addition/change events, those will fire off a follow-on CoT event which handles all the work for these change-type.
                if (changeType == MissionChangeType.REMOVE_CONTENT)
                {
                    // Remove the marker if the only mission that contained it was this one.
                    // If no other mission co-owns the marker we should remove it locally to mirror the mission state.
                    IMapObject removed = mapObjectFinder.findMapObjectByUid(mapItemUid);
                    if (removed != null && sync.getMissionManager().getMapItemUidOwners(mapItemUid).stream().allMatch(mission -> missionGuid.equals(mission.guid())))
                        mapObjectManager.deleteMapObject(removed);
                }
            }

            // Fetch the subscribed mission model changes.
            // This will update any associated map-items and resources.
            sync.getMissionTransfers().downloadMissionByGuid(missionGuid, null, token).whenComplete((download, error) -> {
                if (download != null && !download.wasSuccess())
                {
                    logger.error("Mission '{}' changed but couldn't be fetched", missionGuid, download.error());
                } else if (error != null)
                {
                    logger.error("Mission '{}' changed but couldn't be fetched", missionGuid, error);
                }
            });
            return ImportResult.SUCCESS;
        }

        return ImportResult.IGNORE;
    }

    @Override
    public void onActiveGroupsChanged(@Nonnull ITakServerConnection connection, @Nonnull IActiveGroupSet oldSet, @Nonnull IActiveGroupSet newSet)
    {
        // Query server for mission models.
        //
        // Having different channels/groups selected will change how the server filters the "get all missions" response.
        // TODO TAKX-4356: We need to actually *remove* content when a new set removes access to a mission visible with the old set
        sync(connection)
                .map(MissionSync::getMissionTransfers)
                .ifPresent(MissionTransfers::updateBaseMissionModels);
    }

    @Override
    public void addDataSyncMissionListener(@Nonnull IDataSyncMissionListener listener)
    {
        dataSyncMissionListeners.add(listener);
    }

    @Override
    public void removeDataSyncMissionListener(@Nonnull IDataSyncMissionListener listener)
    {
        dataSyncMissionListeners.remove(listener);
    }

    @Override
    public void addDataSyncSubscribedMissionListener(@Nonnull IDataSyncSubscribedMissionListener listener)
    {
        dataSyncSubscribedMissionListeners.add(listener);
    }

    @Override
    public void removeDataSyncSubscribedMissionListener(@Nonnull IDataSyncSubscribedMissionListener listener)
    {
        dataSyncSubscribedMissionListeners.remove(listener);
    }

    @Override
    public void addDataSyncMissionDownloadListener(@Nonnull IDataSyncMissionDownloadListener listener)
    {
        dataSyncMissionDownloadListeners.add(listener);
    }

    @Override
    public void removeDataSyncMissionDownloadListener(@Nonnull IDataSyncMissionDownloadListener listener)
    {
        dataSyncMissionDownloadListeners.remove(listener);
    }

    @Override
    public void addDataSyncRestorationListener(@Nonnull IDataSyncRestorationListener listener)
    {
        dataSyncRestorationListeners.add(listener);
    }

    @Override
    public void removeDataSyncRestorationListener(@Nonnull IDataSyncRestorationListener listener)
    {
        dataSyncRestorationListeners.remove(listener);
    }

    @Override
    public boolean isMissionOwner(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        return sync(connection)
                .map(sync -> sync.getMissionManager().getMissionByName(missionName))
                .map(Mission::creatorUid)
                .map(creatorUid -> {
                    // The creator identifier is usually our client ID.
                    String clientUid = takServerApiClient.getClientUid().toString();
                    if (creatorUid.equals(clientUid)) return true;

                    // In some cases the client ID will be different, but if we're signed in as the same user that should also be a valid match.
                    ClientEndpoint creatorClient = contactManager.getServerClientEndpoints(connection.getId()).get(creatorUid);
                    return creatorClient != null && Objects.equals(connection.getConnectionConfig().getUsername(), creatorClient.username());
                }).orElse(false);
    }

    @Override
    public boolean isTracked(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        // Check if the mission manager for the given connection is aware of the given mission.
        return sync(connection)
                .map(sync -> sync.getMissionManager().getMissionByName(missionName))
                .isPresent();
    }

    @Override
    public boolean isSubscribed(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        return sync(connection)
                .map(sync -> sync.getMissionManager().isSubscribedByName(missionName))
                .orElse(false);
    }

    @Override
    public boolean isDownloaded(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        // A mission with a "pending change" is still "downloaded", but we have a client-side change that hasn't been acknowledged by the server yet.
        return getDownloadState(connection, missionName).isDownloadedOrPendingChange();
    }

    @Nonnull
    @Override
    public MissionDownloadStatus getDownloadState(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        // If there is no info for the server, we've not downloaded a mission from it then.
        return sync(connection)
                .map(sync -> {
                    MissionTransfers transfers = sync.getMissionTransfers();

                    // Check if mission model has been downloaded.
                    Mission mission = sync.getMissionManager().getDownloadedMissionByName(missionName);
                    if (mission == null) return transfers.isFetchingMissionModelByName(missionName) ? MissionDownloadStatus.MODEL_DOWNLOADING : MissionDownloadStatus.NOT_DOWNLOADED;

                    // Check if any associated content (map items & files) is still being downloaded.
                    if (mission.uids().stream().anyMatch(uid -> transfers.isFetchingMissionItem(uid.data()))) return MissionDownloadStatus.ASSOCIATED_RESOURCE_DOWNLOADING;
                    if (mission.contents().stream().anyMatch(content -> transfers.isFetchingMissionResource(content.data().hash()))) return MissionDownloadStatus.ASSOCIATED_RESOURCE_DOWNLOADING;

                    // Check if any pending content is being uploaded
                    if (transfers.isUploadingContentToMissionByName(missionName)) return MissionDownloadStatus.PENDING_CHANGE;

                    return MissionDownloadStatus.COMPLETED;
                })
                .orElse(MissionDownloadStatus.NOT_DOWNLOADED);
    }

    @Nullable
    @Override
    public String getMissionSubscriptionToken(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        return sync(connection)
                .map(MissionSync::getMissionManager)
                .map(manager -> manager.getMissionSubscriptionTokenByName(missionName))
                .orElse(null);
    }

    @Nullable
    @Override
    public File getLocalDownloadedResource(@Nonnull ITakServerConnection connection, @Nonnull Resource resource)
    {
        return sync(connection)
                .map(MissionSync::getMissionResources)
                .map(resources -> resources.resourceFile(resource))
                .orElse(null);
    }

    @Nullable
    @Override
    public File getLocalDownloadedCot(@Nonnull ITakServerConnection connection, @Nonnull String uid)
    {
        return sync(connection)
                .map(MissionSync::getMissionResources)
                .map(resources -> resources.cotFile(uid))
                .orElse(null);
    }

    /**
     * Requests the manual download of a resource. The download will be attempted regardless of the file's size or age.
     *
     * @param connection Connection to server defining the mission
     * @param mission The mission
     * @param resource The resource to download
     * @return the resource download future
     */
    @Nonnull
    public CompletableFuture<MissionResourceDownload> manuallyDownloadResource(@Nonnull ITakServerConnection connection,
                                                                               @Nonnull Mission mission,
                                                                               @Nonnull Resource resource)
    {
        // The result should never be null because a manual download overrides the auto-download criteria
        // and should only be triggered from UI interactions that are offered after a sync is initialized.
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.downloadToLocalResource(mission, resource, true))
                .orElseThrow(IllegalStateException::new);
    }

    /**
     * Requests the manual download of map items defined in CoT. The download will be attempted regardless of the file's
     * age.
     *
     * @param connection Connection to server defining the mission
     * @param mission The mission
     * @param cot The CoT that contains the map item info
     * @return the map item download future
     */
    @Nonnull
    public CompletableFuture<MissionMapItemDownload> manuallyDownloadCot(@Nonnull ITakServerConnection connection,
                                                                         @Nonnull Mission mission,
                                                                         @Nonnull MissionAdd<String> cot)
    {
        // The result should never be null because a manual download overrides the auto-download criteria
        // and should only be triggered from UI interactions that are offered after a sync is initialized.
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.downloadToLocalCot(mission, cot, true))
                .orElseThrow(IllegalStateException::new);
    }

    @Nonnull
    @Override
    public CompletableFuture<File> getResourceByHash(@Nonnull String sha256)
    {
        for (MissionSync sync : missionSyncs.values())
        {
            // Check if we already have the file locally.
            File file = sync.getMissionResources().findResourceFileByHash(sha256);
            if (file != null) return CompletableFuture.completedFuture(file);

            // Check if there is a pending download for a file by the given hash.
            CompletableFuture<File> future = sync.getMissionTransfers().getFutureFetchedResourcePath(sha256);
            if (future != null) return future;
        }

        // There are no server managers aware of a resource by the given hash at this point.
        // At some later point we may want to do a lookup on the servers via 'GetContent'.
        return CompletableFuture.failedFuture(new FileNotFoundException("No resource with a matching sha256 hash: " + sha256));
    }

    @Nonnull
    @Override
    public CompletableFuture<File> attachResourceByHash(@Nonnull MapItem mapItem, @Nonnull String sha256)
    {
        // Increment the pending attachment counter.
        updatePendingAttachmentCounter(mapItem, 1);
        return getResourceByHash(sha256)
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((file, error) -> {
                    if (file == null)
                    {
                        // Attachment could not be found, decrement the pending counter.
                        updatePendingAttachmentCounter(mapItem, -1);
                        return;
                    }

                    String fileName = file.getName();
                    if (mapItem.getAttachment(fileName) == null) mapItem.addAttachment(fileName, file);

                    // Attaching has succeeded, decrement the pending counter.
                    updatePendingAttachmentCounter(mapItem, -1);
                });
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionDownload> download(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nullable String password)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.downloadMissionByName(missionName, password, null))
                .orElseGet(() -> {
                    logger.error("Cannot download to mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
                    return CompletableFuture.failedFuture(new IllegalStateException("Untracked server: " + connection.getUri()));
                });
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionDeletion> delete(@Nonnull ITakServerConnection connection, @Nonnull String missionName, boolean deleteLocal, boolean deleteRemote)
    {
        return sync(connection).map(sync -> {
            // Remove the mission from the subscribed mission map.
            //
            // We do this immediately so that when we delete the map objects associated with
            // the mission we do not send 'delete this map item from the mission' requests.
            //
            // If the mission isn't one we're subscribed to we'll fall back to one that
            // is only downloaded. And if that's missing then the base mission model.
            MissionManager missionManager = sync.getMissionManager();
            Mission removedMission = missionManager.removeDownloadedMissionByName(missionName);
            String removedSubToken = missionManager.removeMissionSubscriptionTokenByName(missionName);

            // Remove local resources
            if (deleteLocal && removedMission != null) deleteLocalResources(sync, removedMission);

            // Delete the mission on the server if requested.
            if (deleteRemote) return sync.getMissionTransfers().deleteMissionByName(missionName, removedSubToken).thenApply(mission -> {
                logger.info("Deleted mission '{}' off server '{}", mission.name(), connection.getUri());
                return new IDataSyncManager.MissionDeletion(deleteLocal, true, null);
            }).exceptionally(error -> {
                ApiException cause = (ApiException) error.getCause();
                logger.error("Failed to deleted mission '{}' off server '{}", missionName, connection.getUri(), cause);
                return new IDataSyncManager.MissionDeletion(deleteLocal, true, cause);
            });

            // Otherwise, just remove our subscription so we stop receiving CoT events.
            if (removedSubToken != null) sync.getMissionTransfers().deleteMissionSubscriptionByName(missionName, removedSubToken);

            // Yield future of only whether local files were deleted.
            return completedFuture(new IDataSyncManager.MissionDeletion(deleteLocal, false, null));
        }).orElseGet(() -> {
            logger.error("Cannot delete mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
            return CompletableFuture.completedFuture(new MissionDeletion(deleteLocal, deleteRemote, new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER)));
        });
    }

    private void deleteLocalResources(@Nonnull MissionSync sync, @Nonnull Mission removedMission)
    {
        MissionManager manager = sync.getMissionManager();
        MissionResources resources = sync.getMissionResources();

        // Delete all local CoT files for map items that are not used by other downloaded feeds
        removedMission.uids().stream().map(MissionAdd.TString::data)
                .filter(uid -> !manager.isMapItemUidOwned(uid))
                .forEach(uid -> {
                    resources.cotFile(uid).delete();
                    IMapObject mo = mapObjectFinder.findMapObjectByUid(uid);
                    if (mo != null) mapObjectManager.deleteMapObject(mo);
                });

        // Same deal for local file attachments
        removedMission.contents().stream().map(MissionAdd.TResource::data)
                .filter(resource -> !manager.isResourceOwned(resource.hash()))
                .forEach(resource -> resources.resourceFile(resource).delete());
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionAddFile> addFile(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull File file)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.addFile(missionName, file))
                .orElseGet(() -> {
                    logger.error("Cannot add file to mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
                    ApiException failure = new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER);
                    return CompletableFuture.completedFuture(new MissionAddFile(null, failure, CompletableFuture.failedFuture(failure)));
                });
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionAddMapItem> addMapItem(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull IMapObject mapObject)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.addMapItem(missionName, mapObject))
                .orElseGet(() -> {
                    logger.error("Cannot add map item to mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
                    ApiException error = new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER);
                    return CompletableFuture.completedFuture(MissionAddMapItem.failed(error));
                });
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionAddLogEntry> addUserLog(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull String entry,
                                                            @Nullable List<String> keywords, @Nullable List<File> files, @Nullable IMapObject mapItem)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.addUserLog(missionName, entry, keywords, files, mapItem))
                .orElseGet(() -> {
                    logger.error("Cannot add user log to mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
                    ApiException error = new ApiException(ApiException.MESSAGE_UNTRACKED_SERVER);
                    return CompletableFuture.completedFuture(new MissionAddLogEntry(
                            Collections.emptyList(),
                            CompletableFuture.failedFuture(error),
                            CompletableFuture.failedFuture(error),
                            error));
                });
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionDownload> subscribe(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nullable String password)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> {
                    transfers.createMissionSubscriptionByName(missionName, password);
                    return transfers.downloadMissionByName(missionName, password, null);
                })
                .orElseGet(() -> {
                    logger.error("Cannot download to mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
                    return CompletableFuture.failedFuture(new IllegalStateException("Untracked server: " + connection.getUri()));
                });
    }

    @Nonnull
    @Override
    public CompletableFuture<MissionDownload> subscribeWithToken(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull String token)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.downloadMissionByName(missionName, null, token))
                .orElseGet(() -> {
                    logger.error("Cannot download to mission '{}' as the associated server '{}' is not tracked", missionName, connection.getUri());
                    return CompletableFuture.failedFuture(new IllegalStateException("Untracked server: " + connection.getUri()));
                });
    }

    @Override
    public void unsubscribe(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        String missionSubscriptionToken = getMissionSubscriptionToken(connection, missionName);
        if (missionSubscriptionToken != null)
            sync(connection).ifPresent(sync -> {
                sync.getMissionManager().removeMissionSubscriptionTokenByName(missionName);
                sync.getMissionTransfers().deleteMissionSubscriptionByName(missionName, missionSubscriptionToken);
            });
    }

    @Nullable
    @Override
    public CotEvent createInviteCot(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        Mission mission = getMission(connection, missionName);
        if (mission == null) return null;
        return cotExporterManager.export(new MissionMapObject(mission.name(), mission.creatorUid(),
                connection.getConnectionConfig().getNetConnectString()));
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> inviteToMission(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull InvitationType invitationType, @Nonnull Collection<String> contacts)
    {
        return sync(connection)
                .map(MissionSync::getMissionTransfers)
                .map(transfers -> transfers.sendInvitationByName(missionName, invitationType, contacts))
                .orElseGet(() -> CompletableFuture.failedFuture(new IllegalStateException("Cannot invite to mission on untracked TAK Server: " + connection.getUri())));

    }

    @Nonnull
    @Override
    public Map<ITakServerConnection, Collection<Mission>> currentMissions()
    {
        return syncs()
                .map(MissionSync::getMissionManager)
                .collect(Collectors.toMap(MissionManager::getConnection, MissionManager::getMissions));
    }

    @Nullable
    @Override
    public Mission getMission(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        return sync(connection)
                .map(MissionSync::getMissionManager)
                .map(manager -> {
                    Mission mission = manager.getDownloadedMissionByName(missionName);
                    if (mission == null) mission = manager.getMissionByName(missionName);
                    return mission;
                })
                .orElse(null);
    }

    @Nonnull
    @Override
    public CompletableFuture<Mission> createMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission, @Nullable String password)
    {
        return sync(connection)
                .map(sync -> sync.getMissionTransfers().createMission(mission, password))
                .orElseGet(() -> CompletableFuture.failedFuture(new IllegalStateException("Cannot create mission on untracked TAK Server: " + connection.getUri())));
    }

    /**
     * @param missionName Name of mission to search for.
     * @param missionGuid Guid of mission to search for.
     * @return Sync manager containing the given mission by name or guid.
     * {@code null} if no manager contains a mission by the given name or guid.
     */
    @Nullable
    private MissionSync findContainingSync(@Nullable String missionName, @Nullable String missionGuid)
    {
        return syncs().filter(s -> (missionName != null && s.getMissionManager().getMissionByName(missionName) != null)
                || (missionGuid != null && s.getMissionManager().getMissionByGuid(missionGuid) != null))
                .findFirst().orElse(null);
    }

    /**
     * @param connection Connection to get the sync manager of.
     * @return Optional of sync manager for the given connection.
     */
    @Nonnull
    protected Optional<MissionSync> sync(@Nonnull ITakServerConnection connection)
    {
        return Optional.ofNullable(missionSyncs.get(key(connection)));
    }

    /**
     * @return Stream of all sync managers.
     */
    @Nonnull
    protected Stream<MissionSync> syncs()
    {
        return missionSyncs.values().stream();
    }

    /**
     * @param connection Connection to generate a key for.
     * @return Lookup key for the given connection.
     * @since 5.1
     */
    @Nonnull
    public static String key(@Nonnull ITakServerConnection connection)
    {
        // A connection should consider not only the URL of the remote server, but also the user
        // when creating a unique key. Some real-world use includes multiple people sharing the same
        // system and switching sign-ins. Ensuring data is unique per server/user combo allows for
        // such cases.
        var user = Objects.requireNonNullElse(connection.getConnectionConfig().getUsername(), "default");
        return connection.getUri() + ":" + user;
    }

    /**
     * Increments or decrements the counter attribute {@link StandardAttributeNames#DATA_SYNC_PENDING_ATTACHMENTS}
     * on the given map item. When the counter reaches zero the attribute is removed.
     *
     * @param mapItem Map item to update.
     * @param change Difference to adjust counter by.
     * @since 5.3
     */
    private static void updatePendingAttachmentCounter(@Nonnull MapItem mapItem, int change)
    {
        int currentCounter = mapItem.getMetaInteger(DATA_SYNC_PENDING_ATTACHMENTS, 0);
        int newCounter = currentCounter + change;
        if (newCounter <= 0)
        {
            mapItem.getAttributeSet().removeAttribute(DATA_SYNC_PENDING_ATTACHMENTS);
        } else
        {
            mapItem.setMetaInteger(DATA_SYNC_PENDING_ATTACHMENTS, newCounter);
        }
    }

    /**
     * Outline of an arbitrary queued change to a list of missions.
     * @param destinations Destination missions to publish to.
     * @param publishTask Task to publish a change to the given missions.
     * @since 5.8
     */
    private record PendingPublish(@Nonnull List<Mission> destinations, @Nonnull Runnable publishTask)
    {
    }
}
