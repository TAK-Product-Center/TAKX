package gov.tak.core.comms.takserver.mission;

import com.atakmap.filesystem.HashingUtils;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.TimeThresholdOption;
import gov.tak.api.comms.takserver.mission.CotEndpoints;
import gov.tak.api.comms.takserver.mission.MissionEndpoints;
import gov.tak.api.comms.takserver.mission.SyncEndpoints;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.call.IConfigurableEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionSubscription;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionSubscriptionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMission;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionByGuid;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionContentKeys;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UidDetails;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.core.comms.cot.CotExporterManager;
import gov.tak.core.comms.takserver.DataPackageManager;
import gov.tak.core.comms.takserver.DataSyncManager;
import gov.tak.core.comms.takserver.TakServerUtilities;
import gov.tak.platform.cot.importer.CotImportHelper;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.file.FileTypeManager;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_DOWNLOAD_UPDATES;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_LARGE_FILE_PROMPT;
import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_THRESHOLD;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.apache.commons.io.FileUtils.ONE_MB;

/**
 * Data-sync component for initiating and tracking any IO operations with the mission API.
 *
 * @since 5.7
 */
public class MissionTransfers implements IMissionSyncComponent
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.####");

    /** Mission guids and names (Mixed use) */
    private final Set<String> pendingMissionModelDownloads = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** File Sha256 --> Future */
    private final Map<String, CompletableFuture<IDataSyncManager.MissionResourceDownload>> pendingResourceDownloads = new ConcurrentHashMap<>();
    /** Item UID --> Future */
    private final Map<String, CompletableFuture<IDataSyncManager.MissionMapItemDownload>> pendingMapItemDownloads = new ConcurrentHashMap<>();
    /** File path -> Future wrapper + target mission guid */
    private final Map<File, PendingFileUpload> pendingFileUploads = new ConcurrentHashMap<>();
    /** Item UID -> Future wrapper + target mission guid */
    private final Map<String, PendingMapItemUpload> pendingMapItemUploads = new ConcurrentHashMap<>();
    /** Item UID -> Future wrapper + target mission guid */
    private final Set<String> ongoingMapItemImports = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final INotificationService notificationService;
    private final ICoreExecutorService executorService;
    private final CotImportHelper cotImportHelper;
    private final TakServerApiClient takServerApiClient;
    private final DataSyncManager dataSyncManager;
    private final DataPackageManager dataPackageManager;
    private final CotExporterManager cotExporterManager;
    private final MissionManager missionManager;
    private final MissionResources missionResources;
    private final MissionEndpoints missionEndpoints;
    private final SyncEndpoints syncEndpoints;
    private final CotEndpoints cotEndpoints;
    private ITakServerConnection connection;

    MissionTransfers(@Nonnull ICoreExecutorService executorService, @Nonnull INotificationService notificationService,
                     @Nonnull CotImportHelper cotImportHelper, @Nonnull DataSyncManager dataSyncManager,
                     @Nonnull DataPackageManager dataPackageManager, @Nonnull TakServerApiClient takServerApiClient,
                     @Nonnull CotExporterManager cotExporterManager, @Nonnull MissionResources missionResources,
                     @Nonnull MissionManager missionManager, @Nonnull ITakServerConnection connection)
    {
        this.executorService = executorService;
        this.notificationService = notificationService;
        this.cotImportHelper = cotImportHelper;
        this.takServerApiClient = takServerApiClient;
        this.dataSyncManager = dataSyncManager;
        this.dataPackageManager = dataPackageManager;
        this.cotExporterManager = cotExporterManager;
        this.missionManager = missionManager;
        this.missionResources = missionResources;
        this.connection = connection;

        missionEndpoints = takServerApiClient.missionEndpoints(connection);
        syncEndpoints = takServerApiClient.syncEndpoints(connection);
        cotEndpoints = takServerApiClient.cotEndpoints(connection);
    }

    /**
     * Requests the creation of a given mission.
     *
     * @param mission  Model of mission to create.
     * @param password Password to assign to the given mission.
     * @return Future of created remote mission.
     */
    @Nonnull
    public CompletableFuture<Mission> createMission(@Nonnull Mission mission, @Nullable String password)
    {
        if (logger.isDebugEnabled()) logger.debug("Creating mission '{}' on server: {}", mission.name(), connection.getUri());
        return missionEndpoints.createMission(mission, password)
                .asyncGet(executorService)
                .thenCompose(missionResponse -> {
                    // When we see a response, that means TAK Server has created the remote mission.
                    // We subscribe to the mission after since that is likely intended by users in most cases.
                    return createMissionSubscriptionByName(missionResponse.name(), password).whenComplete((subscription, error) -> {
                        // Register the mission when we get the subscription response back.
                        if (subscription != null)
                        {
                            missionManager.putDownloadedMission(missionResponse, password, subscription.token(), true);
                        } else
                        {
                            missionManager.putMission(missionResponse, true);
                            logger.error("Failed to subscribe to newly created feed '{}' on server '{}",
                                    mission.name(), connection.getUri());
                        }
                    }).thenApply(sub -> {
                        // Keywords are not part of the initial mission creation, so we need to now set the keywords
                        // on the created mission. For password protected missions, we need to use the subscription token
                        // for this action.
                        List<String> keywords = mission.keywords();
                        if (!keywords.isEmpty())
                        {
                            String token = missionManager.getMissionSubscriptionTokenByName(mission.name());
                            missionEndpoints.setMissionKeywords(mission.name(), keywords)
                                    .setAuthorizationToken(token)
                                    .asyncGet(executorService)
                                    .whenComplete((updatedMission, error) -> {
                                        if (updatedMission != null)
                                        {
                                            // Update the model (passing null for the pass/token here is fine, it won't override the values assigned above)
                                            missionManager.putDownloadedMission(updatedMission, null, null, true);
                                        } else if (error != null)
                                        {
                                            logger.error("Failed to assign keywords to newly created feed '{}' on server '{}",
                                                    mission.name(), connection.getUri());
                                        }
                                    });
                        }
                        return missionResponse;
                    });
                })
                .whenComplete((response, error) -> {
                    if (response != null)
                    {
                        logger.info("Created feed '{}' on server '{}", mission.name(), connection.getUri());
                    } else
                    {
                        String message = "Server '" + connection.getUri() + "' refused creation of new feed '" + mission.name() + "'";
                        logger.error(message, error);
                        notificationService.show(Notification.builder(message, Notification.Type.Error).build(), INotificationService.MANUAL_CLOSE);
                    }
                });
    }

    /**
     * Requests the creation of a subscription for the given mission.
     *
     * @param missionName Mission name.
     * @param password    Mission password.
     * @return Future of the allocated subscription.
     */
    @Nonnull
    public CompletableFuture<MissionSubscription> createMissionSubscriptionByName(@Nonnull String missionName, @Nullable String password)
    {
        if (logger.isDebugEnabled()) logger.debug("Creating subscription to mission name='{}' on server: {}", missionName, connection.getUri());
        long secAgo = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli() / 1000; // Seconds to epoch, with some leeway
        CreateMissionSubscription createSubscription = missionEndpoints.createMissionSubscription(missionName)
                .withSecondsAgo(secAgo)
                .withUid(takServerApiClient.getClientUid().toString());
        if (password != null && !password.isEmpty()) createSubscription.withPassword(password);
        return createSubscription.asyncGet(executorService).whenComplete((sub, err) -> {
            if (sub != null) missionManager.putMissionSubscriptionTokenByName(missionName, sub.token());
            if (err != null) logger.error("Failed to subscribe to mission '{}'", missionName);
        });
    }

    /**
     * Requests the creation of a subscription for the given mission.
     *
     * @param missionGuid Mission Guid.
     * @param password    Mission password.
     * @return Future of the allocated subscription.
     */
    @Nonnull
    public CompletableFuture<MissionSubscription> createMissionSubscriptionByGuid(@Nonnull String missionGuid, @Nullable String password)
    {
        if (logger.isDebugEnabled()) logger.debug("Creating subscription to mission guid='{}' on server: {}", missionGuid, connection.getUri());
        long secAgo = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli() / 1000; // Seconds to epoch, with some leeway
        CreateMissionSubscriptionByGuid createSubscription = missionEndpoints.createMissionSubscriptionByGuid(missionGuid)
                .withSecondsAgo(secAgo)
                .withUid(takServerApiClient.getClientUid().toString());
        if (password != null && !password.isEmpty()) createSubscription.withPassword(password);
        return createSubscription.asyncGet(executorService).whenComplete((sub, err) -> {
            if (sub != null) missionManager.putMissionSubscriptionTokenByGuid(missionGuid, sub.token());
            if (err != null) logger.error("Failed to subscribe to mission '{}'", missionGuid);
        });
    }

    /**
     * Requests the remote list of missions.
     * All values will be automatically tracked by the {@link MissionManager}.
     *
     * @return Future of remote missions.
     */
    @Nonnull
    public CompletableFuture<List<Mission>> updateBaseMissionModels()
    {
        if (logger.isDebugEnabled()) logger.debug("Fetching mission list from server: {}", connection.getUri());
        return missionEndpoints.getAllMissions().asyncGet(executorService).whenComplete((missions, error) -> {
            if (missions != null)
            {
                for (Mission mission : missions)
                {
                    missionManager.putMission(mission, true);
                }
            } else if (error != null)
            {
                logger.error("Failed to fetch mission list from server: {}", connection.getUri(), error);
            }
        });
    }

    /**
     * Requests the remote mission.
     * The yielded value will be automatically tracked by the {@link MissionManager}.
     *
     * @param missionGuid Guid of mission to look up.
     *
     * @return Future of remote mission.
     */
    @Nonnull
    public CompletableFuture<Mission> updateBaseMissionModel(@Nonnull String missionGuid)
    {
        return updateBaseMissionModel(missionGuid, false);
    }

    /**
     * Requests the remote mission.
     * The yielded value will be automatically tracked by the {@link MissionManager}.
     *
     * @param missionGuid Guid of mission to look up.
     * @param silent      Flag to mute error logging for missions not found.
     *
     * @return Future of remote mission.
     */
    @Nonnull
    public CompletableFuture<Mission> updateBaseMissionModel(@Nonnull String missionGuid, boolean silent)
    {
        // There are a few different contexts where this method is called.
        // However, all of them will only use the level of authorization we have stored in our local cache.
        // If the specific mission endpoint fails, we will fall back to the 'all' missions endpoint and filter by the guid.
        if (logger.isDebugEnabled()) logger.debug("Fetching mission guid={} from server: {}", missionGuid, connection.getUri());
        return missionEndpoints.getMissionByGuid(missionGuid)
                .setAuthorizationToken(missionManager.getMissionSubscriptionTokenByGuid(missionGuid))
                .asyncGet(executorService)
                .exceptionallyCompose(error -> missionEndpoints.getAllMissions().asyncGet(executorService)
                        .thenApply(list -> list.stream()
                                .filter(mission -> mission.guid().equals(missionGuid))
                                .findFirst().orElse(null)))
                .whenComplete((mission, error) -> {
                    if (mission != null)
                    {
                        missionManager.putMission(mission, true);
                    } else if (error != null && !silent)
                    {
                        logger.error("Failed to fetch model for mission '{}'", missionGuid, error);
                    }
                });
    }

    /**
     * Initiates downloading all associated resources of the given mission.
     *
     * @param missionName Name of mission to download.
     * @param password    Password associated with the mission, if any.
     * @return Future of downloaded mission and its associated resources.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionDownload> downloadMissionByName(@Nonnull String missionName, @Nullable String password, @Nullable String token)
    {
        if (logger.isDebugEnabled()) logger.debug("Downloading mission name='{}' from server: {}", missionName, connection.getUri());
        GetMission getMission = missionEndpoints.getMission(missionName).withChanges(true).withLogs(true).withPassword(password);
        return downloadMission(missionName, password, token, getMission);
    }

    /**
     * Initiates downloading all associated resources of the given mission.
     *
     * @param missionGuid Guid of mission to download.
     * @param password    Password associated with the mission, if any.
     * @param token       Subscription token associated with the mission, if previously subscribed.
     * @return Future of downloaded mission and its associated resources.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionDownload> downloadMissionByGuid(@Nonnull String missionGuid, @Nullable String password, @Nullable String token)
    {
        if (logger.isDebugEnabled()) logger.debug("Downloading mission guid={} from server: {}", missionGuid, connection.getUri());
        GetMissionByGuid getMission = missionEndpoints.getMissionByGuid(missionGuid).withChanges(true).withLogs(true).withPassword(password);
        return downloadMission(missionGuid, password, token, getMission);
    }

    /**
     * Initiates downloading all associated resources of the given mission.
     *
     * @param missionKey Name or guid of mission to download. At this point, just used for state tracking.
     * @param password   Password associated with the mission, if any.
     * @param token      Subscription token associated with the mission, if previously subscribed.
     * @param endpoint   Endpoint that will actually fetch the mission model from the server.
     * @return Future of downloaded mission and its associated resources.
     */
    @Nonnull
    private CompletableFuture<IDataSyncManager.MissionDownload> downloadMission(@Nonnull String missionKey, @Nullable String password, @Nullable String token, @Nonnull AbstractEndpointCaller<Mission> endpoint)
    {
        // Add the mission name/guid to the 'pending' set.
        // This is used by the data-sync manager to determine how far along in a mission download process we are.
        // It doesn't particularly matter whether this is a name or guid, so long as they are unique.
        pendingMissionModelDownloads.add(missionKey);

        // Fetch the mission then begin downloading associated map items & resources.
        return endpoint.setAuthorizationToken(token).asyncGet(executorService)
                .whenComplete((mission, error) -> pendingMissionModelDownloads.remove(missionKey))
                .thenApply(mission -> {
                    missionManager.putDownloadedMission(mission, password, token, true);
                    return IDataSyncManager.MissionDownload.newSuccessfulDownload(mission, syncResources(mission), syncMapItems(mission));
                }).exceptionally(throwable -> {
                    String connectionName = connection.getDisplayName();
                    String errorMessage = String.format("Failed fetching mission '%s' from server '%s'", missionKey, connectionName);
                    TakServerUtilities.handleConnectionError(notificationService, connectionName, throwable, errorMessage);
                    if (logger.isDebugEnabled()) logger.debug(errorMessage, throwable);

                    // All mission endpoint failures are wrapped in an ApiException, which in futures are wrapped by CompletionException.
                    return IDataSyncManager.MissionDownload.newFailedDownload((ApiException) throwable.getCause());
                });
    }

    /**
     * Requests the remote deletion of the given mission.
     * Upon success the server should fire off a {@link gov.tak.api.cot.CotTypes#MISSION_DELETED} event,
     * which will handle further tracking processed.
     *
     * @param missionName Mission name.
     * @param token       Mission subscription token.
     * @return Future of remote deleted mission.
     */
    @Nonnull
    public CompletableFuture<Mission> deleteMissionByName(@Nonnull String missionName, @Nullable String token)
    {
        if (logger.isDebugEnabled()) logger.debug("Deleting mission name='{}' from server: {}", missionName, connection.getUri());
        return deleteMission(missionEndpoints.removeMission(missionName), token);
    }

    /**
     * Requests the remote deletion of the given mission.
     * Upon success the server should fire off a {@link gov.tak.api.cot.CotTypes#MISSION_DELETED} event,
     * which will handle further tracking processed.
     *
     * @param missionGuid Mission guid.
     * @param token       Mission subscription token.
     * @return Future of remote deleted mission.
     */
    @Nonnull
    public CompletableFuture<Mission> deleteMissionByGuid(@Nonnull String missionGuid, @Nullable String token)
    {
        if (logger.isDebugEnabled()) logger.debug("Deleting mission guid={} from server: {}", missionGuid, connection.getUri());
        return deleteMission(missionEndpoints.removeMissionByGuid(missionGuid), token);
    }

    @Nonnull
    private CompletableFuture<Mission> deleteMission(@Nonnull IConfigurableEndpointCaller<Mission> endpoint, @Nullable String token)
    {
        if (token != null) endpoint.setAuthorizationToken(token);
        return endpoint.asyncGet(executorService).thenApply(mission -> {
            missionManager.removeMissionByGuid(mission.guid());
            return mission;
        });
    }

    /**
     * Request unsubscribing from the given mission.
     * Upon success the server should stop sending {@link gov.tak.api.cot.CotTypes#MISSION_CHANGED_CONTENT} events.
     *
     * @param missionName Mission name.
     * @param token       Mission subscription token.
     * @return Future of acknowledgement.
     */
    @Nonnull
    public CompletableFuture<Void> deleteMissionSubscriptionByName(@Nonnull String missionName, @Nonnull String token)
    {
        if (logger.isDebugEnabled()) logger.debug("Removing subscription from mission name='{}' on server: {}", missionName, connection.getUri());
        String clientUid = takServerApiClient.getClientUid().toString();
        return missionEndpoints.removeMissionSubscription(missionName)
                .withUid(clientUid)
                .setAuthorizationToken(token)
                .asyncGet(executorService);
    }

    /**
     * Request unsubscribing from the given mission.
     * Upon success the server should stop sending {@link gov.tak.api.cot.CotTypes#MISSION_CHANGED_CONTENT} events.
     *
     * @param missionGuid Mission guid.
     * @param token       Mission subscription token.
     * @return Future of acknowledgement.
     */
    @Nonnull
    public CompletableFuture<Void> deleteMissionSubscriptionByGuid(@Nonnull String missionGuid, @Nonnull String token)
    {
        if (logger.isDebugEnabled()) logger.debug("Removing subscription from mission guid={} on server: {}", missionGuid, connection.getUri());
        String clientUid = takServerApiClient.getClientUid().toString();
        return missionEndpoints.removeMissionSubscriptionByGuid(missionGuid)
                .withUid(clientUid)
                .setAuthorizationToken(token)
                .asyncGet(executorService);
    }

    /**
     * Requests the server send invites to the given contacts for the given mission.
     *
     * @param missionName    Mission name.
     * @param invitationType Contact format of the following list.
     * @param contacts       List of contacts to send the invitation to. See {@link InvitationType}.
     * @return Future of acknowledgement.
     */
    @Nonnull
    public CompletableFuture<Void> sendInvitationByName(@Nonnull String missionName, @Nonnull InvitationType invitationType, @Nonnull Collection<String> contacts)
    {
        // Map contacts to invitation models. The server will fill in the token and role based on what is configured for the mission.
        if (logger.isDebugEnabled()) logger.debug("Sending invites to mission name='{}' to {} on server: {}", missionName, contacts, connection.getUri());
        List<MissionInvitation> invitations = contacts.stream()
                .map(contact -> new MissionInvitation(missionName, contact, invitationType, takServerApiClient.getClientUid().toString(), Instant.now(), null, null))
                .toList();
        return missionEndpoints.sendMissionInvitations(missionName, invitations)
                .setAuthorizationToken(missionManager.getMissionSubscriptionTokenByName(missionName))
                .asyncGet(executorService);
    }

    /**
     * Initiates downloading resources associated with the given mission.
     *
     * @param mission Holds the {@link Mission#contents()} to sync.
     * @return List of futures for each download of a {@link Resource} associated with the mission's contents.
     */
    @Nonnull
    protected List<CompletableFuture<IDataSyncManager.MissionResourceDownload>> syncResources(@Nonnull Mission mission)
    {
        List<MissionAdd.TResource> contents = Objects.requireNonNullElse(mission.contents(), Collections.emptyList());
        if (logger.isDebugEnabled()) logger.debug("Synchronizing file-resources[{}] of mission name='{}' on server: {}", contents.size(), mission.name(), connection.getUri());
        return contents.stream()
                .map(MissionAdd.TResource::data)
                .map(resource -> missionResources.hasLocalResource(resource)
                        ? getLocalResource(mission, resource)
                        : downloadToLocalResource(mission, resource, false))
                .filter(Objects::nonNull) // filter out any resources that did not qualify for auto-download
                .toList();
    }

    /**
     * Initiates downloading map items associated with the given mission.
     *
     * @param mission Holds the {@link Mission#uids()} to sync.
     * @return List of futures for each download of a {@link MissionAdd} associated with the mission's map items.
     */
    @Nonnull
    protected List<CompletableFuture<IDataSyncManager.MissionMapItemDownload>> syncMapItems(@Nonnull Mission mission)
    {
        List<MissionAdd.TString> uids = Objects.requireNonNullElse(mission.uids(), Collections.emptyList());
        if (logger.isDebugEnabled()) logger.debug("Synchronizing map-items[{}] of mission name='{}' on server: {}", uids.size(), mission.name(), connection.getUri());
        return uids.stream()
                .map(cot -> missionResources.hasLocalUpToDateCot(cot)
                        ? getLocalCot(mission, cot)
                        : downloadToLocalCot(mission, cot, false))
                .toList();
    }

    /**
     * Adds a map item to the mission.
     *
     * @param missionName Name of mission to add the map item to.
     * @param mapObject   Map item to add.
     * @return Future of the addition.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionAddMapItem> addMapItem(@Nonnull String missionName, @Nonnull IMapObject mapObject)
    {
        // Create new future for the map item addition.
        // We will complete it in when we receive a mission update that has that map item attached, indicating it has been uploaded.
        String uid = mapObject.getUid();

        if (logger.isDebugEnabled()) logger.debug("Adding map-item '{}' to mission name='{}' on server: {}", mapObject.getName(), missionName, connection.getUri());
        CompletableFuture<IDataSyncManager.MissionAddMapItem> future = new CompletableFuture<>();
        pendingMapItemUploads.put(uid, new PendingMapItemUpload(missionName, future));
        try
        {
            File tempSyncPackage = dataPackageManager.createNewTempDataPackage("sync-upload-" + UUID.randomUUID());
            String itemZipPath = dataPackageManager.addMapObjectToDataPackage(tempSyncPackage, mapObject, file -> {
                // Data-sync files that already are a part of the mission do not need to be attached again.
                String sha256 = HashingUtils.sha256sum(file);
                Mission mission = missionManager.getDownloadedMissionByName(missionName);
                if (mission == null) mission = missionManager.getMissionByName(missionName);
                return mission == null || mission.contents().stream()
                        .noneMatch(t -> sha256.equals(t.data().hash()));
            });
            if (itemZipPath == null)
            {
                tempSyncPackage.delete();
                throw new IOException("Map object had no associated CoT: " + mapObject.getName());
            }
            missionEndpoints.addMissionPackage(missionName, Files.readAllBytes(tempSyncPackage.toPath()))
                    .setAuthorizationToken(missionManager.getMissionSubscriptionTokenByName(missionName))
                    .asyncGet(executorService)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .whenComplete((incompatibilities, error) -> {
                        pendingMapItemUploads.remove(uid);
                        if (error != null)
                        {
                            // Explicit error on our end.
                            future.complete(IDataSyncManager.MissionAddMapItem.failed(new ApiException(error)));
                        } else if (!incompatibilities.isEmpty())
                        {
                            // The endpoint should yield a list of *incompatibilities* with the requested change.
                            // So we should hopefully see an empty list each time. But if we don't...
                            future.complete(IDataSyncManager.MissionAddMapItem.failed(new ApiException("Server denied update to '" + missionName +
                                    "' - requested changes contained incompatibilities")));
                        } else
                        {
                            // Write the current state of the map object to the expected storage location for mission CoT items.
                            // This will prevent us from re-fetching it later when the server notifies us of the update to the mission.
                            for (File attachment : mapObject.getAttachments()) cacheLocalResource(attachment);
                            CotEvent cotContent = cotExporterManager.export(mapObject);
                            String cotText = cotContent == null ? null : cotContent.toString();
                            cacheLocalCot(uid, cotText);

                            // No errors on either side, all good to go.
                            // - Complete the base future of the addition
                            // - Create the future for the following mission update
                            // - Put local CoT of map object into local place so we don't try and refetch it from the server later
                            String missionGuid = missionManager.getGuidFromName(missionName);
                            CompletableFuture<Mission> updatedMissionFuture = updateBaseMissionModel(missionGuid, true);
                            future.complete(new IDataSyncManager.MissionAddMapItem(uid, null, updatedMissionFuture));
                        }
                        tempSyncPackage.delete();
                    });
        } catch (Throwable t)
        {
            future.complete(IDataSyncManager.MissionAddMapItem.failed(new ApiException(t)));
            pendingMapItemUploads.remove(uid);
        }
        return future;
    }

    /**
     * Adds a file to the mission.
     *
     * @param missionName Name of mission to add the file to.
     * @param file        File to add.
     * @return Future of the addition.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionAddFile> addFile(@Nonnull String missionName, @Nonnull File file)
    {
        // Read the file contents or yield a failed future.
        byte[] contents;
        try
        {
            contents = Files.readAllBytes(file.toPath());
        } catch (IOException ex)
        {
            ApiException failure = new ApiException(ex);
            return completedFuture(new IDataSyncManager.MissionAddFile(null, failure, failedFuture(failure)));
        }

        // Upload the contents, then when we get a response back attach the file to the mission.
        String name = file.getName();
        if (logger.isDebugEnabled()) logger.debug("Adding file-resource '{}' to mission name='{}' on server: {}", name, missionName, connection.getUri());
        CompletableFuture<IDataSyncManager.MissionAddFile> future = new CompletableFuture<>();
        PendingFileUpload fileUpload = new PendingFileUpload(missionName, future);
        pendingFileUploads.put(file, fileUpload);
        String mimeType = Optional.ofNullable(FileTypeManager.getTypeFromExtension(file))
                .map(FileTypeManager.FileType::mimeType)
                .orElse(FileTypeManager.DEFAULT_BINARY_MIME_TYPE);
        syncEndpoints.upload(mimeType, contents).withName(name).asyncGet(executorService)
                .thenApply(response -> {
                    String hash = response.hash();
                    cacheLocalResource(file, hash, name);
                    List<String> hashes = List.of(hash);
                    MissionContentKeys keys = new MissionContentKeys(Collections.emptyList(), hashes);
                    CompletableFuture<Mission> additionFuture = missionEndpoints.addMissionContent(missionName, keys)
                            .setAuthorizationToken(missionManager.getMissionSubscriptionTokenByName(missionName))
                            .asyncGet(executorService);
                    return new IDataSyncManager.MissionAddFile(response, null, additionFuture);
                })
                .exceptionally(ex -> {
                    ApiException failure = new ApiException(ex);
                    return new IDataSyncManager.MissionAddFile(null, failure, failedFuture(failure));
                }).whenComplete((add, error) -> {
                    pendingFileUploads.remove(file);
                    if (add != null)
                    {
                        future.complete(add);
                    } else if (error != null)
                    {
                        future.completeExceptionally(error);
                    }
                });
        return future;
    }

    /**
     * Cache the given map item <i>(denoted by uid)</i> CoT in the mission cot directory.
     *
     * @param uid Map object CoT.
     * @param cotText Text of the CoT.
     */
    private void cacheLocalCot(@Nonnull String uid, @Nullable String cotText)
    {
        try
        {
            File cotFile = missionResources.cotFile(uid);
            File cotDir = cotFile.getParentFile();
            if (!cotDir.exists() && !cotDir.mkdirs()) logger.warn("Failed to create containing folder for mission CoT");
            if (cotText != null) Files.writeString(cotFile.toPath(), cotText);
        } catch (Throwable t)
        {
            logger.error("Failed to write local CoT for map-item '{}'", uid);
        }
    }

    /**
     * Cache the given file in the mission resources directory.
     *
     * @param originalFile File to cache.
     */
    private void cacheLocalResource(@Nonnull File originalFile)
    {
        cacheLocalResource(originalFile, HashingUtils.sha256sum(originalFile), originalFile.getName());
    }

    /**
     * Cache the given file in the mission resources directory.
     *
     * @param originalFile File to cache.
     * @param hash SHA256 of file.
     * @param name File name.
     */
    private void cacheLocalResource(@Nonnull File originalFile, @Nonnull String hash, @Nonnull String name)
    {
        File resourceFile = missionResources.resourceFileByHashAndName(hash, name);
        if (resourceFile.exists()) return;
        File resourceDir = resourceFile.getParentFile();

        try
        {
            if (!resourceDir.exists() && !resourceDir.mkdirs()) logger.warn("Failed to create containing folder for mission resource");
            Files.copy(originalFile.toPath(), resourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable t)
        {
            logger.error("Failed to write local resource '{}'", name);
        }
    }

    /**
     * Cache the given file contents in the mission resources directory.
     *
     * @param fileContents File contents to cache.
     * @param hash SHA256 of file.
     * @param name File name.
     */
    private void cacheLocalResource(@Nonnull byte[] fileContents, @Nonnull String hash, @Nonnull String name)
    {
        File resourceFile = missionResources.resourceFileByHashAndName(hash, name);
        if (resourceFile.exists()) return;
        File resourceDir = resourceFile.getParentFile();

        try
        {
            if (!resourceDir.exists() && !resourceDir.mkdirs()) logger.warn("Failed to create containing folder for mission resource");
            Files.write(resourceFile.toPath(), fileContents);
        } catch (Throwable t)
        {
            logger.error("Failed to write local resource '{}'", name);
        }
    }

    /**
     * Uploads associated resources <i>(if provided)</i> and then creates a user log entry in the mission.
     *
     * @param missionName   Name of mission to add the user log to.
     * @param entryContent  Text content of the log entry.
     * @param entryKeywords Optional keywords describing the log entry.
     * @param files         Optional files to upload and link to the log entry.
     * @param mapItem       Optional map item to upload and link to the log entry.
     * @return Future of mission log entry addition and its associated resources.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionAddLogEntry> addUserLog(@Nonnull String missionName, @Nonnull String entryContent,
                                                                             @Nullable List<String> entryKeywords, @Nullable List<File> files,
                                                                             @Nullable IMapObject mapItem)
    {
        List<CompletableFuture<?>> requisiteFutures = new ArrayList<>();

        // Ensure files are uploaded
        List<CompletableFuture<IDataSyncManager.MissionAddFile>> fileAttachFutures;
        if (files != null && !files.isEmpty())
        {
            fileAttachFutures = new ArrayList<>(files.size());
            for (File file : files)
            {
                CompletableFuture<IDataSyncManager.MissionAddFile> addFileFuture = addFile(missionName, file);
                fileAttachFutures.add(addFileFuture);
                requisiteFutures.add(addFileFuture);
            }
        } else
        {
            fileAttachFutures = Collections.emptyList();
        }

        // Ensure map items are uploaded
        CompletableFuture<IDataSyncManager.MissionAddMapItem> mapItemAttachFuture;
        if (mapItem != null)
        {
            mapItemAttachFuture = addMapItem(missionName, mapItem);
            requisiteFutures.add(mapItemAttachFuture);
        } else
        {
            mapItemAttachFuture = null;
        }

        // When all requisite actions are done, submit the log entry
        if (logger.isDebugEnabled()) logger.debug("Adding user-log to mission name='{}' on server: {}", missionName, connection.getUri());
        CompletableFuture<?>[] requisiteFuturesArray = requisiteFutures.toArray(new CompletableFuture[0]);
        return allOf(requisiteFuturesArray).thenApply((v -> {
            String mapItemUid = mapItem == null ? null : mapItem.getUid();
            String creatorUid = takServerApiClient.getClientUid().toString();
            Set<String> keywordSet = entryKeywords == null ? Collections.emptySet() : new HashSet<>(entryKeywords);
            Set<String> fileHashes = files == null ? Collections.emptySet() : fileAttachFutures.stream()
                    .map(future -> future.getNow(null))
                    .filter(Objects::nonNull)
                    .filter(result -> result.response() != null)
                    .map(result -> result.response().hash())
                    .collect(Collectors.toSet());
            var logEntry = new LogEntry(entryContent, creatorUid, mapItemUid, Instant.now(), Collections.singleton(missionName), fileHashes, keywordSet);
            CompletableFuture<LogEntry> logEntryFuture = missionEndpoints.createLogEntry(logEntry)
                    .setAuthorizationToken(missionManager.getMissionSubscriptionTokenByName(missionName))
                    .asyncGet(executorService);
            return new IDataSyncManager.MissionAddLogEntry(fileAttachFutures, mapItemAttachFuture, logEntryFuture, null);
        }));
    }

    /**
     * Treats an already downloaded local CoT event as if it were freshly downloaded.
     * Called when {@link MissionResources#hasLocalUpToDateCot(MissionAdd)} is {@code true} during {@link #syncMapItems(Mission)}.
     *
     * @param mission Mission declaring the resource.
     * @param cot     Cot outline that is already downloaded.
     * @return Map item download future.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionMapItemDownload> getLocalCot(@Nonnull Mission mission, @Nonnull MissionAdd<String> cot)
    {
        File cotFile = missionResources.cotFile(cot);
        ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionDownloadListeners(), listener -> listener.onDownloadCot(connection, mission, cot, cotFile));
        return completedFuture(new IDataSyncManager.MissionMapItemDownload(cot, cotFile));
    }

    /**
     * Treats an already downloaded local resource as if it were freshly downloaded.
     * Called when {@link MissionResources#hasLocalResource(Resource)} is {@code true} during {@link #syncResources(Mission)}.
     *
     * @param mission  Mission declaring the resource.
     * @param resource Resource that is already download.
     * @return Resource download future.
     */
    @Nonnull
    public CompletableFuture<IDataSyncManager.MissionResourceDownload> getLocalResource(@Nonnull Mission mission, @Nonnull Resource resource)
    {
        File resourceFile = missionResources.resourceFile(resource);
        ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionDownloadListeners(), listener -> listener.onDownloadResource(connection, mission, resource, resourceFile));
        return completedFuture(new IDataSyncManager.MissionResourceDownload(resource, resourceFile));
    }

    /**
     * Downloads the given resource.
     *
     * @param mission        Mission declaring the resource.
     * @param resource       Resource to download.
     * @param manualDownload {@code true} indicates the user has requested the download and the file should be
     *                       downloaded regardless of file age or size limit preferences
     * @return Resource download future or {@code null} if the request was ignored due to size or age limits
     */
    @Nullable
    public CompletableFuture<IDataSyncManager.MissionResourceDownload> downloadToLocalResource(@Nonnull Mission mission,
                                                                                               @Nonnull Resource resource,
                                                                                               boolean manualDownload)
    {
        // Skip if we're already downloading this file
        String hash = resource.hash();
        CompletableFuture<IDataSyncManager.MissionResourceDownload> existingFuture = pendingResourceDownloads.get(hash);
        if (existingFuture != null) return existingFuture;

        // Create file path & parent directory
        File resourceFile = missionResources.resourceFile(resource);
        if (!manualDownload &&
                (!autoDownloadContent(resource) || isAgeOverThreshold(resource.name(), resource.submissionTime())))
        {
            // Return the empty file and notify all listeners; no future is required. The user will need to manually
            // request the download
            ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionDownloadListeners(),
                    listener -> listener.onDownloadResource(connection, mission, resource, resourceFile));
            return null;
        }
        File parentDir = resourceFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) logger.warn("Failed to create directory: {}", parentDir);

        // Download to path
        if (logger.isDebugEnabled()) logger.debug("Downloading file-resource '{}' from mission name='{}' on server: {}", resourceFile.getName(), mission.name(), connection.getUri());
        CompletableFuture<IDataSyncManager.MissionResourceDownload> downloadFuture = new CompletableFuture<>();
        pendingResourceDownloads.put(hash, downloadFuture);
        syncEndpoints.getContent().withHash(hash).asyncGetRaw(executorService).whenComplete((raw, error) -> {
            pendingResourceDownloads.remove(hash);
            if (error != null)
            {
                logger.error("Failed downloading resource '{}' from mission name='{}' on server: {}",
                        resource.name(), mission.name(), connection.getUri(), error);
                downloadFuture.completeExceptionally(error);
            }
        }).thenAccept((bytes) -> {
            // Cache the results locally
            cacheLocalResource(bytes, resource.hash(), resource.name());

            // Notify listeners that we downloaded a new resource file
            ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionDownloadListeners(), listener -> listener.onDownloadResource(connection, mission, resource, resourceFile));

            downloadFuture.complete(new IDataSyncManager.MissionResourceDownload(resource, resourceFile));
        });
        return downloadFuture;
    }

    /**
     * @param mission        Mission declaring the map items
     * @param cot            The CoT that contains the map item info
     * @param manualDownload {@code true} indicates the user has requested the download and the map items should be
     *                       downloaded regardless of the age limit preference
     * @return Map items download future or {@code null} if the request was ignored due to age limits
     */
    @Nullable
    public CompletableFuture<IDataSyncManager.MissionMapItemDownload> downloadToLocalCot(@Nonnull Mission mission,
                                                                                         @Nonnull MissionAdd<String> cot,
                                                                                         boolean manualDownload)
    {
        // Skip if we're already downloading this map item
        String uid = cot.data();
        CompletableFuture<IDataSyncManager.MissionMapItemDownload> existingFuture = pendingMapItemDownloads.get(uid);
        if (existingFuture != null) return existingFuture;

        // Create file path & parent directory
        final UidDetails details = cot.details();
        final File cotFile = missionResources.cotFile(cot);
        if (!manualDownload &&
                isAgeOverThreshold("Map Item " + (details != null ? details.callsign() : uid), cot.timestamp()))
        {
            // Return the non-existent file and notify all listeners; no future is required. The user will need to manually
            // request the download.
            ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionDownloadListeners(), listener -> listener.onDownloadCot(connection, mission, cot, cotFile));
            return null;
        }
        File parentDir = cotFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) logger.warn("Failed to create directory: {}", parentDir);

        // Download to path
        if (logger.isDebugEnabled()) logger.debug("Downloading map-item '{}' from mission name='{}' on server: {}", cot.data(), mission.name(), connection.getUri());
        CompletableFuture<IDataSyncManager.MissionMapItemDownload> downloadFuture = new CompletableFuture<>();
        pendingMapItemDownloads.put(uid, downloadFuture);
        cotEndpoints.getCotItem(uid).asyncGetText(executorService).whenComplete((cotMessageText, error) -> {
            pendingMapItemDownloads.remove(uid);
            if (error != null)
            {
                logger.error("Failed downloading map-item '{}' from mission name='{}' on server: {}",
                        cot.data(), mission.name(), connection.getUri(), error);
                downloadFuture.completeExceptionally(error);
            }
        }).thenAccept(cotMessageText -> {
            // Cache the results locally
            cacheLocalCot(uid, cotMessageText);

            // While the import is in-progress record the uid and disallow any republish attempts for it.
            ongoingMapItemImports.add(uid);
            cotImportHelper.parseAndImportCot(cotMessageText).thenRun(() -> ongoingMapItemImports.remove(uid));

            // Notify listeners that we downloaded a new cot file
            ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionDownloadListeners(), listener -> listener.onDownloadCot(connection, mission, cot, cotFile));

            downloadFuture.complete(new IDataSyncManager.MissionMapItemDownload(cot, cotFile));
        });
        return downloadFuture;
    }

    /**
     * Returns a boolean indicating if a resource should be automatically downloaded. To qualify,
     * {@link PreferenceEntry#DATA_SYNC_AUTO_DOWNLOAD_UPDATES } must return {@code true}
     * or the file must be smaller than the value specified in {@link PreferenceEntry#DATA_SYNC_LARGE_FILE_PROMPT}.
     *
     * @param resource The resource to check
     * @return {@code true} if the file should be downloaded automatically, {@code false} otherwise.
     */
    private boolean autoDownloadContent(@Nonnull Resource resource)
    {
        // If this preference is set to true, the item should be downloaded regardless of its size
        if (DATA_SYNC_AUTO_DOWNLOAD_UPDATES.getBooleanValue()) return true;

        final double resourceSizeInMb = (double) resource.size() / ONE_MB;
        final int maxSizeInMb = DATA_SYNC_LARGE_FILE_PROMPT.getIntegerValue();

        if (resourceSizeInMb > maxSizeInMb)
        {
            if (logger.isDebugEnabled()) logger.debug("Delaying download of resource {} during auto-import because its size ({}MB) is larger than the " +
                    "configured max allowable size of {}MB.", resource.name(), DECIMAL_FORMAT.format(resourceSizeInMb), maxSizeInMb);
            return false;
        }

        return true;
    }

    /**
     * @param itemName    The name of the item for use in logging messages
     * @param publishTime The time the item was published
     * @return {@code true} if the published time is over the threshold, {@code false} otherwise.
     * @see TimeThresholdOption
     * @see PreferenceEntry#DATA_SYNC_THRESHOLD
     */
    private boolean isAgeOverThreshold(@Nonnull String itemName, @Nonnull Instant publishTime)
    {
        final TimeThresholdOption threshold = DATA_SYNC_THRESHOLD.getValueAs(TimeThresholdOption.class);

        final Instant now = Instant.now();

        final boolean overThreshold = threshold == TimeThresholdOption.today
                // Data must be from today
                ? !publishTime.atOffset(ZoneOffset.UTC).toLocalDate()
                        .equals(now.atOffset(ZoneOffset.UTC).toLocalDate())
                // Data must not be older than the number of days specified by the threshold
                : ChronoUnit.DAYS.between(publishTime, now) > threshold.getTimeInDays();

        if (overThreshold) if (logger.isDebugEnabled()) logger.debug("Skipping {} because it was above the time threshold of {}", itemName, threshold.getDisplayText());

        return overThreshold;
    }

    /**
     * @param sha256 Sha256 hash of file resource.
     * @return Future of downloaded path.
     */
    @Nullable
    public CompletableFuture<File> getFutureFetchedResourcePath(@Nonnull String sha256)
    {
        CompletableFuture<IDataSyncManager.MissionResourceDownload> future = pendingResourceDownloads.get(sha256);
        return future == null ? null : future.thenApply(IDataSyncManager.MissionResourceDownload::localFilePath);
    }

    /**
     * @param uid Map item UID, {@link gov.tak.api.maps.MapItem#getUid()}.
     * @return Future of downloaded CoT path.
     */
    @Nullable
    public CompletableFuture<File> getFutureFetchedItemPath(@Nonnull String uid)
    {
        CompletableFuture<IDataSyncManager.MissionMapItemDownload> future = pendingMapItemDownloads.get(uid);
        return future == null ? null : future.thenApply(IDataSyncManager.MissionMapItemDownload::localFilePath);
    }

    /**
     * @param missionName Mission name.
     * @return {@code true} when the model download for the given mission is in-progress.
     */
    public boolean isFetchingMissionModelByName(@Nonnull String missionName)
    {
        return pendingMissionModelDownloads.contains(missionName);
    }

    /**
     * @param missionGuid Mission guid.
     * @return {@code true} when the model download for the given mission is in-progress.
     */
    public boolean isFetchingMissionModelByGuid(@Nonnull String missionGuid)
    {
        return pendingMissionModelDownloads.contains(missionGuid);
    }

    /**
     * @param sha256 Sha256 hash of file resource.
     * @return {@code true} when the file download for the given resource is in-progress.
     */
    public boolean isFetchingMissionResource(@Nonnull String sha256)
    {
        return pendingResourceDownloads.containsKey(sha256);
    }

    /**
     * @param uid Map item UID, {@link gov.tak.api.maps.MapItem#getUid()}.
     * @return {@code true} when the map item CoT download is in-progress.
     */
    public boolean isFetchingMissionItem(@Nonnull String uid)
    {
        return pendingMapItemDownloads.containsKey(uid);
    }

    /**
     * @param uid Map item UID, {@link gov.tak.api.maps.MapItem#getUid()}.
     * @return {@code true} when the map item CoT is being imported.
     */
    public boolean isImportingMissionItem(@Nonnull String uid)
    {
        return ongoingMapItemImports.contains(uid);
    }

    /**
     * @param missionName Mission name.
     * @return {@code true} when there are any pending file or map item uploads to the given mission.
     */
    public boolean isUploadingContentToMissionByName(@Nonnull String missionName)
    {
        String guid = missionManager.getGuidFromName(missionName);
        if (guid == null) return false;
        return Stream.concat(pendingFileUploads.values().stream(), pendingMapItemUploads.values().stream())
                .anyMatch(upload -> upload.missionGuid().equals(missionName));
    }

    @Nonnull
    @Override
    public ITakServerConnection getConnection()
    {
        return connection;
    }

    @Override
    public void setConnection(@Nonnull ITakServerConnection connection)
    {
        this.connection = connection;
    }

    /**
     * Common outline for pending uploads.
     * @see PendingFileUpload
     * @see PendingMapItemUpload
     */
    private interface PendingUpload
    {
        @Nonnull
        String missionGuid();
    }

    /**
     * Wrapper to hold information about a pending map item upload to a mission.
     *
     * @param missionGuid Guid of the mission the map item will be added to.
     * @param future      Future of the addition.
     */
    private record PendingMapItemUpload(@Nonnull String missionGuid, @Nonnull CompletableFuture<IDataSyncManager.MissionAddMapItem> future) implements PendingUpload
    {
    }

    /**
     * Wrapper to hold information about a pending file upload to a mission.
     *
     * @param missionGuid Guid of the mission the file will be added to.
     * @param future      Future of the addition.
     */
    private record PendingFileUpload(@Nonnull String missionGuid, @Nonnull CompletableFuture<IDataSyncManager.MissionAddFile> future) implements PendingUpload
    {
    }
}
