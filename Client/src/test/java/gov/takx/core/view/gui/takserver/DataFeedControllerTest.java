package gov.takx.core.view.gui.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.ExternalMissionData;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionBuilder;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.data.MissionChangeType;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.takserver.IExtendedDataSyncManager;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.core.view.gui.takserver.timeline.DataFeedTimelinePanel;
import gov.takx.platform.utils.StandardTimeFormatting;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link DataFeedPanelController}.
 *
 * @since 5.0
 */
public class DataFeedControllerTest extends AIntegrationTest
{
    private static final String creatorUid = "test-user";

    @Mocked
    private ITakServerConnection connection;
    @Mocked
    private StandardTimeFormatting timeFormatting;
    @Mocked
    private IExtendedDataSyncManager dataSyncManager;
    @Mocked
    private IContactNameResolver contactNameResolver;
    @Mocked
    private IUISpaceManager uiSpaceManager;
    @Mocked
    private MissionChangeInfoResolver changeIconResolver;
    @Mocked
    private MissionResourcePreviewProvider previewProvider;

    private DataFeedPanelController controller;

    @BeforeEach
    void beforeEach(@Mocked DataFeedInfoPanel infoPanel)
    {
        controller = Mockito.spy(new DataFeedPanelController(dataSyncManager,
                contactNameResolver,
                timeFormatting,
                uiSpaceManager,
                changeIconResolver,
                previewProvider,
                () -> infoPanel,
                true));

        Mockito.reset(uiSpaceManager);

        // Need to stub this method out since it triggers a NPE attempting to lock the UI state
        Mockito.doAnswer(Answers.RETURNS_MOCKS).when(controller).openMissionDetails(any(), any());
    }

    @Test
    public void updateMission_updatesTheCorrectMission_whenTheMissionObjectChanges()
    {
        final List<MissionChange> missionChangeList = new ArrayList<>();
        missionChangeList.add(createMissionChange(MissionChangeType.CREATE_MISSION));

        // Single change event
        Mission mission = createMission(missionChangeList);
        controller.addOrUpdateMission(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission).size()).isEqualTo(1);

        // Add a change event list and update mission
        missionChangeList.add(createMissionChange(MissionChangeType.ADD_CONTENT));
        controller.addOrUpdateMission(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission).size()).isEqualTo(2);

        // Add a change event list and update mission
        missionChangeList.add(createMissionChange(MissionChangeType.ADD_CONTENT));

        // New instances of existing missions may be created when updates occur.
        // Create new instance of same mission and verify updateMission completes as expected
        mission = createMission(missionChangeList);
        controller.addOrUpdateMission(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission).size()).isEqualTo(3);
    }

    @Test
    public void openMissionTimeline_addsMissingMission()
    {
        final Mission mission = createMission();
        controller.openMissionTimeline(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission)).isNotNull();
    }

    @Test
    public void openMissionTimeline_callsUiSpaceManager()
    {
        final Mission mission = createMission();
        controller.openMissionTimeline(connection, mission);

        // We must wait for the EDT to handle opening the panel for this assertion
        SwingTestUtils.waitForEdtEvents();
        verify(uiSpaceManager).showComponent(any(DataFeedTimelinePanel.class));
    }

    @Test
    public void addMission_buildsTimeline()
    {
        final Mission mission = createMission();

        controller.addOrUpdateMission(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission)).isNotNull();
    }

    @Test
    public void subscribeMission_buildsAllUis()
    {
        final Mission mission = createMission();

        controller.onSubscribe(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission)).isNotNull();
        assertThat(controller.getMissionInfoPanel(connection, mission)).isNotNull();
        assertThat(controller.getMissionListEntry(connection, mission)).isNotNull();

        controller.onUnsubscribe(connection, mission);
    }

    @Test
    public void removeMission_removesTimeline()
    {
        final Mission mission = createMission();

        controller.addOrUpdateMission(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission)).isNotNull();

        controller.removeMissionTimeline(connection, mission);
        assertThat(controller.getMissionTimeline(connection, mission)).isNull();
    }

    @Test
    public void contactChange_updatesChangelog()
    {
        final Mission mission = createMission(List.of(createMissionChange(MissionChangeType.CREATE_MISSION)));
        final DataFeedInfoPanel panel = controller.openMissionDetails(connection, mission);
        when(panel.getCurrentMission()).thenReturn(mission);
        doAnswer(Answers.CALLS_REAL_METHODS).when(panel).contactUpdated(any());

        // Simulate the contact service notifying the panel of a contact update
        IContact contact = mock(IContact.class);
        when(contact.getUid()).thenReturn(creatorUid);
        panel.contactUpdated(contact);

        // We must wait for the EDT to handle opening the panel for this assertion
        // Assert that the changelog model refreshes.
        SwingTestUtils.waitForEdtEvents();
        verify(panel).repopulateChangelogModel(eq(mission));
    }

    private Mission createMission()
    {
        return createMission(Collections.emptyList());
    }

    private Mission createMission(List<MissionChange> missionChangeList)
    {
        return new MissionBuilder("mission")
                .withDescription("desc")
                .withCreatorUid(creatorUid)
                .withMissionChanges(missionChangeList)
                .build();
    }

    private MissionChange createMissionChange(MissionChangeType missionChangeType)
    {
        return new MissionChange(missionChangeType, "mission", Instant.now(),
                creatorUid, "contentUid", null, new ExternalMissionData(null, "<test-change>", null, null, null, null),
                null, null, null);
    }
}