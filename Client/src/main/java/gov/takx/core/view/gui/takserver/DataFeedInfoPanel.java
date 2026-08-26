package gov.takx.core.view.gui.takserver;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataPackageManager;
import gov.tak.api.comms.takserver.IDataSyncMissionDownloadListener;
import gov.tak.api.comms.takserver.IDataSyncSubscribedMissionListener;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.MissionDownloadStatus;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.data.Location;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UidDetails;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactListener;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.api.contact.IContactService;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.icons.ICotIconResolver;
import gov.tak.core.comms.takserver.DataSyncManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IMultiSelector;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.media.MediaType;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.ShapeType;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageFileChooserFilter;
import gov.takx.commons.swing.SingleTypeFileFilter;
import gov.takx.commons.swing.WrapLayout;
import gov.takx.core.mapobject.CancelableMapObjectSelectionListener;
import gov.takx.core.view.gui.takserver.timeline.MissionChangeOrLogEntry;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTaskPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static gov.takx.commons.swing.ImageUtils.DEFAULT_ICON_WIDTH;

/**
 * Shows current information of a subscribed data feed <i>(mission)</i>.
 * <p/>
 * Events from data-sync interfaces are called by {@link DataFeedListPanel} which manages instances of this panel.
 *
 * @since 5.0
 */
@Dependent
public class DataFeedInfoPanel extends JPanel implements IDataSyncSubscribedMissionListener, IDataSyncMissionDownloadListener, IContactListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ImageIcon SYNC_OK_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-OK_36x36.png");
    private static final ImageIcon SYNC_OK_NOT_SUB_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-OK-NotSub_36x36.png");
    private static final ImageIcon SYNC_PROGRESS_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-Progress_36x36.png");
    private static final ImageIcon SYNC_DISCONNECTED_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-Not-Available_36x36.png");
    private static final ImageIcon TIMELINE_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "feed-timeline.png");
    private static final ImageIcon GROUP_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "group.png");
    private static final ImageIcon DOWNLOAD_ICON = UIResources.getToolbarIcon(ResourceDirectory.PLATFORM_ICONS, "cloud_download.png");
    private static final Comparator<DisplayedFile> CMP_FILE_NAME = Comparator.comparing(o -> o.resourcePath().getName().toLowerCase());
    private static final Comparator<DisplayedFile> CMP_FILE_TIME = Comparator.comparing(o -> o.resource().submissionTime());
    private static final Comparator<DisplayedMapItem> CMP_COT_NAME = (o1, o2) -> {
        String callsign1 = Objects.requireNonNullElse(o1.callsign, "");
        String callsign2 = Objects.requireNonNullElse(o2.callsign, "");
        return String.CASE_INSENSITIVE_ORDER.compare(callsign1, callsign2);
    };
    private static final Comparator<DisplayedMapItem> CMP_COT_TIME = Comparator.comparingLong(o -> o.time);
    private static final String NO_KEYWORDS = "(No keywords)";
    private final IPreferenceMonitor<String> callsignMonitor = PreferenceEntry.CALL_SIGN.newStringMonitor(v -> refreshSelfReferences());
    private final Map<File, DisplayedFile> displayedFiles = new ConcurrentHashMap<>();
    private final Map<String, DisplayedMapItem> displayedMapItems = new ConcurrentHashMap<>();
    private final DataSyncManager dataSyncManager;
    private final IDataPackageManager dataPackageManager;
    private final IMapObjectFinder mapObjectFinder;
    private final IGlobeManager globeManager;
    private final IMultiSelector multiSelector;
    private final IContactNameResolver contactNameResolver;
    private final ICotIconResolver cotIconResolver;
    private final StandardTimeFormatting timeFormatting;
    private final MissionChangeInfoResolver changeInfoResolver;
    private final MissionLogEntryInfoResolver entryInfoResolver;
    private final MissionResourcePreviewProvider previewProvider;
    private final DataFeedPanelController panelController;
    private final UUID nodeUuid;
    private final JPanel mapItemFlowPanel = new JPanel();
    private final JPanel photosFlowPanel = new JPanel();
    private final JPanel filesFlowPanel = new JPanel();
    private final JPanel videosFlowPanel = new JPanel();
    private final JList<MissionChangeOrLogEntry> changelogList = new JList<>();
    private final DefaultListModel<MissionChangeOrLogEntry> changelogModel = new DefaultListModel<>();
    private ITakServerConnection connection;
    private Mission mission;

    @Inject
    public DataFeedInfoPanel(DataSyncManager dataSyncManager, IMapObjectFinder mapObjectFinder,
                             IContactNameResolver contactNameResolver, IContactService contactService,
                             IGlobeManager globeManager, IMultiSelector multiSelector,
                             ICotIconResolver cotIconResolver, MissionChangeInfoResolver changeInfoResolver,
                             IDataPackageManager dataPackageManager, MissionLogEntryInfoResolver entryInfoResolver,
                             MissionResourcePreviewProvider previewProvider, DataFeedPanelController panelController,
                             StandardTimeFormatting timeFormatting, @NodeUUID UUID nodeUuid)
    {
        this.dataSyncManager = dataSyncManager;
        this.dataPackageManager = dataPackageManager;
        this.mapObjectFinder = mapObjectFinder;
        this.contactNameResolver = contactNameResolver;
        this.globeManager = globeManager;
        this.multiSelector = multiSelector;
        this.cotIconResolver = cotIconResolver;
        this.changeInfoResolver = changeInfoResolver;
        this.entryInfoResolver = entryInfoResolver;
        this.previewProvider = previewProvider;
        this.panelController = panelController;
        this.timeFormatting = timeFormatting;
        this.nodeUuid = nodeUuid;

        initComponents();

        changelogList.setModel(changelogModel);
        changelogList.setCellRenderer(new MissionChangelogCellRenderer());
        mapItemsGroup.setLayout(new BorderLayout());
        photosFlowPanel.setLayout(new BorderLayout());
        filesFlowPanel.setLayout(new BorderLayout());
        videosFlowPanel.setLayout(new BorderLayout());
        mapItemsGroup.add(mapItemFlowPanel, BorderLayout.CENTER);
        photosGroup.add(photosFlowPanel, BorderLayout.CENTER);
        filesGroup.add(filesFlowPanel, BorderLayout.CENTER);
        videosGroup.add(videosFlowPanel, BorderLayout.CENTER);
        logsGroup.add(changelogList, BorderLayout.CENTER);

        int initialWidth = 400;
        mapItemFlowPanel.setLayout(new WrapLayout(initialWidth));
        photosFlowPanel.setLayout(new WrapLayout(initialWidth));
        filesFlowPanel.setLayout(new WrapLayout(initialWidth));
        videosFlowPanel.setLayout(new WrapLayout(initialWidth));

        timelineButton.setIcon(TIMELINE_ICON);
        invitesButton.setIcon(GROUP_ICON);

        groupsScrollWrapper.getVerticalScrollBar().setUnitIncrement(25);

        contactService.registerContactListener(this);
    }

    /**
     * @param connection Current connection to server declaring the {@link #getCurrentMission() current mission}.
     * @since 5.3
     */
    public void setCurrentConnection(@Nonnull ITakServerConnection connection)
    {
        this.connection = connection;
    }

    /**
     * @return Current connection to server declaring the {@link #getCurrentMission() current mission}.
     */
    @Nullable
    public ITakServerConnection getCurrentConnection()
    {
        return connection;
    }

    /**
     * @return Current mission represented by this entry.
     */
    @Nullable
    public Mission getCurrentMission()
    {
        return mission;
    }

    @Override
    public void onSubscribe(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        // no-op
    }

    @Override
    public void onUnsubscribe(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        // no-op
    }

    @Override
    public void onSubscribedMissionUpdate(@Nonnull ITakServerConnection connection, @Nonnull Mission oldMission, @Nonnull Mission mission)
    {
        setCurrentConnection(connection);

        this.mission = mission;

        List<String> keywords = mission.keywords();
        feedNameLabel.setText(mission.name());
        feedDescriptionLabel.setText(mission.description());
        feedKeywordsLabel.setText(keywords.isEmpty() ? NO_KEYWORDS : "#" + String.join(", #", keywords));
        repopulateChangelogModel(mission);

        // Handle map items being removed
        if (!oldMission.uids().equals(mission.uids()))
        {
            Set<String> oldUids = oldMission.uids().stream().map(MissionAdd.TString::data).collect(Collectors.toSet());
            Set<String> newUids = mission.uids().stream().map(MissionAdd.TString::data).collect(Collectors.toSet());
            Set<String> removedUids = oldUids.stream().filter(u -> !newUids.contains(u)).collect(Collectors.toSet());

            // Remove the map item data entry
            for (String removedUid : removedUids)
            {
                DisplayedMapItem item = displayedMapItems.remove(removedUid);
                if (item != null) item.delete();
            }

            // Sorting will refresh the UI and remove any of the entries that are no longer in the map
            sortMapItems();
        }
    }

    @Override
    public void onDownloadResource(@Nonnull ITakServerConnection connection, @Nonnull Mission mission,
                                   @Nonnull Resource resource, @Nonnull File localPath)
    {
        handleResource(resource, localPath);
    }

    @Override
    public void onDownloadCot(@Nonnull ITakServerConnection connection, @Nonnull Mission mission,
                              @Nonnull MissionAdd<String> cot, @Nonnull File localPath)
    {
        try
        {
            if (localPath.exists() && localPath.length() > 0)
            {
                addCotItem(localPath);
            } else
            {
                addCotManualDownload(cot);
            }
            updateStatus();
        } catch (Exception e)
        {
            logger.error("Failed handling cot '{}' in feed info panel", cot.data(), e);
        }
    }

    @Override
    public void contactAdded(IContact contact)
    {
        // no-op
    }

    @Override
    public void contactRemoved(IContact contact)
    {
        // no-op
    }

    @Override
    public void contactUpdated(IContact contact)
    {
        Mission mission = getCurrentMission(); // Call the getter for test/mock support

        // Repopulating the changelog display when a user referenced by it updates their information (like their callsign)
        if (mission != null && hasContactReference(contact.getUid())) repopulateChangelogModel(mission);
    }

    /**
     * The {@link IContactListener} does not update when our local information changes, so we must
     * track when our {@link PreferenceEntry#CALL_SIGN} changes and refresh the mission if it
     * references our {@link #nodeUuid}.
     * @since 5.3
     */
    private void refreshSelfReferences()
    {
        if (hasContactReference(nodeUuid.toString())) repopulateChangelogModel(mission);
    }

    /**
     * @param contactUid Contact to check for.
     * @return {@code true} when the {@link #mission} contains a reference to the given contact.
     * {@code} false when the user is not referenced in the {@link #mission}.
     * @since 5.3
     */
    private boolean hasContactReference(@Nonnull String contactUid)
    {
        Mission mission = getCurrentMission(); // Call the getter for test/mock support
        if (mission == null) return false;
        final List<MissionChangeOrLogEntry> entries = MissionChangeOrLogEntry.listFromMission(mission);
        return entries.stream().anyMatch(e -> (e.missionChange() != null && e.missionChange().creatorUid().equals(contactUid)) ||
                ((e.logEntry() != null && e.logEntry().creatorUid().equals(contactUid))));
    }

    /**
     * Populates the UI with files and map items already registered in the data sync manager.
     * The rest of the UI will be populated when the appropriate {@link IDataSyncMissionDownloadListener}
     * calls are made.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission to pull data from.
     */
    public void initializeState(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        // Skip if we've already initialized the UI.
        if (feedStatusIconLabel.getIcon() != null) return;

        // If we already have file contents downloaded, populate those in the UI.
        for (MissionAdd.TResource resourceAdd : mission.contents())
        {
            Resource resource = resourceAdd.data();
            File local = dataSyncManager.getLocalDownloadedResource(connection, resource);
            if (local != null && local.isFile())
            {
                handleResource(resource, local);
            }
        }
        for (MissionAdd.TString uid : mission.uids())
        {
            File local = dataSyncManager.getLocalDownloadedCot(connection, uid.data());
            if (local != null && local.isFile())
            {
                try
                {
                    addCotItem(local);
                } catch (IOException ex)
                {
                    logger.error("Failed adding initial CoT '{}' to info panel", uid.data(), ex);
                }
            }
        }

        // Initialize feed info label contents.
        onSubscribedMissionUpdate(connection, mission, mission);
    }

    /**
     * Updates the status icon.
     */
    public void updateStatus()
    {
        String missionName = mission.name();
        boolean isSubscribed = dataSyncManager.isSubscribed(connection, missionName);
        boolean isTracked = dataSyncManager.isTracked(connection, missionName);

        // Update status icon to match sync state.
        MissionDownloadStatus downloadStatus = dataSyncManager.getDownloadState(connection, missionName);
        boolean isConnected = connection.isConnected();
        if (isSubscribed)
        {
            feedStatusIconLabel.setIcon(downloadStatus.isInProgress() ? (isConnected ? SYNC_PROGRESS_ICON : SYNC_DISCONNECTED_ICON) : isTracked ? SYNC_OK_ICON : SYNC_OK_NOT_SUB_ICON);
        } else
        {
            feedStatusIconLabel.setIcon(downloadStatus.isInProgress() ? (isConnected ? SYNC_PROGRESS_ICON : SYNC_DISCONNECTED_ICON) : SYNC_OK_NOT_SUB_ICON);
        }

        // TODO TAKX-2593: Support offline editing of feeds

        // Adding items and editing the feed is only allowed when subscribed.
        // A one-time-download is a read-only snapshot and should not allow editing of any kind.
        boolean isConnectedAndSubscribed = isConnected && isSubscribed && isTracked;
        addItemsMenu.setEnabled(isConnectedAndSubscribed);
        editMenuItem.setEnabled(isConnectedAndSubscribed);
        addItemsMenu.setVisible(isConnectedAndSubscribed);
        editMenuItem.setVisible(isConnectedAndSubscribed);
    }

    private void addCotItem(@Nonnull File localFile) throws IOException
    {
        final String cotContents = Files.readString(localFile.toPath());
        addMapItemEntry(CotEvent.parse(cotContents));
    }

    /**
     * Adds a manual download button to the map item list
     *
     * @param missionAdd The CoT info
     */
    private void addCotManualDownload(@Nonnull MissionAdd<String> missionAdd)
    {
        DisplayedMapItem pendingMapItem = createDownloadCotButton(missionAdd);
        if (pendingMapItem != null)
        {
            displayedMapItems.put(missionAdd.data(), pendingMapItem);
            sortMapItems();
        }
    }

    /**
     * Creates a button that the user can use to manually download a map item and wraps it in a {@link DisplayedMapItem}.
     *
     * @param missionAdd The CoT item info
     * @return A download button wrapped in a {@link DisplayedMapItem}
     */
    @Nullable
    private DisplayedMapItem createDownloadCotButton(@Nonnull MissionAdd<String> missionAdd)
    {
        final UidDetails uidDetails = missionAdd.details();
        if (uidDetails == null)
        {
            // The full uid details are only provided with fully hydrated mission models.
            // You only receive a fully hydrated mission model when requesting an individual mission,
            // and even then the server must have the 'archiving' feature enabled.
            //
            // Without archiving enabled, missions are never fully hydrated.
            // In TAK Server 5.0+ this should be addressed by setting 'alwaysArchiveMissionCot="true"' even when 'archive="false"'
            // It is not something we can really handle well from the client side, so our best bet is to nag users to nag the
            // TAK Server admin who disabled archiving.
            logger.warn("Mission '{}' is missing map-item data, '{}' likely has mission archiving disabled", mission.name(), connection.getUri());
            return null;
        }

        final String callsign = uidDetails.callsign();
        final long timestamp = missionAdd.timestamp().toEpochMilli();
        final String date = timeFormatting.formatDateOnlyWithCurrent(timestamp);

        // Create and wire up the button
        final JButton button = new JButton("<html><b>" + callsign + "</b><br>Created: " + date + "</html>");
        button.setIcon(DOWNLOAD_ICON);
        button.addActionListener(e -> {
            button.setEnabled(false);
            dataSyncManager.manuallyDownloadCot(connection, mission, missionAdd);
        });

        return new DisplayedMapItem(timestamp, callsign, mapItemFlowPanel, button);
    }

    @InvokeOnEDT
    private void handleResource(@Nonnull Resource resource, @Nonnull File resourcePath)
    {
        // Remove old entry if already present.
        DisplayedFile existing = displayedFiles.get(resourcePath);
        if (existing != null) existing.delete();

        // Add the new resource entry to the correct media type panel.
        String extension = FileUtils.getExtension(resourcePath);
        MediaType mediaType = MediaType.getMediaTypeForExtension(extension);

        // An empty resource file indicates that the user must manually download it
        final JComponent resourcePanel = !resourcePath.exists() || resourcePath.length() == 0
                ? previewProvider.getDownloadResourceButton(resource,
                        e -> dataSyncManager.manuallyDownloadResource(connection, mission, resource))
                : previewProvider.previewFile(resource, resourcePath);

        if (resourcePanel != null)
        {
            resourcePanel.setToolTipText(resourcePath.getName());

            JPanel parent;
            switch (mediaType)
            {
                case Image ->
                {
                    parent = photosFlowPanel;
                    photosFlowPanel.add(resourcePanel);
                }
                case Video ->
                {
                    parent = videosFlowPanel;
                    videosFlowPanel.add(resourcePanel);
                }
                default ->
                {
                    parent = filesFlowPanel;
                    filesFlowPanel.add(resourcePanel);
                }
            }
            displayedFiles.put(resourcePath, new DisplayedFile(resourcePath, resource, parent, resourcePanel));
        }

        // Will sort all displayed contents, which will add the latest file as well.
        sortFiles();
    }

    /**
     * Updates the changelog list with the latest values from a given mission.
     *
     * @param mission Mission to pull changelog entries from.
     */
    @InvokeOnEDT
    @VisibleForTesting
    public void repopulateChangelogModel(@Nonnull Mission mission)
    {
        List<MissionChangeOrLogEntry> joined = MissionChangeOrLogEntry.listFromMission(mission);
        changelogModel.clear();
        changelogModel.addAll(joined);
        changelogList.revalidate();
        changelogList.repaint();
    }

    /**
     * Adds a map item entry to the {@link #mapItemFlowPanel}.
     *
     * @param event CoT event representing a map item.
     */
    private void addMapItemEntry(@Nonnull CotEvent event)
    {
        BufferedImage cotImage = cotIconResolver.getIcon(DEFAULT_ICON_WIDTH, event);
        addMapItemEntry(event, cotImage, () -> {
            IMapObject found = mapObjectFinder.findMapObjectByUid(event.getUID());
            if (found != null)
            {
                //noinspection DataFlowIssue
                globeManager.getLastFocusedGlobe().goTo(found.getLocation());
            }
        });
    }

    /**
     * Adds a map item entry to the {@link #mapItemFlowPanel}.
     *
     * @param event CoT event outlining the map item.
     * @param image Image representation of a CoT event type.
     * @param onClick Action to run when clicking the map item entry.
     */
    @InvokeOnEDT
    private void addMapItemEntry(@Nonnull CotEvent event, @Nonnull BufferedImage image, @Nullable Runnable onClick)
    {
        String uid = event.getUID();
        DataFeedMapItemPanel mapItemPanel = new DataFeedMapItemPanel(event, image, onClick);
        DisplayedMapItem newDisplay = new DisplayedMapItem(event.getTime().getMilliseconds(), event.getCallsign(), mapItemFlowPanel, mapItemPanel);
        DisplayedMapItem priorDisplay = displayedMapItems.put(uid, newDisplay);
        if (priorDisplay != null) priorDisplay.delete();

        // Will sort all displayed contents, which will add the latest map item as well.
        sortMapItems();
    }

    /**
     * Sort photos and videos by the current selection type.
     */
    @InvokeOnEDT
    private void sortFiles()
    {
        boolean sortByName = sortNameRadioItem.isSelected();
        photosFlowPanel.removeAll();
        filesFlowPanel.removeAll();
        videosFlowPanel.removeAll();
        displayedFiles.values().stream()
                .sorted(sortByName ? CMP_FILE_NAME : CMP_FILE_TIME)
                .forEach(DisplayedFile::add);
        photosFlowPanel.revalidate();
        photosFlowPanel.repaint();
        photosGroup.revalidate();
        photosGroup.repaint();
        filesFlowPanel.revalidate();
        filesFlowPanel.repaint();
        filesGroup.revalidate();
        filesGroup.repaint();
        videosFlowPanel.revalidate();
        videosFlowPanel.repaint();
        videosGroup.revalidate();
        videosGroup.repaint();
    }

    @InvokeOnEDT
    private void sortMapItems()
    {
        boolean sortByName = sortNameRadioItem.isSelected();
        mapItemFlowPanel.removeAll();
        displayedMapItems.values().stream()
                .sorted(sortByName ? CMP_COT_NAME : CMP_COT_TIME)
                .forEach(DisplayedMapItem::add);
        mapItemFlowPanel.revalidate();
        mapItemFlowPanel.repaint();
        mapItemsGroup.revalidate();
        mapItemsGroup.repaint();
    }

    private void openFeedEditor()
    {
        Window window = SwingUtilities.windowForComponent(this);
        DataFeedEditorDialog dialog = DataFeedEditorDialog.forEditing(window, connection, mission);
        dialog.setVisible(true);
    }

    private void addItemFromFileSelect()
    {
        File[] selectedFiles = showMultiFileChooser(c -> c.setFileFilter(null));
        addFilesToSync(selectedFiles);
    }

    private void addItemFromImageSelection()
    {
        File[] selectedFiles = showMultiFileChooser(c -> c.setFileFilter(new ImageFileChooserFilter()));
        addFilesToSync(selectedFiles);
    }

    private void addFilesToSync(@Nullable File[] selectedFiles)
    {
        if (selectedFiles != null)
        {
            for (File selectedFile : selectedFiles)
            {
                dataSyncManager.addFile(connection, mission.name(), selectedFile)
                        .whenComplete((result, error) -> {
                            // Only operate on error.
                            // The result is handled via listener logic.
                            if (error != null) logger.error("Failed attaching file '{}' to mission '{}'", selectedFile.getName(), mission.name(), error);
                        });
            }
        }
    }

    private void addItemFromLassoSelection()
    {
        multiSelector.arm(ShapeType.FREEHAND_POLYGON, selected -> {
            try
            {
                for (IMapObject mapObject : selected)
                {
                    addMapObject(mapObject);
                }
            } finally
            {
                multiSelector.disarmAndClear();
            }
        });
    }

    private void addItemFromSingleMapSelection()
    {
        new CancelableMapObjectSelectionListener(list -> {
            if (list != null) addMapObjects(list);
        }).register();
    }

    private void addItemFromNewUserLog()
    {
        new DataFeedUserLogDialog(connection, mission.name())
                .setVisible(true);
    }

    private void exportAsFeedInviteCot()
    {
        String missionName = mission.name();
        File file = showSingleFileChooser(c -> c.setFileFilter(new SingleTypeFileFilter("cot")));
        if (file != null)
        {
            if (!FileUtils.hasExtension(file, "cot")) file = new File(file.getAbsolutePath() + ".cot");
            try
            {
                Files.writeString(file.toPath(), exportMissionInvite());
            } catch (IOException ex)
            {
                logger.error("Failed writing mission invite for '{}'", missionName, ex);
                AlwaysOnTopJOptionPane.showErrorDialog(
                        "There was an error writing the mission invite file for '" + missionName + "'",
                        "Failed exporting invite");
            }
        }
    }

    private void exportAsFeedLog()
    {
        File file = showSingleFileChooser(c -> c.setFileFilter(new SingleTypeFileFilter("csv")));
        if (file == null) return;
        if (!FileUtils.hasExtension(file, "csv")) file = new File(file.getAbsolutePath() + ".csv");

        try
        {
            Files.writeString(file.toPath(), exportChangelogCsv());
        } catch (IOException ex)
        {
            logger.error("Failed writing mission log for '{}'", mission.name(), ex);
            AlwaysOnTopJOptionPane.showErrorDialog(
                    "There was an error writing the mission log for '" + mission.name() + "'",
                    "Failed exporting log");
        }
    }

    private void exportAsDataPackage()
    {
        File file = showSingleFileChooser(c -> c.setFileFilter(new SingleTypeFileFilter("zip")));
        if (file == null) return;
        if (!FileUtils.hasExtension(file, "zip")) file = new File(file.getAbsolutePath() + ".zip");

        try
        {
            File missionPackage = dataPackageManager.createNewMissionPackage(connection, mission);

            // Add changelog
            File tempCsv = File.createTempFile("mission", "changelog");
            dataPackageManager.addFileToDataPackage(missionPackage, tempCsv);
            //noinspection ResultOfMethodCallIgnored
            tempCsv.delete();

            // Add invite CoT
            File tempInvite = File.createTempFile("mission", "invite");
            dataPackageManager.addFileToDataPackage(missionPackage, tempInvite);
            //noinspection ResultOfMethodCallIgnored
            tempInvite.delete();

            // Add associated resources
            List<File> files = mission.contents().stream()
                    .map(r -> dataSyncManager.getLocalDownloadedResource(connection, r.data()))
                    .filter(Objects::nonNull)
                    .toList();
            dataPackageManager.addFilesToDataPackage(missionPackage, files);

            // Add associated map items
            List<IMapObject> mapObjects = mission.uids().stream()
                    .map(MissionAdd.TString::data)
                    .map(mapObjectFinder::findMapObjectByUid)
                    .filter(Objects::nonNull)
                    .toList();
            dataPackageManager.addMapObjectsToDataPackage(missionPackage, mapObjects);

            // Copy to destination
            Files.copy(missionPackage.toPath(), file.toPath());
        } catch (IOException ex)
        {
            logger.error("Failed writing mission package for '{}'", mission.name(), ex);
            AlwaysOnTopJOptionPane.showErrorDialog(
                    "There was an error writing the mission package for '" + mission.name() + "'",
                    "Failed exporting mission package");
        }
    }

    /**
     * Called when {@link #sortModifiedTimeRadioItem} is selected.
     * <br>
     * Resorts file contents.
     */
    private void sortModifiedTimeRadioItem()
    {
        sortNameRadioItem.setSelected(false);
        sortModifiedTimeRadioItem.setSelected(true);
        sortFiles();
        sortMapItems();
    }

    /**
     * Called when {@link #sortNameRadioItem} is selected.
     * <br>
     * Resorts file contents.
     */
    private void sortNameRadioItem()
    {
        sortNameRadioItem.setSelected(true);
        sortModifiedTimeRadioItem.setSelected(false);
        sortFiles();
        sortMapItems();
    }

    /**
     * Adds the given map objects to the mission as CoT events.
     *
     * @param mapObjects Map objects to add to the current mission.
     */
    private void addMapObjects(@Nonnull Collection<IMapObject> mapObjects)
    {
        for (IMapObject mapObject : mapObjects)
        {
            addMapObject(mapObject);
        }
    }

    /**
     * Adds a map object to upload to the mission as a CoT event.
     *
     * @param mapObject Map object to add to the current mission.
     */
    private void addMapObject(@Nonnull IMapObject mapObject)
    {
        String missionName = mission.name();
        dataSyncManager.addMapItem(connection, missionName, mapObject)
                .whenComplete((result, error) -> {
                    // Extract bundled API exception if present.
                    if (error == null && result != null) error = result.error();

                    // Only operate on error.
                    // The result is handled via listener logic.
                    if (error != null)
                    {
                        logger.error("Failed attaching map item '{}' to mission '{}'", mapObject.getName(), missionName, error);

                        if (error instanceof ApiException apiException && apiException.getMessage() != null)
                        {
                            AlwaysOnTopJOptionPane.showErrorDialog(apiException.getMessage(), "Map Item Upload Error");
                        } else
                        {
                            AlwaysOnTopJOptionPane.showErrorDialog(
                                    "Uncaught exception adding map item to mission '" + missionName + "'",
                                    "Map Item Upload Error");
                        }
                    }
                });
    }

    /**
     * @param extraConfig Consumer to apply additional configuration to the chooser before prompting for files.
     *
     * @return Selected files, if any.
     */
    @Nullable
    private File[] showMultiFileChooser(@Nonnull Consumer<JFileChooser> extraConfig)
    {
        JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        extraConfig.accept(fileChooser);
        fileChooser.showDialog(this, "Select");
        return fileChooser.getSelectedFiles();
    }

    /**
     * @return Selected file, if any.
     */
    @Nullable
    private File showSingleFileChooser(@Nonnull Consumer<JFileChooser> extraConfig)
    {
        JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        extraConfig.accept(fileChooser);
        fileChooser.showDialog(this, "Select");
        return fileChooser.getSelectedFile();
    }

    /**
     * Displays currently invited users, plus the option to invite others.
     */
    private void showInvites()
    {
        new DataFeedInviteDialog(connection, mission.name())
                .setVisible(true);
    }

    /**
     * Displays the data feed's timeline.
     */
    private void showTimeline()
    {
        panelController.openMissionTimeline(connection, mission);
    }

    @Nonnull
    private String exportMissionInvite()
    {
        // TODO TAKX-2257: Move export logic to its own service
        CotEvent event = dataSyncManager.createInviteCot(connection, mission.name());
        if (event == null) throw new IllegalStateException("Failed to export mission invite to CoT");
        return event.toString();
    }

    @Nonnull
    private String exportChangelogCsv()
    {
        // TODO TAKX-2257: Move export logic to its own service
        StringBuilder sb = new StringBuilder("Category,DTG,Log,User,Latitude,Longitude\n");
        String entryFormat = "%s,%s,%s,%s,%s,%s\n";

        // Mapping to a list for use with the ChangeInfoResolver
        List<MissionChangeOrLogEntry> entries = Collections.list(changelogModel.elements());
        for (int i = 0; i < changelogModel.size(); i++)
        {
            MissionChangeOrLogEntry entry = entries.get(i);

            LogEntry log = entry.logEntry();
            MissionChange change = entry.missionChange();
            if (log != null)
            {
                String creatorUid = log.creatorUid();
                String creator = contactNameResolver.resolveName(creatorUid);
                String title = log.content().replace(',', '.');

                sb.append(entryFormat.formatted("User Log", log.dtg(), title, creator, "", ""));
            } else if (change != null)
            {
                String creatorUid = change.creatorUid();
                String creator = contactNameResolver.resolveName(creatorUid);
                String title = changeInfoResolver.getChangeMessage(entries, entry, i).replace(',', '.');

                String latitude = "";
                String longitude = "";
                if (change.details() != null)
                {
                    UidDetails details = change.details();
                    Location location = details.location();
                    latitude = String.valueOf(location.lat());
                    longitude = String.valueOf(location.lon());
                }

                sb.append(entryFormat.formatted("Mission Log", change.timestamp(), title, creator, latitude, longitude));
            }
        }
        return sb.toString();
    }

    /**
     * Renderer for {@link MissionChange} and {@link LogEntry} items in the {@link #changelogList}.
     */
    private class MissionChangelogCellRenderer extends DefaultListCellRenderer
    {
        private final FontMetrics fontMetrics = getFontMetrics(getFont());
        private final int ellipsisWidth = fontMetrics.stringWidth("…");

        @Override
        public Component getListCellRendererComponent(JList list, Object objValue, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, objValue, index, isSelected, cellHasFocus);

            // The value will always be this type.
            MissionChangeOrLogEntry value = (MissionChangeOrLogEntry) objValue;

            if (value.missionChange() != null)
            {
                String text = changeInfoResolver.getChangeMessage(MissionChangeOrLogEntry.listFromMission(mission), value, index);

                // The basic <html> text-wrapping trick does not work here...
                // We want to clip the text length, so it doesn't force the rest of the UI to create a horizontal scroll bar.
                boolean clipped = false;
                int width = Math.max(300, list.getWidth() - 30);
                while (fontMetrics.stringWidth(text) + ellipsisWidth > width)
                {
                    text = text.substring(0, text.length() - 1);
                    clipped = true;
                }
                if (clipped) text += "…";

                ImageIcon icon = changeInfoResolver.getChangeIcon(value);
                setIcon(icon);
                setText(text);
            } else if (value.logEntry() != null)
            {
                renderUserLog(value.logEntry());
            }

            return this;
        }

        private void renderUserLog(@Nonnull LogEntry log)
        {
            String creatorUid = log.creatorUid();
            String creator = contactNameResolver.resolveName(creatorUid);

            setText(creator + " - " + log.content());
            setIcon(entryInfoResolver.getChangeIcon(log));
        }
    }

    /**
     * Wrapper for a displayed map item.
     *
     * @param time The map item timestamp
     * @param callsign The map item callsign; may be {@code null} if the callsign could not be resolved
     * @param parent Parent panel that holds the displayed content
     * @param content The panel displaying the map item.
     */
    private record DisplayedMapItem(long time, @Nullable String callsign, @Nonnull JPanel parent, @Nonnull JComponent content)
    {
        private void add()
        {
            parent.add(content);
        }

        private void delete()
        {
            parent.remove(content);
        }
    }

    /**
     * Wrapper for a displayed file or manual download button.
     *
     * @param resourcePath Path to displayed file.
     * @param resource Resource displayed.
     * @param parent Parent panel that holds the displayed content.
     * @param content The panel displaying the file or the manual download button
     */
    private record DisplayedFile(@Nonnull File resourcePath, @Nonnull Resource resource,
            @Nonnull JPanel parent, @Nonnull JComponent content)
    {
        private void add()
        {
            parent.add(content);
        }

        private void delete()
        {
            parent.remove(content);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var headerPanel = new JPanel();
        feedStatusIconLabel = new JLabel();
        feedNameLabel = new JLabel();
        invitesButton = new JButton();
        timelineButton = new JButton();
        feedDescriptionLabel = new JLabel();
        feedKeywordsLabel = new JLabel();
        var menuBar = new JMenuBar();
        addItemsMenu = new JMenu();
        var addFileSelectItem = new JMenuItem();
        var addImagesItem = new JMenuItem();
        var addLassoItem = new JMenuItem();
        var addMapSelectItem = new JMenuItem();
        var addNewUserLog = new JMenuItem();
        var exportMenu = new JMenu();
        var exportFeedInviteCotItem = new JMenuItem();
        var exportFeedLogItem = new JMenuItem();
        var exportFeedAsDataPackageItem = new JMenuItem();
        var sortMenu = new JMenu();
        sortModifiedTimeRadioItem = new JRadioButtonMenuItem();
        sortNameRadioItem = new JRadioButtonMenuItem();
        editMenuItem = new JMenuItem();
        groupsScrollWrapper = new JScrollPane();
        var groupsWrapper = new JPanel();
        mapItemsGroup = new JXTaskPane();
        photosGroup = new JXTaskPane();
        filesGroup = new JXTaskPane();
        videosGroup = new JXTaskPane();
        logsGroup = new JXTaskPane();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(450, 400));
        setBorder(Borders.createEmptyBorder("4dlu, 4dlu, 4dlu, 4dlu"));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap), fill:default:grow"));

        //======== headerPanel ========
        {
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, 2*($lcgap, default)",
                    "2*(default, $lgap), default"));

            //---- feedStatusIconLabel ----
            feedStatusIconLabel.setName("feedStatusIconLabel");
            headerPanel.add(feedStatusIconLabel, cc.xywh(1, 1, 1, 3));

            //---- feedNameLabel ----
            feedNameLabel.setText("Feed Name");
            feedNameLabel.setFont(feedNameLabel.getFont().deriveFont(feedNameLabel.getFont().getStyle() | Font.BOLD, feedNameLabel.getFont().getSize() + 2f));
            feedNameLabel.setName("feedNameLabel");
            headerPanel.add(feedNameLabel, cc.xy(3, 1));

            //---- invitesButton ----
            invitesButton.setBorderPainted(false);
            invitesButton.setToolTipText("Invite TAK contacts");
            invitesButton.setName("invitesButton");
            invitesButton.addActionListener(e -> showInvites());
            headerPanel.add(invitesButton, cc.xywh(5, 1, 1, 3, CellConstraints.DEFAULT, CellConstraints.CENTER));

            //---- timelineButton ----
            timelineButton.setBorderPainted(false);
            timelineButton.setToolTipText("Show feed timeline");
            timelineButton.setName("timelineButton");
            timelineButton.addActionListener(e -> showTimeline());
            headerPanel.add(timelineButton, cc.xywh(7, 1, 1, 3, CellConstraints.DEFAULT, CellConstraints.CENTER));

            //---- feedDescriptionLabel ----
            feedDescriptionLabel.setText("Feed Description");
            feedDescriptionLabel.setName("feedDescriptionLabel");
            headerPanel.add(feedDescriptionLabel, cc.xy(3, 3));

            //---- feedKeywordsLabel ----
            feedKeywordsLabel.setText("(No keywords)");
            feedKeywordsLabel.setForeground(new Color(0x666666));
            feedKeywordsLabel.setFont(feedKeywordsLabel.getFont().deriveFont(feedKeywordsLabel.getFont().getStyle() | Font.ITALIC));
            feedKeywordsLabel.setName("feedKeywordsLabel");
            headerPanel.add(feedKeywordsLabel, new CellConstraints(1, 5, 3, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(0, 7, 0, 7)));
        }
        add(headerPanel, cc.xy(1, 1));

        //======== menuBar ========
        {
            menuBar.setName("menuBar");

            //======== addItemsMenu ========
            {
                addItemsMenu.setText("Add Items");
                addItemsMenu.setName("addItemsMenu");

                //---- addFileSelectItem ----
                addFileSelectItem.setText("File Select");
                addFileSelectItem.setName("addFileSelectItem");
                addFileSelectItem.addActionListener(e -> addItemFromFileSelect());
                addItemsMenu.add(addFileSelectItem);

                //---- addImagesItem ----
                addImagesItem.setText("Images");
                addImagesItem.setName("addImagesItem");
                addImagesItem.addActionListener(e -> addItemFromImageSelection());
                addItemsMenu.add(addImagesItem);

                //---- addLassoItem ----
                addLassoItem.setText("Lasso");
                addLassoItem.setName("addLassoItem");
                addLassoItem.addActionListener(e -> addItemFromLassoSelection());
                addItemsMenu.add(addLassoItem);

                //---- addMapSelectItem ----
                addMapSelectItem.setText("Map Select");
                addMapSelectItem.setName("addMapSelectItem");
                addMapSelectItem.addActionListener(e -> addItemFromSingleMapSelection());
                addItemsMenu.add(addMapSelectItem);

                //---- addNewUserLog ----
                addNewUserLog.setText("User Log");
                addNewUserLog.setName("addNewUserLog");
                addNewUserLog.addActionListener(e -> addItemFromNewUserLog());
                addItemsMenu.add(addNewUserLog);
            }
            menuBar.add(addItemsMenu);

            //======== exportMenu ========
            {
                exportMenu.setText("Export");
                exportMenu.setName("exportMenu");

                //---- exportFeedInviteCotItem ----
                exportFeedInviteCotItem.setText("Feed Invite CoT");
                exportFeedInviteCotItem.setName("exportFeedInviteCotItem");
                exportFeedInviteCotItem.addActionListener(e -> exportAsFeedInviteCot());
                exportMenu.add(exportFeedInviteCotItem);

                //---- exportFeedLogItem ----
                exportFeedLogItem.setText("Feed Log");
                exportFeedLogItem.setName("exportFeedLogItem");
                exportFeedLogItem.addActionListener(e -> exportAsFeedLog());
                exportMenu.add(exportFeedLogItem);

                //---- exportFeedAsDataPackageItem ----
                exportFeedAsDataPackageItem.setText("Data Package");
                exportFeedAsDataPackageItem.setName("exportFeedAsDataPackageItem");
                exportFeedAsDataPackageItem.addActionListener(e -> exportAsDataPackage());
                exportMenu.add(exportFeedAsDataPackageItem);
            }
            menuBar.add(exportMenu);

            //======== sortMenu ========
            {
                sortMenu.setText("Item Sort");
                sortMenu.setName("sortMenu");

                //---- sortModifiedTimeRadioItem ----
                sortModifiedTimeRadioItem.setText("Modified Time");
                sortModifiedTimeRadioItem.setSelected(true);
                sortModifiedTimeRadioItem.setName("sortModifiedTimeRadioItem");
                sortModifiedTimeRadioItem.addActionListener(e -> sortModifiedTimeRadioItem());
                sortMenu.add(sortModifiedTimeRadioItem);

                //---- sortNameRadioItem ----
                sortNameRadioItem.setText("Name");
                sortNameRadioItem.setName("sortNameRadioItem");
                sortNameRadioItem.addActionListener(e -> sortNameRadioItem());
                sortMenu.add(sortNameRadioItem);
            }
            menuBar.add(sortMenu);

            //---- editMenuItem ----
            editMenuItem.setText("Edit Properties");
            editMenuItem.setName("editMenuItem");
            editMenuItem.addActionListener(e -> openFeedEditor());
            menuBar.add(editMenuItem);
        }
        add(menuBar, cc.xy(1, 3));

        //======== groupsScrollWrapper ========
        {
            groupsScrollWrapper.setName("groupsScrollWrapper");

            //======== groupsWrapper ========
            {
                groupsWrapper.setName("groupsWrapper");
                groupsWrapper.setLayout(new FormLayout(
                        "default:grow",
                        "5*(fill:default, $lgap), fill:default:grow"));

                //---- mapItemsGroup ----
                mapItemsGroup.setTitle("Map Items");
                mapItemsGroup.setName("mapItemsGroup");
                groupsWrapper.add(mapItemsGroup, cc.xy(1, 1));

                //---- photosGroup ----
                photosGroup.setTitle("Photos");
                photosGroup.setName("photosGroup");
                groupsWrapper.add(photosGroup, cc.xy(1, 3));

                //---- filesGroup ----
                filesGroup.setTitle("Files");
                filesGroup.setName("filesGroup");
                groupsWrapper.add(filesGroup, cc.xy(1, 5));

                //---- videosGroup ----
                videosGroup.setTitle("Videos");
                videosGroup.setName("videosGroup");
                groupsWrapper.add(videosGroup, cc.xy(1, 7));

                //---- logsGroup ----
                logsGroup.setTitle("Logs");
                logsGroup.setName("logsGroup");
                groupsWrapper.add(logsGroup, cc.xy(1, 9));
            }
            groupsScrollWrapper.setViewportView(groupsWrapper);
        }
        add(groupsScrollWrapper, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel feedStatusIconLabel;
    private JLabel feedNameLabel;
    private JButton invitesButton;
    private JButton timelineButton;
    private JLabel feedDescriptionLabel;
    private JLabel feedKeywordsLabel;
    private JMenu addItemsMenu;
    private JRadioButtonMenuItem sortModifiedTimeRadioItem;
    private JRadioButtonMenuItem sortNameRadioItem;
    private JMenuItem editMenuItem;
    private JScrollPane groupsScrollWrapper;
    private JXTaskPane mapItemsGroup;
    private JXTaskPane photosGroup;
    private JXTaskPane filesGroup;
    private JXTaskPane videosGroup;
    private JXTaskPane logsGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
