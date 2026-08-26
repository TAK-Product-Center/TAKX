package gov.tak.core.comms.takserver;

import com.atakmap.filesystem.HashingUtils;
import feign.Request;
import feign.Response;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.takserver.IDataSyncMissionDownloadListener;
import gov.tak.api.comms.takserver.IDataSyncMissionListener;
import gov.tak.api.comms.takserver.IDataSyncSubscribedMissionListener;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.MissionDownloadStatus;
import gov.tak.api.comms.takserver.mission.ApiProvider;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.Location;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionBuilder;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UidDetails;
import gov.tak.api.comms.takserver.mission.data.UploadedResourceResponse;
import gov.tak.api.comms.takserver.mission.feign.FeignCotApi;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.cot.CotExporterManager;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.tak.core.comms.takserver.mission.MissionSync;
import gov.tak.platform.cot.importer.MissionImporter;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DataSyncManager} functionality.
 *
 * @implNote If any weird threading issue occurs validate all data-sync internals use {@link gov.takx.api.platform.ICoreExecutorService} to that during tests they are executed synchronously.
 * @since 5.2
 */
@SuppressWarnings("FutureReturnValueIgnored")
public class DataSyncManagerTest extends AIntegrationTest
{
    private static final String missionPassword = "the-password";
    private static final String missionSubToken = "the-token";

    private final UUID clientUid;
    private final String clientUidStr;
    private final MapObjectManager mapObjectManager;
    private final IMapObjectFinder mapObjectFinder;
    private final DataSyncManager dataSync;
    private final TakServerApiClient apiClient;
    private final CotImporterManager cotImporterManager;
    private final CotExporterManager cotExporterManager;
    private final IVirtualFileSystemManager projectVfsManager;
    private TakServerConnection connection1;
    private TakServerConnection connection2;
    private boolean connectionsInitialized;

    // Inject the REST API provider so that we can yield our mocked APIs
    @Mocked
    private ApiProvider apiProvider;

    // Inject to facilitate usage of our dummy connections
    @Mocked
    private ITakServerConnectionManager connectionManager;

    // Mocked API's for data-sync endpoints
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private FeignMissionApi missionApi1;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private FeignMissionApi missionApi2;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private FeignCotApi cotApi1;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private FeignCotApi cotApi2;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private FeignSyncApi syncApi1;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private FeignSyncApi syncApi2;

    // Listeners
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private IDataSyncMissionListener missionListener;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private IDataSyncSubscribedMissionListener subscribedMissionListener;
    @Mocked(forBeanUpdating = false, allowCaching = false)
    private IDataSyncMissionDownloadListener downloadListener;

    public DataSyncManagerTest()
    {
        dataSync = getBean(DataSyncManager.class);
        apiClient = getBean(TakServerApiClient.class);
        mapObjectManager = getBean(MapObjectManager.class);
        mapObjectFinder = getBean(IMapObjectFinder.class);
        cotImporterManager = getBean(CotImporterManager.class);
        cotExporterManager = getBean(CotExporterManager.class);
        projectVfsManager = getBean(IVirtualFileSystemManager.class, VFSManagerScope.Literal.PROJECT_INSTANCE);
        clientUid = apiClient.getClientUid();
        clientUidStr = clientUid.toString();

        Object dataSyncInstance = CdiTestUtils.getInstance(dataSync);
        Object apiClientInstance = CdiTestUtils.getInstance(apiClient);
        BlacksmithExtension.setBeansToUpdate(dataSyncInstance, apiClientInstance);
    }

    private void setupConnections()
    {
        connectionsInitialized = true;

        StreamingConnectionConfiguration config1 = new StreamingConnectionConfiguration(UUID.randomUUID());
        config1.setDisplayName("Connection 1");
        config1.setUsername("User");
        config1.setUri("ssl", "ts1.example.com", 8089);
        config1.setEnabled(true);

        StreamingConnectionConfiguration config2 = new StreamingConnectionConfiguration(UUID.randomUUID());
        config2.setDisplayName("Connection 2");
        config1.setUsername("User");
        config2.setUri("ssl", "ts2.example.com", 8089);
        config2.setEnabled(true);

        connection1 = new TakServerConnection(UUID.randomUUID(), null, config1);
        connection2 = new TakServerConnection(UUID.randomUUID(), null, config2);
        connection1.setConnected(true);
        connection2.setConnected(true);

        when(apiProvider.getMissionApi(eq(connection1))).thenReturn(missionApi1);
        when(apiProvider.getMissionApi(eq(connection2))).thenReturn(missionApi2);

        when(apiProvider.getCotApi(eq(connection1))).thenReturn(cotApi1);
        when(apiProvider.getCotApi(eq(connection2))).thenReturn(cotApi2);

        when(apiProvider.getSyncApi(eq(connection1))).thenReturn(syncApi1);
        when(apiProvider.getSyncApi(eq(connection2))).thenReturn(syncApi2);

        when(connectionManager.findConnectionByUuid(same(connection1.getId()))).thenReturn(connection1);
        when(connectionManager.findConnectionByUuid(same(connection2.getId()))).thenReturn(connection2);

        // The 'thenReturn' doesn't like the generic type here...
        Collection<? extends ITakServerConnection> connections = List.of(connection1, connection2);
        when(connectionManager.getTakServerConnections(anyBoolean())).thenAnswer((Answer<Collection<? extends ITakServerConnection>>) invocation -> connections);
        when(connectionManager.getTakServerConnections()).thenCallRealMethod();
    }

    @BeforeEach
    public void resetMocks()
    {
        // The connection setup step has to occur AFTER blacksmith has done its job.
        // This means it has to happen AFTER the 'BeforeAll' and the constructor,
        // and that means the best place to hook it to occur before any test runs
        // is right here. We only want to do it once though, hence the boolean check.
        if (!connectionsInitialized)
        {
            setupConnections();

            // We also want to register our mocked listeners here since blacksmith is done now.
            dataSync.addDataSyncMissionListener(missionListener);
            dataSync.addDataSyncSubscribedMissionListener(subscribedMissionListener);
            dataSync.addDataSyncMissionDownloadListener(downloadListener);
        }

        // Clear mocks on the API interfaces between each test
        reset(missionApi1, missionApi2, cotApi1, cotApi2, syncApi1, syncApi2, missionListener, subscribedMissionListener, downloadListener);
        clearInvocations(missionApi1, missionApi2, cotApi1, cotApi2, syncApi1, syncApi2, missionListener, subscribedMissionListener, downloadListener);

        // Clear the internal data-sync state
        dataSync.clearSyncManagers();
    }

    @AfterEach
    public void cleanup()
    {
        FileUtils.deleteDirectory(dataSync.getRootDir());
        FileUtils.deleteDirectory(projectVfsManager.getVirtualFileSystemForAttachments().getRootDir());
    }

    /**
     * Validate the control flow of data-sync fetching available missions from a server.
     * This occurs <i>once</i> on the initial connection of a {@link ITakServerConnection}.
     * Any following updates are handled by server-sent CoT events.
     */
    @Test
    void testBasicMissionFetch()
    {
        // Configure API to yield just these two missions for connection 1
        String missionName1 = "mission1";
        String missionName2 = "mission2";
        String missionName3 = "mission3";
        Mission mission1 = new MissionBuilder(missionName1)
                .withGuid("m1")
                .withDescription("desc1")
                .withCreatorUid(clientUidStr)
                .build();
        Mission mission2 = new MissionBuilder(missionName2)
                .withGuid("m2")
                .withDescription("desc2")
                .withCreatorUid(clientUidStr)
                .build();
        Mission mission3 = new MissionBuilder(missionName3)
                .withGuid("m3")
                .withDescription("desc3")
                .withCreatorUid("somebody-else")
                .withUids(List.of(new MissionAdd.TString("marker1", Instant.now(), clientUidStr, null, null, null, null)))
                .build();
        when(missionApi1.getAllMissions(any(), any())).thenReturn(List.of(mission1, mission2, mission3));

        // Validate before we do anything the missions are not tracked.
        assertThat(dataSync.isTracked(connection1, missionName1)).isFalse();
        assertThat(dataSync.isTracked(connection1, missionName2)).isFalse();
        assertThat(dataSync.isTracked(connection1, missionName3)).isFalse();

        // Connect to the first server.
        connectTo(connection1);

        // Validate the missions are tracked now.
        assertThat(dataSync.isTracked(connection1, missionName1)).isTrue();
        assertThat(dataSync.isTracked(connection1, missionName2)).isTrue();
        assertThat(dataSync.isTracked(connection1, missionName3)).isTrue();
        assertThat(dataSync.isTracked(connection1, "unknown")).isFalse();
        assertThat(dataSync.isTracked(connection2, missionName1)).isFalse();

        // Validate that the missions are the equal.
        Mission fetched1 = dataSync.getMission(connection1, missionName1);
        Mission fetched2 = dataSync.getMission(connection1, missionName2);
        Mission fetched3 = dataSync.getMission(connection1, missionName3);
        assertThat(mission1).isEqualTo(fetched1);
        assertThat(mission2).isEqualTo(fetched2);
        assertThat(mission3).isEqualTo(fetched3);

        // Validate the missions map has the connection.
        var missionsPerConnection = dataSync.currentMissions();
        assertThat(missionsPerConnection).containsOnlyKeys(connection1);

        // Validate the missions map contains the two missions from the endpoint.
        Collection<Mission> missions = missionsPerConnection.get(connection1);
        assertThat(missions).containsOnly(mission1, mission2, mission3);

        // Validate that ownership based on our mocked models.
        assertThat(dataSync.isMissionOwner(connection1, missionName1)).isTrue();
        assertThat(dataSync.isMissionOwner(connection1, missionName2)).isTrue();
        assertThat(dataSync.isMissionOwner(connection1, missionName3)).isFalse(); // Had other owner
        assertThat(dataSync.isMissionOwner(connection1, "unknown")).isFalse(); // Non-existent mission

        // Validate any tracking that assumes downloading/subscribing is a default value (false, not downloaded, etc.)
        assertThat(dataSync.isDownloaded(connection1, missionName1)).isFalse();
        assertThat(dataSync.isDownloaded(connection1, missionName2)).isFalse();
        assertThat(dataSync.isDownloaded(connection1, missionName3)).isFalse();
        assertThat(dataSync.getDownloadState(connection1, missionName1)).isEqualTo(MissionDownloadStatus.NOT_DOWNLOADED);
        assertThat(dataSync.getDownloadState(connection1, missionName2)).isEqualTo(MissionDownloadStatus.NOT_DOWNLOADED);
        assertThat(dataSync.getDownloadState(connection1, missionName3)).isEqualTo(MissionDownloadStatus.NOT_DOWNLOADED);
        assertThat(dataSync.isSubscribed(connection1, missionName1)).isFalse();
        assertThat(dataSync.isSubscribed(connection1, missionName2)).isFalse();
        assertThat(dataSync.isSubscribed(connection1, missionName3)).isFalse();
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName1)).isNull();
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName2)).isNull();
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName3)).isNull();
    }

    /**
     * Validate the control flow of a mission being updated via data-sync events from TAK Server.
     */
    @Test
    void testMissionFetchUpdatesMission()
    {
        // Configure API to yield one mission
        String missionName = "mission1";
        String missionGuid = "m1";
        Mission mission1pre = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withDescription("desc1")
                .withCreatorUid(clientUidStr)
                .build();
        when(missionApi1.getAllMissions(any(), any())).thenReturn(List.of(mission1pre));

        // Trigger fetching the missions upon initial connection.
        connectTo(connection1);

        // Listener should have been notified of the new mission from the initial connection.
        verify(missionListener, times(1)).onNewMission(same(connection1), eq(mission1pre));

        // Validate the mission was fetched.
        Mission fetched1pre = dataSync.getMission(connection1, missionName);
        assertThat(mission1pre).isEqualTo(fetched1pre);

        // Change what the API yields for the same request (simulating a server updating the mission)
        Mission mission1post = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withDescription("desc1")
                .withCreatorUid(clientUidStr)
                .withKeywords(List.of("foo, bar"))
                .build();
        when(missionApi1.getAllMissions(any(), any())).thenReturn(List.of(mission1post));
        when(missionApi1.getMission(any(), any(), eq(missionName))).thenReturn(List.of(mission1post));
        when(missionApi1.getMissionByGuid(any(), any(), eq(missionGuid))).thenReturn(List.of(mission1post));

        // Trigger the sync manager to re-fetch the mission by notifying it of an update.
        Instant now = Instant.now();
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c-k' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'/>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid));

        // The listener should have been notified of the update.
        verify(missionListener, times(1)).onUpdateMission(same(connection1), eq(mission1pre), eq(mission1post));

        // Validate that data-sync is aware of the new mission model.
        Mission fetched1post = dataSync.getMission(connection1, missionName);
        assertThat(fetched1post).isEqualTo(mission1post);
        assertThat(fetched1pre).isNotEqualTo(fetched1post);

        // Remove the mission.
        when(missionApi1.getAllMissions(any(), any())).thenReturn(List.of());
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-d' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='DELETE' tool='public'/>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid));

        // Validate that data-sync has removed the mission.
        assertThat(dataSync.getMission(connection1, missionName)).isNull();
        assertThat(dataSync.currentMissions().get(connection1)).isEmpty();

        // The listener should have been notified of the removal.
        verify(missionListener, times(1)).onRemoveMission(same(connection1), eq(mission1post));

        // Recreate the mission.
        setupSubscribeScenario(mission1post);
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-n' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CREATE' tool='public'/>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid));

        // Validate that data-sync is aware of the mission again.
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission1post);
        assertThat(dataSync.currentMissions().get(connection1)).containsExactly(mission1post);

        // The listener should have been notified of the update.
        verify(missionListener, times(1)).onNewMission(same(connection1), eq(mission1post));
    }

    /**
     * Validate the control flow of subscribing to a mission and the different
     * states before and after subscription is complete.
     */
    @Test
    void testMissionSubscription()
    {
        // Configure API to yield one mission.
        // - Low:  Basic model
        // - High: Simulated extended model accessed by getting the mission by name
        String missionName = "mission1";
        String missionGuid = "m1";
        MissionBuilder builder = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withPasswordProtected(true)
                .withDescription("desc1")
                .withCreatorUid(clientUidStr);
        Mission mission1low = builder.build();
        Mission mission1high = builder
                .withKeywords(List.of("one", "two", "three"))
                .build();
        ArgumentMatcher<Map<String, Object>> containsPasswordEntry = map -> missionPassword.equals(map.get("password"));
        when(missionApi1.getAllMissions(any(), any())).thenReturn(List.of(mission1low));
        when(missionApi1.getMission(any(), argThat(containsPasswordEntry), eq(missionName)))
                .thenReturn(List.of(mission1high));
        MissionSubscription missionSubscription = new MissionSubscription("uid", missionSubToken, clientUidStr, "username", Instant.now(), null);
        when(missionApi1.createMissionSubscription(any(), argThat(containsPasswordEntry), eq(missionName)))
                .thenReturn(missionSubscription);

        // Trigger fetching the missions upon initial connection.
        connectTo(connection1);

        // Validate the mission was fetched.
        Mission fetched1pre = dataSync.getMission(connection1, missionName);
        assertThat(mission1low).isEqualTo(fetched1pre);

        // Validate that before subscribing we do not know the missions subscription token.
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName)).isNull();

        // Validate that we have not downloaded/subscribed to the mission initially.
        assertThat(dataSync.isDownloaded(connection1, missionName)).isFalse();
        assertThat(dataSync.isSubscribed(connection1, missionName)).isFalse();
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.NOT_DOWNLOADED);

        // Subscribe to the mission
        var downloadFuture = dataSync.subscribe(connection1, missionName, missionPassword);
        var download = downloadFuture.join();
        assertThat(download.mission()).isEqualTo(mission1high);

        // Validate that we have now downloaded/subscribed to the mission.
        // - Subscription implies downloading since that is a pre-requisite step.
        assertThat(dataSync.isDownloaded(connection1, missionName)).isTrue();
        assertThat(dataSync.isSubscribed(connection1, missionName)).isTrue();
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);

        // Validate that we can use data-sync to request the sub-token now that we are subscribed.
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName)).isEqualTo(missionSubToken);

        // Validate that after subscribing the mission getter yields the enhanced model instead of the lower detail model.
        Mission fetched1post = dataSync.getMission(connection1, missionName);
        assertThat(mission1high).isEqualTo(fetched1post);

        // Now unsubscribe.
        dataSync.unsubscribe(connection1, missionName);

        // Validate that the state now mirrors how it was before we subscribed to the mission.
        assertThat(mission1low).isEqualTo(fetched1pre);
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName)).isNull();
        assertThat(dataSync.isSubscribed(connection1, missionName)).isFalse();

        // Note: Unsubscribing does not imply deletion.
        // Though if you use the 'delete' method that does unsubscribing.
        // Anyway, this data should still be around until we explicitly request its deletion.
        assertThat(dataSync.isDownloaded(connection1, missionName)).isTrue();

        // Lastly delete local contents (since subscribing downloaded them).
        dataSync.delete(connection1, missionName, true, false);

        // Validate the mission is no longer downloaded.
        assertThat(dataSync.isDownloaded(connection1, missionName)).isFalse();
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.NOT_DOWNLOADED);
    }

    /**
     * Validate adding/updating a local map object + attachments sends the appropriate requests to the server
     * and any 'in-progress' state <i>({@link gov.tak.core.comms.takserver.mission.MissionTransfers#isUploadingContentToMissionByName(String)})</i>
     * is cleaned up afterward.
     */
    @Test
    void testLocalChanges()
    {
        // Configure API to yield one mission
        String missionName = "mission";
        String missionGuid = "m";
        Mission mission = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .build();
        setupSubscribeScenario(mission);

        // Connect + subscribe to mission
        connectTo(connection1);
        dataSync.subscribe(connection1, missionName, null).join();

        // Create a map object, upload and wait for result. Both before and after the download state should be 'complete'.
        // Nothing is pending after we finish the initial subscription, and nothing is pending after
        // the map item is uploaded, and we get an acknowledgement.
        IMapObject mapObject = mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder("test-family", "test-type")
                .location(new GeoPoint(10, 10))
                .build());
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);
        dataSync.addMapItem(connection1, missionName, mapObject).join();
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);

        // Verify the server saw the upload request
        verify(missionApi1, times(1)).addMissionPackage(any(), any(), eq(missionName), any());

        // Update the mock to now hold the uploaded map item
        Instant now = Instant.now();
        mission = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withUids(List.of(new MissionAdd.TString(mapObject.getUid(), now, clientUidStr, null, null, null, detailsOf(mapObject))))
                .build();
        setupSubscribeScenario(mission);
        setupCotResponse(mapObject);

        // The server would send a following event for the mission change, and then the CoT event of the map object again
        String timeStart = now.toString();
        String timeStale = now.plus(1, ChronoUnit.DAYS).toString();
        CotEvent updatedCot = Objects.requireNonNull(cotExporterManager.export(mapObject));
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentUid>%ITEM_UID%</contentUid>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>ADD_CONTENT</type>
                          <details callsign='%ITEM_NAME%' type='%ITEM_TYPE%'/>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", timeStart)
                .replace("%STALE%", timeStale)
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ITEM_UID%", mapObject.getUid())
                .replace("%ITEM_NAME%", mapObject.getName())
                .replace("%ITEM_TYPE%", mapObject.getCotType()));
        cotImporterManager.importEvent(updatedCot);

        // Assert that the mission model is updated and indicates the map item is attached (done, not in-progress).
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission);
        var manager = dataSync.sync(connection1).map(MissionSync::getMissionManager);
        var transfers = dataSync.sync(connection1).map(MissionSync::getMissionTransfers);
        assertThat(manager).isPresent();
        assertThat(transfers).isPresent();
        assertThat(manager.get().isMapItemUidOwned(mapObject.getUid())).isTrue();
        assertThat(transfers.get().isImportingMissionItem(mapObject.getUid())).isFalse();

        // When a tracked map object property updates another request should be sent to update the mission.
        // For instance, changing the CoT type should trigger an update.
        mapObject.setCotType("a-n-G");

        // The upload endpoint should have been called an additional time
        verify(missionApi1, times(2)).addMissionPackage(any(), any(), eq(missionName), any());

        // At no point should have we requested the CoT content from the server
        // - All changes come from our client making them, so any remote requests indicate necessary networking
        verify(cotApi1, never()).getCotItem(any(), any(), any());

        // And same as before, once the upload is acknowledged we should be marked as completed.
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);

        // Now add an attachment to the map object.
        String attachmentName = "hello.txt";
        String attachmentContent = "hi";
        byte[] attachmentBytes = attachmentContent.getBytes();
        String attachmentSha256 = HashingUtils.sha256sum(attachmentBytes);
        mapObject.addAttachment(attachmentName, new ByteArrayInputStream(attachmentBytes));

        // The upload endpoint will be hit again, and the cot API still not being hit.
        verify(missionApi1, times(3)).addMissionPackage(any(), any(), eq(missionName), any());
        verify(cotApi1, never()).getCotItem(any(), any(), any());

        // Update the mock to now hold the uploaded map item.
        mission = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withUids(List.of(new MissionAdd.TString(mapObject.getUid(), now, clientUidStr, null, null, null, detailsOf(mapObject))))
                .withContents(List.of(new MissionAdd.TResource(new Resource(null, attachmentSha256, attachmentName, attachmentName, now, clientUidStr, clientUidStr, null, "text/plain", attachmentBytes.length, -1), now, clientUidStr, null, null, null, null)))
                .build();
        setupSubscribeScenario(mission);
        setupCotResponse(mapObject);
        setupFileResponse(attachmentSha256, attachmentContent);

        // Model server notification of mission update.
        updatedCot = Objects.requireNonNull(cotExporterManager.export(mapObject));
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentResource>
                            <creatorUid>%CLIENT_UID%</creatorUid>
                            <expiration>-1</expiration>
                            <groupVector>1</groupVector>
                            <hash>%ATT_HASH%</hash>
                            <mimeType>%ATT_TYPE%</mimeType>
                            <name>%ATT_NAME%</name>
                            <size>%ATT_SIZE%</size>
                            <submissionTime>%TIME%</submissionTime>
                            <submitter>%CLIENT_UID%</submitter>
                            <uid>%ATT_UID%</uid>
                          </contentResource>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>ADD_CONTENT</type>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", timeStart)
                .replace("%STALE%", timeStale)
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ATT_UID%", UUID.randomUUID().toString())
                .replace("%ATT_NAME%", attachmentName)
                .replace("%ATT_HASH%", attachmentSha256)
                .replace("%ATT_SIZE%", String.valueOf(attachmentBytes.length))
                .replace("%ATT_TYPE%", "text/plain"));
        cotImporterManager.importEvent(updatedCot);

        // Just like before we should not observe requests to the server for the file contents.
        // - Our client uploaded the file, there is no need for us to request it back unless.
        verify(syncApi1, never()).content(any(), any());

        // This time we're going to upload the 'attachment' directly (mainly for code-coverage sake)
        attachmentName = "alternative.txt";
        attachmentContent = "hello";
        attachmentBytes = attachmentContent.getBytes();
        attachmentSha256 = HashingUtils.sha256sum(attachmentBytes);
        setupFileResponse(attachmentSha256, attachmentContent);
        File alternativeAttachment = new File(dataSync.getRootDir(), attachmentName);
        FileUtils.writeString(alternativeAttachment, attachmentContent);

        // Update the mission model and other endpoints for responding to the file upload request.
        mission = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withUids(List.of(new MissionAdd.TString(mapObject.getUid(), now, clientUidStr, null, null, null, detailsOf(mapObject))))
                .withContents(List.of(mission.contents().get(0), new MissionAdd.TResource(new Resource(null, attachmentSha256, attachmentName, attachmentName, now, clientUidStr, clientUidStr, null, "text/plain", attachmentBytes.length, -1), now, clientUidStr, null, null, null, null)))
                .build();
        setupSubscribeScenario(mission);
        when(syncApi1.upload(any(), any(), anyString(), any())).thenReturn(new UploadedResourceResponse(null, now, "text/plain", clientUidStr, attachmentSha256));

        // Add the file to the mission, simulate server response.
        dataSync.addFile(connection1, missionName, alternativeAttachment);
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentResource>
                            <creatorUid>%CLIENT_UID%</creatorUid>
                            <expiration>-1</expiration>
                            <groupVector>1</groupVector>
                            <hash>%ATT_HASH%</hash>
                            <mimeType>%ATT_TYPE%</mimeType>
                            <name>%ATT_NAME%</name>
                            <size>%ATT_SIZE%</size>
                            <submissionTime>%TIME%</submissionTime>
                            <submitter>%CLIENT_UID%</submitter>
                            <uid>%ATT_UID%</uid>
                          </contentResource>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>ADD_CONTENT</type>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", timeStart)
                .replace("%STALE%", timeStale)
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ATT_UID%", UUID.randomUUID().toString())
                .replace("%ATT_NAME%", attachmentName)
                .replace("%ATT_HASH%", attachmentSha256)
                .replace("%ATT_SIZE%", String.valueOf(attachmentBytes.length))
                .replace("%ATT_TYPE%", "text/plain"));

        // Verify that:
        // - No additional mission package call made, this will instead call 'upload' then 'addMissionContent'
        // - Again, we don't refetch the file from the server since we're the ones adding it.
        verify(missionApi1, times(3)).addMissionPackage(any(), any(), eq(missionName), any());
        verify(syncApi1, times(1)).upload(any(), any(), eq("text/plain"), eq(attachmentBytes));
        verify(missionApi1, times(1)).addMissionContent(any(), any(), eq(missionName), any());
        verify(syncApi1, never()).content(any(), any());

        // We can assert the file is accessible too.
        assertThat(dataSync.getResourceByHash(attachmentSha256).join()).hasDigest("SHA-256", attachmentSha256);
    }

    @Test
    void testRemoteChanges()
    {
        // Configure API to yield one mission
        String missionName = "mission1";
        String missionGuid = "m1";
        Mission mission1pre = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .build();
        setupSubscribeScenario(mission1pre);
        connectTo(connection1);

        // Subscribe to the mission
        var downloadFuture = dataSync.subscribe(connection1, missionName, missionPassword);
        var download = downloadFuture.join();
        assertThat(download.mission()).isEqualTo(mission1pre);
        assertThat(dataSync.isSubscribed(connection1, missionName)).isTrue();

        // Simulate a map item being added to the feed by somebody else.
        // - Create CoT for item
        // - Update endpoints to yield updated mission / CoT
        String mapItemUid = UUID.randomUUID().toString();
        String mapItemName = "MyMapItem";
        String mapItemType = "a-u-G";
        Instant now = Instant.now();
        String mapItemCot = """
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                  <event version='2.0' uid='%ITEM_UID%' type='%ITEM_TYPE%' time='%TIME%' start='%TIME%' stale='%STALE%' how='m-g-g' access='Undefined'>
                    <point lat="10.0" lon="10.0" hae="9999999.0" ce="9999999.0" le="9999999.0"/>
                    <detail>
                      <contact callsign='%ITEM_NAME%'/>
                    <archive/>
                  </detail>
                </event>
                """
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%ITEM_UID%", mapItemUid)
                .replace("%ITEM_NAME%", mapItemName)
                .replace("%ITEM_TYPE%", mapItemType);
        Mission mission1post = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withUids(List.of(new MissionAdd.TString(mapItemUid, now, clientUidStr, null, null, null, detailsOf(mapItemType, mapItemName, new Location(10, 10), null))))
                .build();
        setupSubscribeScenario(mission1post);
        setupCotResponse(mapItemUid, mapItemCot);
        cotImporterManager.importEvent(CotEvent.parse(mapItemCot));

        // Trigger data-sync to update by sending it an event notifying it of the remote update.
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentUid>%ITEM_UID%</contentUid>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>ADD_CONTENT</type>
                          <details callsign='%ITEM_NAME%' type='%ITEM_TYPE%'/>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ITEM_UID%", mapItemUid)
                .replace("%ITEM_NAME%", mapItemName)
                .replace("%ITEM_TYPE%", mapItemType));

        // Assert download is done.
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission1post);

        // Assert we have the map object on the globe.
        assertThat(mapObjectFinder.findMapObjectByUid(mapItemUid)).isNotNull();

        // Verify the updated model is requested.
        //  - We do one update for any feed with changes (subscribed or not)
        //  - If we're subscribed, then we do another with more detail
        verify(missionApi1, times(2)).getMissionByGuid(any(), any(), eq(missionGuid));

        // Verify the CoT for the item was fetched.
        //  - Triggered by sync since we cannot know if any arbitrary map-item CoT is going to be associated with a specific data-sync feed update.
        verify(cotApi1, times(1)).getCotItem(any(), any(), eq(mapItemUid));

        // Now remove the map item
        setupSubscribeScenario(mission1pre);
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentUid>%ITEM_UID%</contentUid>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>REMOVE_CONTENT</type>
                          <details callsign='%ITEM_NAME%' type='%ITEM_TYPE%'/>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ITEM_UID%", mapItemUid)
                .replace("%ITEM_NAME%", mapItemName)
                .replace("%ITEM_TYPE%", mapItemType));

        // Verify the updated model is requested. Another two updates, same reason as before.
        verify(missionApi1, times(4)).getMissionByGuid(any(), any(), eq(missionGuid));

        // Assert mission is back to having no map items.
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission1pre);

        // Assert the map object is removed from the globe.
        assertThat(mapObjectFinder.findMapObjectByUid(mapItemUid)).isNull();

        // Now simulate a resource being added to the mission.
        String attachmentName = "hello.txt";
        String attachmentContent = "hi";
        byte[] attachmentBytes = attachmentContent.getBytes();
        String attachmentSha256 = HashingUtils.sha256sum(attachmentBytes);
        mission1post = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withContents(List.of(new MissionAdd.TResource(new Resource(null, attachmentSha256, attachmentName, attachmentName, now, clientUidStr, clientUidStr, null, "text/plain", attachmentBytes.length, -1), now, clientUidStr, null, null, null, null)))
                .build();
        setupFileResponse(attachmentSha256, attachmentContent);
        setupSubscribeScenario(mission1post);
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentResource>
                            <creatorUid>%CLIENT_UID%</creatorUid>
                            <expiration>-1</expiration>
                            <groupVector>1</groupVector>
                            <hash>%ATT_HASH%</hash>
                            <mimeType>%ATT_TYPE%</mimeType>
                            <name>%ATT_NAME%</name>
                            <size>%ATT_SIZE%</size>
                            <submissionTime>%TIME%</submissionTime>
                            <submitter>%CLIENT_UID%</submitter>
                            <uid>%ATT_UID%</uid>
                          </contentResource>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>ADD_CONTENT</type>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ATT_UID%", UUID.randomUUID().toString())
                .replace("%ATT_NAME%", attachmentName)
                .replace("%ATT_HASH%", attachmentSha256)
                .replace("%ATT_SIZE%", String.valueOf(attachmentBytes.length))
                .replace("%ATT_TYPE%", "text/plain"));

        // Verify the updated model is requested. Another two updates, same reason as before.
        verify(missionApi1, times(6)).getMissionByGuid(any(), any(), eq(missionGuid));

        // Verify the attachment was requested.
        verify(syncApi1, times(1)).content(any(), any());

        // Assert mission has the attachment.
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission1post);
        assertThat(dataSync.getResourceByHash(attachmentSha256)).isCompleted();

        // Now remove the attachment
        setupSubscribeScenario(mission1pre);
        sendEvent("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='change' type='t-x-m-c' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%' type='CHANGE' tool='public'>
                      <MissionChanges>
                        <MissionChange>
                          <contentResource>
                            <creatorUid>%CLIENT_UID%</creatorUid>
                            <expiration>-1</expiration>
                            <groupVector>1</groupVector>
                            <hash>%ATT_HASH%</hash>
                            <mimeType>%ATT_TYPE%</mimeType>
                            <name>%ATT_NAME%</name>
                            <size>%ATT_SIZE%</size>
                            <submissionTime>%TIME%</submissionTime>
                            <submitter>%CLIENT_UID%</submitter>
                            <uid>%ATT_UID%</uid>
                          </contentResource>
                          <creatorUid>%CLIENT_UID%</creatorUid>
                          <isFederatedChange>false</isFederatedChange>
                          <missionGuid>%MISSION_GUID%</missionGuid>
                          <missionName>%MISSION_NAME%</missionName>
                          <timestamp>%TIME%</timestamp>
                          <type>REMOVE_CONTENT</type>
                        </MissionChange>
                      </MissionChanges>
                    </mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%ATT_UID%", UUID.randomUUID().toString())
                .replace("%ATT_NAME%", attachmentName)
                .replace("%ATT_HASH%", attachmentSha256)
                .replace("%ATT_SIZE%", String.valueOf(attachmentBytes.length))
                .replace("%ATT_TYPE%", "text/plain"));

        // Verify the updated model is requested. Another two updates, same reason as before.
        verify(missionApi1, times(8)).getMissionByGuid(any(), any(), eq(missionGuid));

        // Assert mission no longer has the attachment.
        assertThat(dataSync.getDownloadState(connection1, missionName)).isEqualTo(MissionDownloadStatus.COMPLETED);
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission1pre);

        // Now let's delete that mission.
        // Since this is last in the test it is ok to not update the other endpoints.
        // We just want to show the right endpoint gets called.
        dataSync.delete(connection1, missionName, true, true);
        verify(missionApi1, times(1)).removeMission(any(), any(), eq(missionName));
    }

    @Test
    void testCreateMission()
    {
        List<String> keywords = List.of("foo", "bar");
        String missionName = "mission1";
        String missionGuid = "m1";
        Mission mission = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withKeywords(keywords)
                .withPasswordProtected(true)
                .build();
        Mission missionWithoutKeywords = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .withPasswordProtected(true)
                .build();

        // Setup server responses.
        //  - No existing missions
        //  - Creation doesn't include keywords
        //  - Setting keywords yields the up-to-date model
        when(missionApi1.getAllMissions(any(), any())).thenReturn(Collections.emptyList());
        when(missionApi1.createMission(any(), any(), eq(missionName))).thenReturn(List.of(missionWithoutKeywords));
        when(missionApi1.setMissionKeywords(any(), any(), eq(missionName), eq(keywords))).thenReturn(List.of(mission));
        setupSubscribeEndpoint(missionName, missionGuid);

        // Connect to the server.
        connectTo(connection1);

        // Create the mission.
        dataSync.createMission(connection1, mission, missionPassword);

        // We should see:
        // - 1 call to create the mission
        // - 1 call to assign the keywords (because you cant do that with the first call for... reasons)
        // - 1 call to subscribe to the created mission
        verify(missionApi1, times(1)).createMission(any(), any(), eq(missionName));
        verify(missionApi1, times(1)).setMissionKeywords(any(), any(), eq(missionName), eq(keywords));
        verify(missionApi1, times(1)).createMissionSubscription(any(), any(), eq(missionName));

        // For our local listener, we should see:
        // - 1 call to create the mission (model from the 'createMission' endpoint without keywords)
        // - 1 call to update the mission (model from the 'setMissionKeywords' endpoint with keywords)
        // This applies to both the subscribed and non-subscribed listeners.
        verify(missionListener, times(1)).onNewMission(eq(connection1), eq(missionWithoutKeywords));
        verify(missionListener, times(1)).onUpdateMission(eq(connection1), eq(missionWithoutKeywords), eq(mission));
        verify(subscribedMissionListener, times(1)).onSubscribe(eq(connection1), eq(missionWithoutKeywords));
        verify(subscribedMissionListener, times(1)).onSubscribedMissionUpdate(eq(connection1), eq(missionWithoutKeywords), eq(mission));

        // Verify our local state.
        assertThat(dataSync.isTracked(connection1, missionName)).isTrue();
        assertThat(dataSync.isDownloaded(connection1, missionName)).isTrue();
        assertThat(dataSync.isSubscribed(connection1, missionName)).isTrue();
        assertThat(dataSync.getMission(connection1, missionName)).isEqualTo(mission);
    }

    @Test
    void testInvitations()
    {
        // Create the model but don't download or subscribe yet.
        String missionName = "mission1";
        String missionGuid = "m1";
        Mission mission = new MissionBuilder(missionName)
                .withGuid(missionGuid)
                .withCreatorUid(clientUidStr)
                .build();

        // If we don't have the mission locally then creating an invitation should fail.
        assertThat(dataSync.createInviteCot(connection1, missionName)).isNull();

        // Connect to the server and populate the mission list.
        // We should then have the model stored locally and can create an invitation.
        setupSubscribeScenario(mission);
        connectTo(connection1);

        // These client-side created invitations are weak.
        // We are not the server and thus cannot generate subscription tokens.
        // They're usable for public missions that are not password protected though.
        // Other tests (for MissionExporter) cover more specific contracts for these generated invites.
        assertThat(dataSync.createInviteCot(connection1, missionName)).isNotNull();

        // We'll subscribe to the mission now so that when we request the server create an invitation
        // we can show that we also pass along our local copy of our own subscription token,
        dataSync.subscribe(connection1, missionName, null);
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName)).isEqualTo(missionSubToken);

        // Send some other client the invitation via the server.
        // Verify that it has the expected information we can provide.
        String otherClientUid = UUID.randomUUID().toString();
        dataSync.inviteToMission(connection1, missionName, InvitationType.CLIENT_UID, List.of(otherClientUid));
        verify(missionApi1, times(1)).sendMissionInvites(any(), any(), eq(missionName), argThat(list -> list.size() == 1 && otherClientUid.equals(list.get(0).invitee())));

        // Unsubscribe, show that we no longer have the token.
        dataSync.unsubscribe(connection1, missionName);
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName)).isNull();

        // Now we want to simulate the server sending down an invitation to the mission.
        //  - We don't go through the import manager because it initializes data before Blacksmith creates the model
        //    for what mocks are going to be injected into beans. So we manually do that here instead.
        Instant now = Instant.now();
        MissionImporter importer = new MissionImporter();
        BlacksmithExtension.interceptRuntimeManagedBean(importer);
        importer.importData(CotEvent.parse("""
                <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
                <event version='2.0' uid='invite' type='t-x-m-i' time='%TIME%' start='%TIME%' stale='%STALE%' how='h-g-i-g-o' access='Undefined'>
                  <point lat="0.0" lon="0.0" hae="0.0" ce="9999999.0" le="9999999.0"/>
                  <detail>
                    <mission authorUid='%CLIENT_UID%' name='%MISSION_NAME%' guid='%MISSION_GUID%'
                             token='%MISSION_TOKEN%' server='%SERVER%' type='INVITE' tool='public'></mission>
                  </detail>
                </event>
                """
                .replace("%CLIENT_UID%", clientUidStr)
                .replace("%TIME%", now.toString())
                .replace("%STALE%", now.plus(1, ChronoUnit.DAYS).toString())
                .replace("%MISSION_NAME%", missionName)
                .replace("%MISSION_GUID%", missionGuid)
                .replace("%MISSION_TOKEN%", missionSubToken)
                .replace("%SERVER%", connection1.getConnectionConfig().getNetConnectString().toString())));

        // We should be subscribed again with the token if we processed the invite correctly.
        assertThat(dataSync.getMissionSubscriptionToken(connection1, missionName)).isEqualTo(missionSubToken);
    }

    private void setupCotResponse(@Nonnull IMapObject mapObject)
    {
        String cot = cotExporterManager.export(mapObject).toString();
        String uid = mapObject.getUid();
        setupCotResponse(uid, cot);
    }

    private void setupCotResponse(@Nonnull String uid, @Nonnull String cot)
    {
        when(cotApi1.getCotItem(any(), any(), eq(uid)))
                .thenReturn(Response.builder()
                        .status(200)
                        .body(cot, StandardCharsets.UTF_8)
                        .request(mock(Request.class))
                        .build());
    }

    private void setupFileResponse(@Nonnull String sha256, @Nonnull String text)
    {
        // If we run this setup multiple times, prior matchers will be hit, so we need a null check to allow chaining here.
        MapMatcher<String, Object> hashEquals = queryParams -> queryParams != null && sha256.equals(queryParams.get("hash"));
        when(syncApi1.content(any(), argThat(hashEquals)))
                .thenReturn(Response.builder()
                        .status(200)
                        .body(text, StandardCharsets.UTF_8)
                        .request(mock(Request.class))
                        .build());
        when(syncApi1.upload(any(), argThat(hashEquals), any(), any()))
                .thenReturn(new UploadedResourceResponse(UUID.randomUUID().toString(), Instant.now(), "text/plain", clientUidStr, sha256));
    }

    @Nonnull
    private static UidDetails detailsOf(@Nonnull IMapObject mapObject)
    {
        var location = new Location(mapObject.getLocation().getLongitude(), mapObject.getLocation().getLatitude());
        String type = mapObject.getType();
        String name = mapObject.getName();
        List<String> attachments = mapObject.getAttachments().stream().map(HashingUtils::sha256sum).toList();
        return detailsOf(type, name, location, attachments);
    }

    @Nonnull
    private static UidDetails detailsOf(@Nonnull String type, @Nonnull String name, @Nonnull Location location, @Nullable List<String> attachments)
    {
        return new UidDetails(
                null,
                null,
                type, // CoT type
                null, null,
                location,
                name, // Callsign (name/title fields largely unused in ecosystem, this is favored)
                null,
                attachments // File attachments as sha256 hashes
        );
    }

    // TODO: Remaining tests for coverage
    //  - GUID alternatives
    //       - Not exposed from DataSyncManager due to name-only usage
    //  - Items
    //      - getLocalDownloadedResource
    //      - getLocalDownloadedCot
    //      - manuallyDownloadResource
    //      - manuallyDownloadCot
    //      - download
    //      - addUserLog
    //  - Persist
    //    - Show that holding some missions, persisting, and restoring later (After clearing) restores expected contents
    //    - Assert subscribed missions stay subscribed
    //    - Assert downloaded (not subscribed missions) stay downloaded

    private void sendEvent(@Nonnull String eventXml)
    {
        dataSync.importData(CotEvent.parse(eventXml));
    }

    private void connectTo(@Nonnull ITakServerConnection connection)
    {
        dataSync.onServerConnected(new TakServerConnectionOpened(connection));
    }

    private void setupSubscribeScenario(@Nonnull Mission mission)
    {
        String missionName = mission.name();
        String missionGuid = mission.guid();
        when(missionApi1.getAllMissions(any(), any())).thenReturn(List.of(mission));

        when(missionApi1.getMission(any(), any(), eq(missionName)))
                .thenReturn(List.of(mission));
        when(missionApi1.getMissionByGuid(any(), any(), eq(missionGuid)))
                .thenReturn(List.of(mission));

        when(missionApi1.removeMission(any(), any(), eq(missionName))).thenReturn(List.of(mission));
        when(missionApi1.removeMissionByGuid(any(), any())).thenReturn(List.of(mission)); // Yes, this endpoint is unique by not passing the guid as a path parameter...

        setupSubscribeEndpoint(missionName, missionGuid);

        when(missionApi1.addMissionPackage(any(), any(), eq(missionName), any()))
                .thenReturn(Collections.emptyList()); // Response is list of incompatibilities, so empty is a success.

        when(missionApi1.addMissionContent(any(), any(), eq(missionName), any()))
                .thenReturn(List.of(mission));
        when(missionApi1.addMissionContentByGuid(any(), any(), eq(missionName), any()))
                .thenReturn(List.of(mission));
    }

    private void setupSubscribeEndpoint(String missionName, String missionGuid)
    {
        MissionSubscription missionSubscription = new MissionSubscription("uid", DataSyncManagerTest.missionSubToken, clientUidStr, "username", Instant.now(), null);
        when(missionApi1.createMissionSubscription(any(), any(), eq(missionName)))
                .thenReturn(missionSubscription);
        when(missionApi1.createMissionSubscriptionByGuid(any(), any(), eq(missionGuid)))
                .thenReturn(missionSubscription);
    }

    private interface MapMatcher<K, V> extends ArgumentMatcher<Map<K, V>>
    {
        @Override
        default Class<?> type()
        {
            return Map.class;
        }
    }
}
