package gov.takx.core.view.gui.takserver;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.comms.takserver.IDataSyncMissionDownloadListener;
import gov.tak.api.comms.takserver.IDataSyncMissionListener;
import gov.tak.api.comms.takserver.IDataSyncSubscribedMissionListener;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.core.comms.takserver.IDataSyncRestorationListener;
import gov.tak.core.comms.takserver.IExtendedDataSyncManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.event.StartBackgroundProjectServices;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.takserver.timeline.DataFeedTimelinePanel;
import gov.takx.core.view.gui.takserver.timeline.MissionChangeOrLogEntry;
import gov.takx.core.view.gui.takserver.timeline.TimelineEntry;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.Component;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static gov.takx.core.project.ServiceOrdering.START_BACKGROUND_GenericService;
import static jakarta.interceptor.Interceptor.Priority.PLATFORM_AFTER;

/**
 * Manages the state of data-sync panels.
 * <ul>
 *     <li>{@link DataFeedInfoPanel}</li>
 *     <li>{@link DataFeedListEntry}</li>
 *     <li>{@link DataFeedTimelinePanel}</li>
 * </ul>
 * 
 * @since 5.0
 */
@ApplicationScoped
public class DataFeedPanelController implements IDataSyncMissionListener, IDataSyncSubscribedMissionListener,
        IDataSyncMissionDownloadListener, IDataSyncRestorationListener
{
    private static final URL DATA_SYNC_ICON_URL = UIResources.getUrl(ResourceDirectory.TAK_ASSETS_PNG, "Data-Sync_32x32.png");
    private static final URL TIMELINE_ICON_URL = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "feed-timeline.png");
    private static final ComponentDescriptor TIMELINE_COMPONENT_DESCRIPTOR =
            new ComponentDescriptorBuilder(JPanel.class, "Timeline")
                    .setIcon(TIMELINE_ICON_URL)
                    .setComponentPosition(ComponentPosition.RIGHT)
                    .setToolbarButtonVisible(true)
                    .setParentScrollingEnabled(false)
                    .build();
    private static final ComponentDescriptor FEED_DETAILS_COMPONENT_DESCRIPTOR =
            new ComponentDescriptorBuilder(JPanel.class, "Feed Details")
                    .setIcon(DATA_SYNC_ICON_URL)
                    .setComponentPosition(ComponentPosition.RIGHT)
                    .setToolbarButtonVisible(false)
                    .setParentScrollingEnabled(false)
                    .build();
    private static final DataFeedListPanel LIST_PANEL = DataFeedListPanel.getInstance();

    private final Map<String, DataFeedListEntry> feedEntries = new ConcurrentHashMap<>();
    private final Map<String, DataFeedInfoPanel> feedPanels = new ConcurrentHashMap<>();
    private final Map<String, List<TimelineEntry>> timelineMaps = new ConcurrentHashMap<>();
    private final DataFeedTimelinePanel dataFeedTimelinePanel = new DataFeedTimelinePanel((con, name) -> timelineMaps.get(key(con, name)));
    private final IExtendedDataSyncManager dataSyncManager;
    private final IContactNameResolver contactNameResolver;
    private final IUISpaceManager uiSpaceManager;
    private final StandardTimeFormatting timeFormatting;
    private final MissionChangeInfoResolver changeInfoResolver;
    private final MissionResourcePreviewProvider previewProvider;
    private final Provider<DataFeedInfoPanel> dataFeedInfoPanelProvider;
    private final boolean underTest;
    private final IPreferenceMonitor<String> callsignMonitor = PreferenceEntry.CALL_SIGN.newStringMonitor(s -> onPreferenceChanged(), false);
    private final IPreferenceMonitor<String> timeFormattingMonitor = UserPreferenceEntry.DATE_FORMAT.newStringMonitor(s -> onPreferenceChanged(), false);
    private DataFeedInfoPanelHolder dataFeedInfoPanelHolder;

    @Inject
    public DataFeedPanelController(IExtendedDataSyncManager dataSyncManager,
                                   IContactNameResolver contactNameResolver,
                                   StandardTimeFormatting timeFormatting,
                                   IUISpaceManager uiSpaceManager,
                                   MissionChangeInfoResolver changeInfoResolver,
                                   MissionResourcePreviewProvider previewProvider,
                                   Provider<DataFeedInfoPanel> dataFeedInfoPanelProvider,
                                   @PlatformUnderTest boolean underTest)
    {
        this.dataSyncManager = dataSyncManager;
        this.contactNameResolver = contactNameResolver;
        this.timeFormatting = timeFormatting;
        this.uiSpaceManager = uiSpaceManager;
        this.changeInfoResolver = changeInfoResolver;
        this.previewProvider = previewProvider;
        this.dataFeedInfoPanelProvider = dataFeedInfoPanelProvider;
        this.underTest = underTest;

        LIST_PANEL.setStatusLookup(dataSyncManager::getDownloadState);
    }

    /** Start this service. */
    private void startService(@Observes @Priority(START_BACKGROUND_GenericService) StartBackgroundProjectServices ignored)
    {
        // Nothing externally injects this controller, thus we need to eagerly initialize this service.
        // It reaches out to update 'dependent' components like the different data-sync user interfaces.
    }

    @PostConstruct
    private void postConstruct()
    {
        // While doing tests, we may be operating on partial data via mocks which can cause undefined behavior.
        // When we test the behavior of this controller, we do not rely on data-sync listener callbacks either,
        // so we do not need the listeners even then.
        if (underTest) return;

        callsignMonitor.start();
        timeFormattingMonitor.start();

        dataSyncManager.addDataSyncMissionListener(this);
        dataSyncManager.addDataSyncMissionDownloadListener(this);
        dataSyncManager.addDataSyncSubscribedMissionListener(this);
        dataSyncManager.addDataSyncRestorationListener(this);
    }

    @SuppressWarnings("unused")
    private void onProjectOpening(@Observes ProjectOpened event)
    {
        dataSyncManager.currentMissions().forEach((connection, missions) -> {
            for (Mission mission : missions) onNewMission(connection, mission);
        });
    }

    @SuppressWarnings("unused")
    private void onProjectClosing(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) ProjectClosing event)
    {
        callsignMonitor.stop();
        timeFormattingMonitor.stop();

        // We'll want to re-allocate the UI components if the user opens a new project later.
        // Setting the holder to null will allow the components to be lazily re-created later in 'openMissionDetails'
        dataFeedInfoPanelHolder = null;

        // Clear UI instances
        dataFeedTimelinePanel.clear();
        timelineMaps.clear();
        feedEntries.clear();
        feedPanels.clear();

        // Clear list entries
        LIST_PANEL.sortFeeds(Collections.emptyList());
    }

    @SuppressWarnings("unused")
    private void onConnectionOpened(@Observes TakServerConnectionOpened event)
    {
        updatePanelsForConnection(event.getConnection());
    }

    @SuppressWarnings("unused")
    private void onConnectionClosed(@Observes TakServerConnectionClosed event)
    {
        updatePanelsForConnection(event.getConnection());
    }

    @SuppressWarnings("unused")
    private void onConnectionRemoved(@Observes @Priority(PLATFORM_AFTER) TakServerConnectionRemoved event)
    {
        // When a connection is removed, we want to remove all the feeds from the
        // display that are associated with that server. The exception are feeds
        // that the user has intentionally downloaded.
        ITakServerConnection connection = event.getConnection();
        for (var entry : new ArrayList<>(feedEntries.entrySet()))
        {
            String key = entry.getKey();
            DataFeedListEntry entryPanel = entry.getValue();

            ITakServerConnection currentConnection = entryPanel.getCurrentConnection();
            if (connection.equals(currentConnection))
            {
                String missionName = entryPanel.getCurrentMission().name();
                DataFeedInfoPanel infoPanel = feedPanels.get(key);

                // Skip items that have been downloaded locally, we want to keep those
                // in the displayed list since the local content is still kept.
                if (dataSyncManager.isDownloaded(connection, missionName))
                {
                    entryPanel.updateConnectionState(false);
                    if (infoPanel != null) infoPanel.updateStatus();
                    continue;
                }

                // Disable the panels so they are not interactable.
                entryPanel.setEnabled(false);
                if (infoPanel != null) infoPanel.setEnabled(false);

                // Remove them from the stores.
                dataFeedTimelinePanel.removeFeed(connection, missionName);
                feedPanels.remove(key);
                feedEntries.remove(key);
            }
        }

        // Re-sort with the updated entries collection will remove entry panels.
        LIST_PANEL.sortFeeds(feedEntries.values());
    }

    private void updatePanelsForConnection(@Nonnull ITakServerConnection connection)
    {
        boolean connected = connection.isConnected();
        URI connectionUri = connection.getUri();
        String connectionUsername = connection.getConnectionConfig().getUsername();
        for (DataFeedListEntry entry : feedEntries.values())
        {
            ITakServerConnection currentConnection = entry.getCurrentConnection();
            if (currentConnection == null) continue;

            if (connection.equals(currentConnection))
            {
                entry.updateConnectionState(connected);
            } else if (connectionUri.equals(currentConnection.getUri()) && Objects.equals(connectionUsername, currentConnection.getConnectionConfig().getUsername()))
            {
                entry.setCurrentConnection(connection);
                entry.updateConnectionState(connected);
            }
        }

        for (DataFeedInfoPanel panel : feedPanels.values())
        {
            ITakServerConnection currentConnection = panel.getCurrentConnection();
            if (currentConnection == null) continue;

            if (connection.equals(currentConnection))
            {
                panel.updateStatus();
            } else if (connectionUri.equals(currentConnection.getUri()) && Objects.equals(connectionUsername, currentConnection.getConnectionConfig().getUsername()))
            {
                panel.setCurrentConnection(connection);
                panel.updateStatus();
            }
        }
    }

    /**
     * @since 5.5
     */
    private void onPreferenceChanged()
    {
        for (var entry : dataSyncManager.currentMissions().entrySet())
        {
            ITakServerConnection connection = entry.getKey();
            for (Mission mission : entry.getValue())
            {
                addOrUpdateMission(connection, mission);
            }
        }
    }

    @Override
    public void onNewMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        addOrUpdateMission(connection, mission);
    }

    @Override
    public void onRemoveMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        // Skip removal if its locally downloaded
        if (dataSyncManager.isDownloaded(connection, mission.name())) return;

        // Update data feed list
        String key = key(connection, mission);
        DataFeedListEntry feedEntry = feedEntries.remove(key);

        // Skip if no feed exists, shouldn't happen
        if (feedEntry == null) return;

        // Remove the entry in the list and close data feed info dialogs
        LIST_PANEL.removeListEntry(feedEntry);
        closeMissionDetails(connection, mission);

        // If the user did not request this deletion, and they have this mission downloaded,
        // allow them to choose if they want to keep local files.
        String name = mission.name();
        if (!feedEntry.hasUserRequestedRemoteDeletion() && !mission.contents().isEmpty() &&
                dataSyncManager.getDownloadState(connection, name).isAtLeastPartiallyDownloaded())
        {
            int dialogResult = JOptionPane.showConfirmDialog(LIST_PANEL, "The feed '" + name + "' was remotely removed.\n"
                    + "Do you want to keep the files locally?", name + " removed", JOptionPane.YES_NO_OPTION);
            if (dialogResult != JOptionPane.YES_OPTION) dataSyncManager.delete(connection, name, true, false);
        }
    }

    @Override
    public void onUpdateMission(@Nonnull ITakServerConnection connection, @Nonnull Mission oldMission, @Nonnull Mission mission)
    {
        addOrUpdateMission(connection, mission);
    }

    @Override
    public void onSubscribe(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        addOrUpdateDownloadedMission(connection, mission);

        // Open a panel for the mission
        DataFeedInfoPanel panel = getOrCreateMissionDetailsPanel(connection, mission);
        panel.onSubscribe(connection, mission);
        openMissionDetails(connection, mission);
    }

    @Override
    public void onUnsubscribe(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        // If unsubscribing happened as a result of a feed being deleted (which happened earlier while we
        // were subscribed and thus wasn't removed) then we should remove it now.
        if (!dataSyncManager.isTracked(connection, mission.name()))
        {
            onRemoveMission(connection, mission);
            return;
        }

        addOrUpdateMission(connection, mission);

        // After removing our subscription we lose the full changelog model. Remove the timeline option for this feed.
        dataFeedTimelinePanel.removeFeed(connection, mission.name());

        // Notify the mission details panel about the unsubscription.
        DataFeedInfoPanel missionDetailsPanel = getMissionDetailsPanel(connection, mission);
        if (missionDetailsPanel != null)
        {
            missionDetailsPanel.onUnsubscribe(connection, mission);
        }

        // Close the associated mission's panel
        closeMissionDetails(connection, mission);
    }

    @Override
    public void onSubscribedMissionUpdate(@Nonnull ITakServerConnection connection, @Nonnull Mission oldMission, @Nonnull Mission mission)
    {
        // Update the associated mission's panel
        DataFeedInfoPanel panel = getOrCreateMissionDetailsPanel(connection, mission);
        panel.onSubscribedMissionUpdate(connection, oldMission, mission);

        addOrUpdateMission(connection, mission);
    }

    @Override
    public void onSubscribedMissionRestore(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        addOrUpdateDownloadedMission(connection, mission);
    }

    @Override
    public void onDownloadedMissionRestore(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        addOrUpdateDownloadedMission(connection, mission);
    }

    @Override
    public void onDownloadResource(@Nonnull ITakServerConnection connection, @Nonnull Mission mission, @Nonnull Resource resource, @Nonnull File localPath)
    {
        // Update the associated mission's panel
        DataFeedInfoPanel panel = getOrCreateMissionDetailsPanel(connection, mission);
        panel.onDownloadResource(connection, mission, resource, localPath);

        addOrUpdateMission(connection, mission);
    }

    @Override
    public void onDownloadCot(@Nonnull ITakServerConnection connection, @Nonnull Mission mission, @Nonnull MissionAdd<String> cot, @Nonnull File localPath)
    {
        // Update the associated mission's list entry
        addOrUpdateMission(connection, mission);

        // Update the associated mission's panel
        getOrCreateMissionDetailsPanel(connection, mission).onDownloadCot(connection, mission, cot, localPath);
    }

    /**
     * Removes the timeline display for the given mission.
     *
     * @param connection Connection to server defining the mission.
     * @param mission The mission whose feed timeline should be removed
     */
    public void removeMissionTimeline(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        String key = key(connection, mission);

        timelineMaps.remove(key);
        dataFeedTimelinePanel.removeFeed(connection, mission.name());
    }

    /**
     * Adds the details tab <i>(if needed)</i> and displays the mission details.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission to show details of.
     * @return The details panel for the given mission.
     */
    @Nonnull
    public DataFeedInfoPanel openMissionDetails(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        if (dataFeedInfoPanelHolder == null)
        {
            dataFeedInfoPanelHolder = new DataFeedInfoPanelHolder();
            uiSpaceManager.addComponent(FEED_DETAILS_COMPONENT_DESCRIPTOR, dataFeedInfoPanelHolder);
            uiSpaceManager.addComponent(TIMELINE_COMPONENT_DESCRIPTOR, dataFeedTimelinePanel);
        }

        DataFeedInfoPanel infoPanel = getOrCreateMissionDetailsPanel(connection, mission);
        dataFeedInfoPanelHolder.setPanel(infoPanel);
        show(dataFeedInfoPanelHolder);
        return infoPanel;
    }

    /**
     * Displays the timeline of the given mission.
     *
     * @param connection Connection to server defining the mission.
     * @param mission The mission whose feed timeline should be displayed
     */
    public void openMissionTimeline(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        final String missionName = mission.name();

        // Add mission if it does not already exist
        if (!timelineMaps.containsKey(key(connection, mission))) addOrUpdateMission(connection, mission);

        // Select the mission's feed
        dataFeedTimelinePanel.setSelectedFeed(connection, missionName);

        // Open the Timeline panel
        show(dataFeedTimelinePanel);
    }

    /**
     * Updates all UI panels associated with the given mission.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission outline to update details with.
     * @since 5.3
     */
    private void addOrUpdateDownloadedMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        addOrUpdateMission(connection, mission);

        // Subscribed feeds are downloaded in full, including the changelog, which allows us to create the timeline display.
        dataFeedTimelinePanel.addFeed(connection, mission.name());
    }

    /**
     * Updates all UI panels associated with the given mission.
     * 
     * @param connection Connection to server defining the mission.
     * @param mission Mission outline to update details with.
     */
    public void addOrUpdateMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        // Update the timeline panel for the mission
        String missionName = mission.name();
        String key = key(connection, mission);

        // We can get smarter about updating the existing entries, but just rebuild for now
        timelineMaps.put(key, buildMissionTimeline(connection, mission));

        // If the feed is currently being displayed, force a refresh
        if (dataFeedTimelinePanel.isFeedSelected(connection, missionName))
        {
            dataFeedTimelinePanel.showTimeline(connection, missionName);
        }

        // Update the associated mission's list entry
        DataFeedListEntry entry = getOrCreateListEntry(connection, mission);
        entry.updateMission(connection, mission);
        LIST_PANEL.sortFeeds(feedEntries.values());

        // Update the associated mission's info panel
        DataFeedInfoPanel panel = feedPanels.get(key);
        if (panel != null) panel.updateStatus();
    }

    /**
     * Allows test to check the controller's state.
     *
     * @param connection  Connection to server defining the mission.
     * @param mission The mission to check.
     * @return The panel housing {@link Mission} information, or {@code null} if there is no panel for the mission
     */
    @VisibleForTesting
    public DataFeedListEntry getMissionListEntry(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return feedEntries.get(key(connection, mission));
    }

    /**
     * Allows test to check the controller's state.
     *
     * @param connection  Connection to server defining the mission.
     * @param mission The mission to check.
     * @return The panel housing {@link Mission} information, or {@code null} if there is no panel for the mission
     */
    @VisibleForTesting
    public DataFeedInfoPanel getMissionInfoPanel(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return feedPanels.get(key(connection, mission));
    }

    /**
     * Allows test to check the controller's state.
     *
     * @param connection  Connection to server defining the mission.
     * @param mission The mission to check.
     * @return The list of {@link TimelineEntry} objects or {@code null} if there is no timeline for the mission
     */
    @VisibleForTesting
    public List<TimelineEntry> getMissionTimeline(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return timelineMaps.get(key(connection, mission));
    }

    /**
     * Builds the timeline for a {@link Mission}.
     *
     * @param connection Connection to server defining the mission.
     * @param mission The mission to pull user/change logs from.
     * @return The mission's timeline containing all user and change log entries in timestamp sorted order.
     */
    @Nonnull
    private List<TimelineEntry> buildMissionTimeline(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        List<MissionChangeOrLogEntry> joined = MissionChangeOrLogEntry.listFromMission(mission);
        List<TimelineEntry> entries = new ArrayList<>(joined.size());
        for (int i = 0; i < joined.size(); i++)
        {
            MissionChangeOrLogEntry e = joined.get(i);
            MissionChange missionChange = e.missionChange();
            entries.add(new TimelineEntry(
                    changeInfoResolver.getChangeIcon(e),
                    changeInfoResolver.getChangeMessage(joined, e, i),
                    timeFormatting.formatDateOnlyWithCurrent(e.timestamp().toEpochMilli()),
                    changeInfoResolver.getDetailsMessage(mission, e),
                    getExtraDetailsProvider(connection, missionChange)));
        }
        return entries;
    }

    /**
     * @param connection Connection to the server defining the mission with the change
     * @param missionChange The mission change event
     * @return A supplier that provides preview for the associated {@link Resource} of the given mission change.
     * {@code null} if there is no associated resource.
     */
    @Nullable
    private Supplier<JPanel> getExtraDetailsProvider(@Nonnull ITakServerConnection connection, @Nullable MissionChange missionChange)
    {
        if (missionChange == null) return null;

        final Resource resource = missionChange.contentResource();
        if (resource != null)
        {
            File localFile = dataSyncManager.getLocalDownloadedResource(connection, resource);
            if (localFile != null && localFile.isFile())
            {
                return () -> previewProvider.previewFile(resource, localFile);
            }
        }

        return null;
    }

    /**
     * @param connection Connection to server defining the mission.
     * @param mission Mission to get details of.
     *
     * @return Panel representing the mission, or {@code null} if none has been created.
     */
    @Nullable
    private DataFeedInfoPanel getMissionDetailsPanel(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return feedPanels.get(key(connection, mission));
    }

    /**
     * @param connection Connection to server defining the mission.
     * @param mission Mission to show details of.
     *
     * @return Panel representing the mission.
     */
    @Nonnull
    private DataFeedInfoPanel getOrCreateMissionDetailsPanel(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return feedPanels.computeIfAbsent(key(connection, mission), k -> {
            DataFeedInfoPanel panel = dataFeedInfoPanelProvider.get();
            panel.initializeState(connection, mission);
            panel.updateStatus();
            return panel;
        });
    }

    /**
     * Closes and removes the details tab if the mission is currently being displayed.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission to close detail display of, if present.
     */
    private void closeMissionDetails(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        final DataFeedInfoPanel infoPanel = feedPanels.remove(key(connection, mission));
        if (infoPanel != null && dataFeedInfoPanelHolder != null && dataFeedInfoPanelHolder.removeIfActive(infoPanel))
        {
            uiSpaceManager.removeComponent(dataFeedInfoPanelHolder);
            dataFeedInfoPanelHolder = null;
        }
    }

    /**
     * @param component Panel to display.
     */
    @InvokeOnEDT
    private void show(@Nonnull Component component)
    {
        uiSpaceManager.showComponent(component);
    }

    /**
     * @param connection Connection to server defining the mission.
     * @param mission Mission to show details of.
     *
     * @return Mission details entry.
     */
    @Nonnull
    private DataFeedListEntry getOrCreateListEntry(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return feedEntries.computeIfAbsent(key(connection, mission),
                k -> new DataFeedListEntry(this, dataSyncManager, contactNameResolver, timeFormatting));
    }

    @Nonnull
    private static String key(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        return key(connection, mission.name());
    }

    @Nonnull
    private static String key(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        return connection.getUri() + missionName;
    }
}
