package gov.tak.core.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.TimeThresholdOption;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionUtils;
import gov.tak.core.comms.cot.CotExporterManager;
import gov.tak.core.comms.takserver.DataPackageManager;
import gov.tak.core.comms.takserver.DataSyncManager;
import gov.tak.platform.cot.importer.CotImportHelper;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.core.view.gui.takserver.DataFeedDeleteDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static gov.takx.platform.preferences.PreferenceEntry.DATA_SYNC_AUTO_DELETE_THRESHOLD;

/**
 * Top level data-sync component for tracking per-server data.
 *
 * @since 5.7
 */
@Dependent
public class MissionSync implements IMissionSyncComponent
{
    /** Static set keeping track of unique {@link DataSyncManager#key(ITakServerConnection)} values initialized. */
    private static final Set<String> INITIALIZED_KEYS = new HashSet<>();

    private final boolean underTest;
    private final DataSyncManager dataSyncManager;
    private final DataPackageManager dataPackageManager;
    private final TakServerApiClient takServerApiClient;
    private final ICoreExecutorService executorService;
    private final CotImportHelper cotImportHelper;
    private final CotExporterManager cotExporterManager;
    private final INotificationService notificationService;

    private MissionManager missionManager;
    private MissionPersistence missionPersistence;
    private MissionResources missionResources;
    private MissionTransfers missionTransfers;
    private ITakServerConnection connection;

    private boolean initialized;

    @Inject
    MissionSync(DataSyncManager dataSyncManager, TakServerApiClient takServerApiClient,
                DataPackageManager dataPackageManager, ICoreExecutorService executorService,
                CotImportHelper cotImportHelper, CotExporterManager cotExporterManager,
                INotificationService notificationService, @PlatformUnderTest boolean underTest)
    {
        this.dataSyncManager = dataSyncManager;
        this.dataPackageManager = dataPackageManager;
        this.takServerApiClient = takServerApiClient;
        this.executorService = executorService;
        this.cotImportHelper = cotImportHelper;
        this.cotExporterManager = cotExporterManager;
        this.notificationService = notificationService;
        this.underTest = underTest;
    }

    /**
     * Sets the connection like {@link #setConnection(ITakServerConnection)} but also initializes sub-services.
     *
     * @param connection Connection to assign to this manager.
     * @return Self.
     * @throws IOException Thrown when a connection is already set in the manager.
     */
    @Nonnull
    public MissionSync withConnection(@Nonnull ITakServerConnection connection) throws IOException
    {
        if (this.connection == null)
        {
            setConnection(connection);
            initializeConnection();
            return this;
        } else
        {
            throw new IOException("Already connected to: " + this.connection.getUri());
        }
    }

    @Override
    public void setConnection(@Nonnull ITakServerConnection connection)
    {
        this.connection = connection;

        // Delegate to sub-services
        if (isInitialized())
        {
            missionManager.setConnection(connection);
            missionTransfers.setConnection(connection);
        }
    }

    @Nonnull
    @Override
    public ITakServerConnection getConnection()
    {
        return Objects.requireNonNull(connection, uninitializedMessage("Connection"));
    }

    /**
     * @return Mission model tracking service.
     */
    @Nonnull
    public MissionManager getMissionManager()
    {
        return Objects.requireNonNull(missionManager, uninitializedMessage("Model-Service"));
    }

    /**
     * @return Mission persist service.
     */
    @Nonnull
    public MissionPersistence getMissionPersistence()
    {
        return Objects.requireNonNull(missionPersistence, uninitializedMessage("Persist-Service"));
    }

    /**
     * @return Mission local resource service.
     */
    @Nonnull
    public MissionResources getMissionResources()
    {
        return Objects.requireNonNull(missionResources, uninitializedMessage("Resource-Service"));
    }

    /**
     * @return Mission IO transfer service.
     */
    @Nonnull
    public MissionTransfers getMissionTransfers()
    {
        return Objects.requireNonNull(missionTransfers, uninitializedMessage("Transfer-Service"));
    }

    /**
     * Persist data-sync state for this manager.
     */
    public void persist()
    {
        getMissionPersistence().persist();
    }

    /**
     * @return {@code true} if this manager has been initialized with a connection.
     * {@code false} if no connection has been assigned, or one was but has since been shut down.
     * @see #setConnection(ITakServerConnection)
     */
    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     * Initialize subservices once the TAK server connection is specified.
     */
    private void initializeConnection()
    {
        // Initialize sub-services.
        missionManager = new MissionManager(dataSyncManager, connection);
        missionResources = new MissionResources(dataSyncManager, connection);
        missionPersistence = new MissionPersistence(missionManager, missionResources, dataSyncManager);
        missionTransfers = new MissionTransfers(executorService, notificationService, cotImportHelper,
                dataSyncManager, dataPackageManager, takServerApiClient, cotExporterManager,
                missionResources, missionManager, connection);

        // Restore previously seen & subscribed mission models.
        //
        // Do not restore when we're retargeting a connection.
        //
        // This happens when a user deletes a connection and re-adds the same one again.
        // Since this happens while TAKX is running, restoring would overwrite current data with whatever
        // is on-disk from when we last persisted (which can be outdated).
        String key = DataSyncManager.key(connection);
        if (INITIALIZED_KEYS.add(key) && !underTest)
        {
            missionPersistence.restore();

            // Queue up remote synchronization for associated items and resources.
            for (Mission value : missionManager.getDownloadedMissionMap().values())
            {
                String guid = value.guid();
                String token = missionManager.getMissionSubscriptionTokenByGuid(guid);
                if (token != null)
                {
                    String password = missionManager.getMissionPasswordByGuid(guid);
                    missionTransfers.createMissionSubscriptionByGuid(guid, password);
                    missionTransfers.downloadMissionByGuid(guid, password, null);
                }
            }
        }

        // Delete inactive missions that are older than a configured time.
        deleteOldMissions();

        // Queue up fetching all the missions on the server.
        missionTransfers.updateBaseMissionModels();

        // Mark initialized so we don't call this again later.
        initialized = true;
    }

    /**
     * Prompts the user to delete synced missions that haven't been updated for longer than the period specified in
     * {@link PreferenceEntry#DATA_SYNC_AUTO_DELETE_THRESHOLD}
     */
    private void deleteOldMissions()
    {
        final TimeThresholdOption threshold = DATA_SYNC_AUTO_DELETE_THRESHOLD.getValueAs(TimeThresholdOption.class);

        if (threshold != TimeThresholdOption.none)
        {
            final Instant instantThreshold = Instant.now().minus(threshold.getTimeInDays(), ChronoUnit.DAYS);
            final List<Mission> oldMissions = missionManager.getMissions().stream()
                    .filter(mission -> MissionUtils.getLastUpdateTime(mission).isBefore(instantThreshold))
                    .toList();

            for (Mission mission : oldMissions)
            {
                // We may need to prevent multiple dialogs from being displayed at once
                new DataFeedDeleteDialog(null, dataSyncManager, connection, mission, null, null).setVisible(true);
            }
        }
    }

    @Nonnull
    private static String uninitializedMessage(@Nonnull String arg)
    {
        return "MissionSync not initialized, cannot offer: " + arg;
    }
}
