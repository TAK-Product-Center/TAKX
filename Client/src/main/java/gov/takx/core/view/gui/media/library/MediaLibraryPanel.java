package gov.takx.core.view.gui.media.library;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.VideoEndpoints;
import gov.tak.api.comms.takserver.mission.data.Feed;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.video.ConnectionEntry;
import gov.tak.api.video.cot.ConnectionEntryDetail;
import gov.tak.api.video.cot.VideoDetailHandler;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.event.takserver.TakServerConnectionEvent;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.MediaFormats;
import gov.takx.api.media.MediaManagerListenerAdapter;
import gov.takx.api.media.VideoBuilder;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.notification.Notification.Type;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.core.mapobject.gui.SendAsCotDialog;
import gov.takx.core.media.VideoConnectionManager;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.media.UrlDialog;
import gov.takx.core.view.gui.media.library.table.ColorChooserEditor;
import gov.takx.core.view.gui.media.library.table.MediaLibraryJXTable;
import gov.takx.core.view.gui.media.library.table.MediaLibraryTableColumnNames;
import gov.takx.core.view.gui.media.library.table.MediaLibraryTableModel;
import gov.takx.core.view.gui.media.library.table.MediaLibraryTableObject;
import gov.takx.core.view.gui.takserver.UiUtilities;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Media library and audio playback panel
 * <p>
 * The panel allows users to add or remove audio, video, and image to/from the library.  Information about the media is
 * stored in a table contained in the left portion of the split pane.   The table also allows users to add the media
 * to the timeline and determine the color of the associated device.
 * <p>
 * The right pane displays a waveform of a loaded audio file or stream.  Standard audio playback features are
 * available, including play, pause, stop, and skip forward and back.  Additionally, the user can specify a section of
 * the audio to listen to repeatedly.
 *
 * @since 2.37
 */
@PseudoBean
public class MediaLibraryPanel extends ATitledPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String VFS_NAMESPACE = "MediaManager";
    private static final String VFS_STATE_FILENAME = "MediaLibraryPanelState.json";

    private static final String USER = "user";

    private static final ImageIcon OPEN_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "open.png");

    private static final String PROGRESS_CARD = "progressCard";
    private static final String SELECTION_CARD = "selectionCard";
    private static final String LIBRARY_CARD = "libraryCard";

    @Inject
    private IMediaManager mediaManager;

    @Inject
    private IMediaPlayerManager mediaPlayerManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private ITakServerConnectionManager takServerManager;

    @Inject
    private TakServerApiClient takServerApiClient;

    @Inject
    private VideoConnectionManager videoConnectionManager;

    @Inject
    private INotificationService notificationService;

    private boolean restored;
    private final CardLayout cardLayout;
    private VideoSelectionPanel videoSelectionPanel;
    private final ChooseMediaTypePopupMenu addMediaPopupMenu = new ChooseMediaTypePopupMenu();
    private final MediaLibraryTableModel libraryTableModel = new MediaLibraryTableModel();
    private final TableModelListener tableModelListener;

    private final MediaManagerListener mediaManagerListener = new MediaManagerListener();

    private final List<ITakServerConnection> pendingQueriedConnections = new CopyOnWriteArrayList<>();
    private final List<ITakServerConnection> erroredQueriedConnections = new CopyOnWriteArrayList<>();

    private final LibraryTableMouseListener libraryTableMouseListener = new LibraryTableMouseListener();

    public MediaLibraryPanel()
    {
        super(ButtonType.MEDIA.getDisplayText());

        initComponents();

        cardLayout = (CardLayout) contentPanel.getLayout();

        tableModelListener = e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == MediaLibraryTableColumnNames.Color.ordinal())
            {
                final int firstRow = libraryTable.getRowSorter().convertRowIndexToModel(e.getFirstRow());
                final Color color = (Color) libraryTableModel.getValueAt(firstRow, e.getColumn());
                final IMedia media = libraryTableModel.getRowObject(firstRow).getMedia();
                media.setColor(color);
            }
        };

        configureLibraryTable();

        showLibrary();
    }

    private void configureLibraryTable()
    {
        libraryAddButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        libraryRemoveButton.setIcon(UIResources.REMOVE_ACTION_BUTTON_ICON);
        libraryOpenButton.setIcon(OPEN_ICON);
        libraryEditButton.setIcon(UIResources.EDIT_ACTION_BUTTON_ICON);
        sendAsCotButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "send.png"));
        libraryTable.setModel(libraryTableModel);

        TableColumnModelExt tcm = (TableColumnModelExt) libraryTable.getColumnModel();

        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.Type.toString()), 60, true);
        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.Name.toString()), 100, true);
        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.PathURL.toString()), 200, false);
        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.CreationTime.toString()), 165, false);
        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.DurationSize.toString()), 90, false);
        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.Timeline.toString()), 60, false);
        setColumnDefaults(tcm.getColumnExt(MediaLibraryTableColumnNames.Color.toString()), 40, false);

        TableCellEditor tableCellEditor = new ColorChooserEditor(this);
        TableColumn tableColumn = tcm.getColumnExt(MediaLibraryTableColumnNames.Color.toString());
        tableColumn.setCellEditor(tableCellEditor);

        ListSelectionModel cellSelectionModel = libraryTable.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellSelectionModel.addListSelectionListener(this::tableSelectionChanged);

        libraryTableModel.addTableModelListener(tableModelListener);

        // TODO: implement the timeline checkbox
        libraryTable.removeColumn(tcm.getColumnExt(MediaLibraryTableColumnNames.Timeline.toString()));
    }

    private void setColumnDefaults(TableColumnExt ce, int width, boolean visible)
    {
        ce.setMinWidth(width);
        ce.setPreferredWidth(width);
        ce.setVisible(visible);
        ce.setIdentifier(ce.getTitle());
    }

    @PostConstruct
    private void postConstruct()
    {
        onTakServerConnectionEvent(null); // Get our initial button state set

        mediaManager.addMediaManagerListener(mediaManagerListener);
        libraryTable.addMouseListener(libraryTableMouseListener);
    }

    public String getDescription()
    {
        return "<HTML>All available media in the current project is displayed below, grouped by audio, video, and imagery.</HTML>";
    }

    private void showLibrary()
    {
        cardLayout.show(contentPanel, LIBRARY_CARD);
    }

    private void showProgress()
    {
        cardLayout.show(contentPanel, PROGRESS_CARD);
    }

    private void showSelection()
    {
        cardLayout.show(contentPanel, SELECTION_CARD);
        videoSelectionPanel.takeFocus();
    }

    /**
     * On any connection event, update the downloadButton.  Enable it if we have any connected connections,
     * otherwise, disable it.
     *
     * @since 5.0
     */
    @InvokeOnEDT
    private void onTakServerConnectionEvent(@Observes TakServerConnectionEvent ignore)
    {
        downloadWrapper.removeAll();

        final JButton downloadButton = UiUtilities.buildDownloadButton(takServerManager, this::queryFeeds,
                "Download Video list from TAK Server");
        downloadWrapper.add(BorderLayout.CENTER, downloadButton);
    }

    /**
     * Kick off a query for available video feeds.
     *
     * @param connection Connection to query for videos, or {@code null} to query from all available servers.
     * @since 5.0
     */
    private void queryFeeds(@Nullable ITakServerConnection connection)
    {
        if (videoSelectionPanel != null) selectionPanel.remove(videoSelectionPanel);
        videoSelectionPanel = new VideoSelectionPanel(this::addFeedsToLibrary, this::showLibrary);
        selectionPanel.add(videoSelectionPanel, BorderLayout.CENTER);

        showProgress();

        populateVideoSelectionList(connection);
    }

    private void populateVideoSelectionList(@Nullable ITakServerConnection connection)
    {
        pendingQueriedConnections.clear();

        final Collection<? extends ITakServerConnection> connections = connection == null
                ? takServerManager.getTakServerConnections(true)
                : List.of(connection);

        connections.forEach(conn -> {
            final VideoEndpoints videoEndpoints = takServerApiClient.videoEndpoints(conn);

            pendingQueriedConnections.add(conn);

            videoEndpoints.getVideoCollections().asyncGet()
                    .whenComplete(((videoConnections, throwable) -> {
                        if (throwable != null)
                        {
                            logger.warn("Error querying {}", conn, throwable);
                            erroredQueriedConnections.add(conn);
                        } else
                        {
                            pendingQueriedConnections.remove(conn);
                            videoConnections.videoConnections().forEach(vc -> videoSelectionPanel.addFeeds(vc.feeds()));
                        }
                        checkQueryFeedback();
                    }));
        });
    }

    /**
     * Check if there are no more pending connection queries, and if so show the selection panel.
     *
     * @since 5.0
     */
    @InvokeOnEDT
    private void checkQueryFeedback()
    {
        // Show the selection card once all queried connections have gotten data back.
        // If there are no videos to show, jump straight back to the library panel.
        if (pendingQueriedConnections.isEmpty())
        {
            if (videoSelectionPanel.hasFeeds())
            {
                showSelection();
            } else
            {
                String message = "The requested servers had no videos to offer.";
                if (!erroredQueriedConnections.isEmpty())
                {
                    message += "\n\nThe following connections encountered errors when querying for video feeds:\n" +
                            erroredQueriedConnections.stream()
                                    .map(ITakServerConnection::getDisplayName)
                                    .collect(Collectors.joining("\n - "));
                }
                AlwaysOnTopJOptionPane.showErrorDialogLater(message, "No Videos found");
                showLibrary();
            }
        }
    }

    private void addFeedsToLibrary(List<Feed> feeds)
    {
        for (Feed feed : feeds)
        {
            final String name = mediaManager.createUniqueName(feed.alias());

            final URI feedUri;
            try
            {
                feedUri = new URI(feed.url());
            } catch (URISyntaxException e)
            {
                logger.warn("Invalid video URL; will not add video feed: {} for {}", feed.url(), feed.alias());
                continue;
            }

            final VideoBuilder builder = new VideoBuilder(name, "TAKServer", feedUri);

            try
            {
                builder.setThumbnailURL(feed.thumbnail());
            } catch (IllegalArgumentException e)
            {
                logger.warn("Invalid thumbnail URL; ignoring: {} for {}", feed.thumbnail(), feed.alias());
            }

            // Construct a connection entry from the feed info.
            // Default values mirror the kernel, but there are no constants defined there.
            final ConnectionEntry entry = new ConnectionEntry(feed.alias(), feedUri.toString());
            entry.setUID(feed.uuid());
            entry.setNetworkTimeout(ParseUtils.parseInt(feed.networkTimeout(), 5_000));
            entry.setBufferTime(ParseUtils.parseInt(feed.bufferTime(), -1));
            entry.setRoverPort(ParseUtils.parseInt(feed.roverPort(), -1));
            entry.setIgnoreEmbeddedKLV(Boolean.parseBoolean(feed.ignoreEmbeddedKLV()));
            builder.setConnectionEntry(entry);

            mediaManager.addOrReplaceMedia(builder.build());
        }

        showLibrary();
    }

    private void libraryAddButtonActionPerformed()
    {
        if (addMediaPopupMenu.isVisible())
        {
            addMediaPopupMenu.setVisible(false);
        } else
        {
            // The popup menu is shown at the position x, y in the coordinate space of the invoker, so we only need to
            // offset by the button height to get it to display immediately below the button.
            addMediaPopupMenu.show(libraryAddButton, 0, libraryAddButton.getHeight());
        }
    }

    /** @since 5.2 */
    private void libraryEditButtonActionPerformed()
    {
        MediaLibraryTableObject selectedRowObject = getSelectedRowObject();
        if (selectedRowObject != null)
        {
            if (selectedRowObject.getMedia().getLocalFilePath() == null)
            {
                UrlDialog urlDialog = new UrlDialog(mediaManager, media -> updateMedia(media, selectedRowObject.getMedia()), true);
                urlDialog.setName(selectedRowObject.getName());
                urlDialog.setUrl(selectedRowObject.getMedia().getSourceUri().toString());
                urlDialog.setVisible(true);
            } else
            {
                JFileChooser fileChooser = createFileChooser(new File(selectedRowObject.getMedia().getSourceUri()));
                int returnVal = fileChooser.showOpenDialog(JFrame.getFrames()[0]);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File selectedFile = fileChooser.getSelectedFile();
                    try
                    {
                        IMedia newMedia = mediaManager.createMediaFromLocalFile(selectedFile, USER);
                        updateMedia(newMedia, selectedRowObject.getMedia());
                    } catch (IllegalArgumentException e)
                    {
                        showMediaErrorMessage(selectedFile, e);
                    }
                } else
                {
                    logger.debug("Open command cancelled by user");
                }
            }
        }
    }

    /**
     * Update the table entry for a given media.
     *
     * @param newMedia New media to reference
     * @param oldMedia Old media to update with the new media
     * @since 5.2
     */
    // TODO: Add tests, TAKX-2751
    @VisibleForTesting
    void updateMedia(@Nonnull IMedia newMedia, @Nonnull IMedia oldMedia)
    {
        mediaManager.removeMedia(oldMedia);
        mediaManager.addMedia(newMedia);
    }

    private void libraryRemoveButtonActionPerformed()
    {
        MediaLibraryTableObject selectedRowObject = getSelectedRowObject();
        if (selectedRowObject != null)
        {
            IMedia mediaToRemove = selectedRowObject.getMedia();
            mediaManager.removeMedia(mediaToRemove);
            libraryTableModel.refresh();
        }
    }

    private void sendAsCotButtonClicked()
    {
        final var selectedRowObject = getSelectedRowObject();
        if (selectedRowObject != null)
        {
            final var media = selectedRowObject.getMedia();
            final var connectionEntry = videoConnectionManager.getConnectionEntry(media);
            if (connectionEntry == null)
            {
                notificationService.show(Notification.builder("Missing connection data for Media: " + media.getName(), Type.Warning).build());
                return;
            }

            new SendAsCotDialog(connectionEntry.getAlias(),
                    () -> {
                        CoordinatedTime now = new CoordinatedTime();
                        CotEvent cotEvent = new CotEvent(connectionEntry.getUID(), CotTypes.VIDEO, CotEvent.VERSION_2_0, GeoPoint.ZERO_POINT,
                                now, now, now, CotConstants.HOW_MACHINE_GENERATED_PREFIX, null, null, null, null);
                        CotDetail cotDetail = new CotDetail();
                        CotDetail videoDetail = new CotDetail(VideoDetailHandler.DETAIL);
                        videoDetail.addChild(ConnectionEntryDetail.toCotDetail(connectionEntry));
                        cotDetail.addChild(videoDetail);
                        cotDetail.addChild(new CotDetail(DetailConstants.CONTACT, DetailConstants.ATTR_CALLSIGN, connectionEntry.getAlias()));
                        cotEvent.setDetail(cotDetail);
                        return Collections.singletonList(cotEvent);
                    },
                    () -> notificationService.show(Notification.builder("Failed to generate CoT for Media: " + media.getName(), Type.Warning).build()))
                    .setVisible(true);
        }
    }

    /**
     * Gets the selected media item and creates a media player with it.
     *
     * @since 5.1
     */
    private void createMediaPlayerForSelectedObject()
    {
        MediaLibraryTableObject selectedRowObject = getSelectedRowObject();
        if (selectedRowObject != null)
        {
            IMedia media = selectedRowObject.getMedia();
            mediaPlayerManager.createDefaultPlayer(media);
        }
    }

    /**
     * @return table object for the currently selected row, or {@code null} if no selection
     * @since 3.5
     */
    @Nullable
    private MediaLibraryTableObject getSelectedRowObject()
    {
        int selectedRow = libraryTable.getSelectedRow();
        return selectedRow == -1
                ? null
                : libraryTableModel.getRowObject(libraryTable.convertRowIndexToModel(selectedRow));
    }

    /**
     * Create a standardized {@link JFileChooser} for this panel to use
     * @param selectedFile (Optional) Where to open the file chooser, and/or the file to be selected by default
     * @return A {@link JFileChooser} to use
     * @since 5.2
     */
    private JFileChooser createFileChooser(@Nullable File selectedFile)
    {
        JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setDialogTitle("Select Media File");
        if (selectedFile != null && selectedFile.exists())
        {
            if (selectedFile.isDirectory()) fileChooser.setCurrentDirectory(selectedFile);
            if (selectedFile.isFile()) fileChooser.setSelectedFile(selectedFile);
        }
        fileChooser.resetChoosableFileFilters(); // The instance is shared, so we need to clear before setting the filter again
        fileChooser.setFileFilter(new FileNameExtensionFilter("Media Files", MediaFormats.ALL_FORMATS));
        return fileChooser;
    }

    /**
     * Displays a file chooser dialog, gets metadata on the selected file, builds an entry for the media library,
     * and appends the entry to the table
     */
    private void addFile()
    {
        JFileChooser fileChooser = createFileChooser(null);
        int returnVal = fileChooser.showOpenDialog(JFrame.getFrames()[0]);

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            addFile(fileChooser.getSelectedFile());
        } else
        {
            logger.debug("Open command cancelled by user");
        }
    }

    /**
     * User has selected Add File
     *
     * @param file the File to add to the media manager
     */
    private void addFile(File file)
    {
        try
        {
            IMedia media = mediaManager.addMediaFromLocalFile(file, USER);

            // At the time of this coding it was only possible to add content one item at a time. If we ever change that
            // behavior to add multiple items we should also think about changing this default behavior where we
            // automatically open and play the content. Doing this auto-open also solves another problem where content
            // would get auto-opened when the project was reopened because the docking.xml did not have a record of the
            // dockable having been closed.

            mediaPlayerManager.createDefaultPlayer(media);
        } catch (IllegalArgumentException e)
        {
            showMediaErrorMessage(file, e);
        }
    }

    /**
     * Standardized error dialog reporting for {@link IMediaManager} exceptions thrown during media creation
     * @param file The file being processed when the error occurred
     * @param exception The exception being thrown
     */
    private void showMediaErrorMessage(File file, IllegalArgumentException exception)
    {
        String message;
        String title;
        if (exception.getMessage().contains("exist"))
        {
            title = "File Not Found";
            message = "Could not find file: " + file.getPath();
        } else
        {
            title = "Unsupported Media Type";
            message = "Unsupported media extension for file: " + file.getName();
        }
        AlwaysOnTopJOptionPane.showErrorDialog(message, title);
    }

    /**
     * Displays dialog for the user to enter the name and URL of the media item
     */
    private void addUrl()
    {
        UrlDialog urlDialog = new UrlDialog(mediaManager, null);
        urlDialog.setVisible(true);
    }

    /**
     * Updates whether the remove and open buttons are enabled due to a selection change within the table.
     * The buttons are enabled when an item is selected and disabled when nothing is selected.
     */
    private void tableSelectionChanged(ListSelectionEvent ignored)
    {
        final var rowObject = getSelectedRowObject();
        final boolean isLocalFile = rowObject != null && rowObject.isLocalFile();
        final var media = rowObject != null ? rowObject.getMedia() : null;
        final boolean enable = media != null;

        libraryRemoveButton.setEnabled(enable);
        libraryOpenButton.setEnabled(enable);

        // We can only send URL-based videos as CoT
        sendAsCotButton.setEnabled(enable && media instanceof IVideo && !isLocalFile);

        // Don't allow the user to edit a video that is defined by a connection entry (meaning it was created
        // from CoT or a downloaded video feed).
        final var hasConnectionEntry = media instanceof IVideo video && video.getConnectionEntry() != null;
        libraryEditButton.setEnabled(enable && !hasConnectionEntry && !isLocalFile);
    }

    public void changeColor()
    {
    }

    private void onProjectOpened(@Observes ProjectOpened event)
    {
        // Restore JXTable state (media entries are restored by MediaManager.startService())
        restoreState();
    }

    private void handleProjectSaving(@Observes ProjectSaving event)
    {
        storeState();
    }

    private void handleProjectClosing(@Observes ProjectClosing event)
    {
        libraryTableModel.removeTableModelListener(tableModelListener);
        mediaManager.removeMediaManagerListener(mediaManagerListener);
        libraryTable.removeMouseListener(libraryTableMouseListener);
    }

    @RemoveObserver
    private void handleProjectClosed(@Observes ProjectClosed event)
    {
        restored = false;
    }

    /**
     * Save the library panels state.
     *
     * @see #restoreState()
     */
    private void storeState()
    {
        // root map
        final IRestorableMap rootMap = JSONMapUtils.createNewRestorableMap();

        libraryTable.getRestorableMap(rootMap);

        // write entire map to a file
        rootMap.writeToFile(getStateFile());
    }

    /**
     * Restore the state of the library panel.
     *
     * @see #storeState()
     */
    private void restoreState()
    {
        if (restored) return;

        libraryTable.restoreFromMap(getStoredState());

        restored = true;
    }

    /**
     * Return the entire state map or {@code null} if the file does not exist.
     *
     * @return The state map or {@code null} if the map doesn't exist.
     */
    private IRestorableMap getStoredState()
    {
        IRestorableMap rootMap = null;

        // look for the state file
        File stateFile = getStateFile();

        // if it exists, turn it back into a map
        if (stateFile.exists())
        {
            try
            {
                rootMap = JSONMapUtils.restoreMap(stateFile);
            } catch (Exception e)
            {
                logger.error("Error restoring state from {}", stateFile, e);
            }
        }

        return rootMap;
    }

    /**
     * Find the Explorer Manager state file in the VFS.
     *
     * @return The Explorer Manager state file, or {@code null} if it does not exist.
     */
    private File getStateFile()
    {
        return new File(getStateDirectory(), VFS_STATE_FILENAME);
    }

    /**
     * Find the VFS directory where the Explorer Manager state should be stored.
     *
     * @return The VFS directory where the Explorer Manager state should be stored.
     */
    private File getStateDirectory()
    {
        File dir = projectVFSManager.getVirtualFileSystem(VFS_NAMESPACE).getRootDir();

        return new File(dir.getPath());
    }

    /**
     * Handler for MediaManager callbacks.
     *
     * @since 3.0
     */
    private class MediaManagerListener extends MediaManagerListenerAdapter
    {
        @Override
        public void mediaAdded(IMedia media)
        {
            libraryTableModel.addMedia(media);
        }

        @Override
        public void mediaRemoved(IMedia media)
        {
            libraryTableModel.removeMedia(media);
        }

        @Override
        @InvokeOnEDT
        public void mediaCleared()
        {
            libraryTableModel.clear();
        }
    }

    /**
     * Popup menu to prompt the user to either select a file or a URL to add to the library.
     */
    private class ChooseMediaTypePopupMenu extends JPopupMenu
    {
        ChooseMediaTypePopupMenu()
        {
            JMenuItem fileType = new JMenuItem("File");
            fileType.addActionListener(ae -> addFile());
            add(fileType);

            JMenuItem urlType = new JMenuItem("URL");
            urlType.addActionListener(ae -> addUrl());
            add(urlType);
        }
    }

    /**
     * A mouse listener that can be attached to the library table to respond to double-click;
     * double-clicking will open the selected media item.
     *
     * @since 5.1
     */
    private final class LibraryTableMouseListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
            {
                createMediaPlayerForSelectedObject();
            }
        }
    }

    /**
     * Initializes the GUI components
     */
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        contentPanel = new JPanel();
        var progressPanel = new JPanel();
        var progressBar = new JProgressBar();
        selectionPanel = new JPanel();
        var libraryPanel = new JPanel();
        var libraryButtonPanel = new JPanel();
        libraryAddButton = new JButton();
        libraryEditButton = new JButton();
        libraryRemoveButton = new JButton();
        libraryOpenButton = new JButton();
        sendAsCotButton = new JButton();
        downloadWrapper = new JPanel();
        var tableScrollPane = new JScrollPane();
        libraryTable = new MediaLibraryJXTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(new Dimension(300, 400));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== contentPanel ========
        {
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new CardLayout());

            //======== progressPanel ========
            {
                progressPanel.setName("progressPanel");
                progressPanel.setLayout(new BorderLayout());

                //---- progressBar ----
                progressBar.setIndeterminate(true);
                progressBar.setStringPainted(true);
                progressBar.setString("Retrieving video list");
                progressBar.setFont(progressBar.getFont().deriveFont(progressBar.getFont().getSize() + 2f));
                progressBar.setName("progressBar");
                progressPanel.add(progressBar, BorderLayout.NORTH);
            }
            contentPanel.add(progressPanel, "progressCard");

            //======== selectionPanel ========
            {
                selectionPanel.setName("selectionPanel");
                selectionPanel.setLayout(new BorderLayout());
            }
            contentPanel.add(selectionPanel, "selectionCard");

            //======== libraryPanel ========
            {
                libraryPanel.setName("libraryPanel");
                libraryPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default, $lgap, fill:default:grow"));

                //======== libraryButtonPanel ========
                {
                    libraryButtonPanel.setName("libraryButtonPanel");
                    libraryButtonPanel.setLayout(new FormLayout(
                            "default:grow, 5*(default, $lcgap), left:default",
                            "default"));

                    //---- libraryAddButton ----
                    libraryAddButton.setToolTipText("Add item to library");
                    libraryAddButton.setName("libraryAddButton");
                    libraryAddButton.addActionListener(e -> libraryAddButtonActionPerformed());
                    libraryButtonPanel.add(libraryAddButton, cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                    //---- libraryEditButton ----
                    libraryEditButton.setEnabled(false);
                    libraryEditButton.setToolTipText("Edit selected item");
                    libraryEditButton.setName("libraryEditButton");
                    libraryEditButton.addActionListener(e -> libraryEditButtonActionPerformed());
                    libraryButtonPanel.add(libraryEditButton, cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                    //---- libraryRemoveButton ----
                    libraryRemoveButton.setToolTipText("Remove selected item from library");
                    libraryRemoveButton.setEnabled(false);
                    libraryRemoveButton.setName("libraryRemoveButton");
                    libraryRemoveButton.addActionListener(e -> libraryRemoveButtonActionPerformed());
                    libraryButtonPanel.add(libraryRemoveButton, cc.xy(6, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                    //---- libraryOpenButton ----
                    libraryOpenButton.setToolTipText("Open selected item in media player");
                    libraryOpenButton.setEnabled(false);
                    libraryOpenButton.setName("libraryOpenButton");
                    libraryOpenButton.addActionListener(e -> createMediaPlayerForSelectedObject());
                    libraryButtonPanel.add(libraryOpenButton, cc.xy(8, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                    //---- sendAsCotButton ----
                    sendAsCotButton.setToolTipText("Send this media as CoT");
                    sendAsCotButton.setEnabled(false);
                    sendAsCotButton.setName("sendAsCotButton");
                    sendAsCotButton.addActionListener(e -> sendAsCotButtonClicked());
                    libraryButtonPanel.add(sendAsCotButton, cc.xy(10, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                    //======== downloadWrapper ========
                    {
                        downloadWrapper.setName("downloadWrapper");
                        downloadWrapper.setLayout(new BorderLayout());
                    }
                    libraryButtonPanel.add(downloadWrapper, cc.xy(12, 1));
                }
                libraryPanel.add(libraryButtonPanel, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                //======== tableScrollPane ========
                {
                    tableScrollPane.setName("tableScrollPane");

                    //---- libraryTable ----
                    libraryTable.setColumnControlVisible(true);
                    libraryTable.setModel(new DefaultTableModel(
                            new Object[][]{
                                    {null, null, null, null, null, null, null},
                            },
                            new String[]{
                                    "Name", "Path/URL", "Type", "Creation Time", "Duration/Size", "Timeline", "Color"
                            })
                    {
                        Class<?>[] columnTypes = new Class<?>[]{
                                String.class, String.class, String.class, Date.class, Double.class, Object.class, Object.class
                        };
                        boolean[] columnEditable = new boolean[]{
                                false, false, false, false, false, false, false
                        };

                        @Override
                        public Class<?> getColumnClass(int columnIndex)
                        {
                            return columnTypes[columnIndex];
                        }

                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex)
                        {
                            return columnEditable[columnIndex];
                        }
                    });
                    {
                        TableColumnModel cm = libraryTable.getColumnModel();
                        cm.getColumn(0).setMinWidth(100);
                        cm.getColumn(0).setPreferredWidth(100);
                        cm.getColumn(1).setMinWidth(50);
                        cm.getColumn(1).setPreferredWidth(200);
                        cm.getColumn(4).setMinWidth(100);
                        cm.getColumn(4).setPreferredWidth(100);
                    }
                    libraryTable.setPreferredScrollableViewportSize(new Dimension(200, 360));
                    libraryTable.setName("libraryTable");
                    tableScrollPane.setViewportView(libraryTable);
                }
                libraryPanel.add(tableScrollPane, cc.xy(1, 3));
            }
            contentPanel.add(libraryPanel, "libraryCard");
        }
        add(contentPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPanel;
    private JPanel selectionPanel;
    private JButton libraryAddButton;
    private JButton libraryEditButton;
    private JButton libraryRemoveButton;
    private JButton libraryOpenButton;
    private JButton sendAsCotButton;
    private JPanel downloadWrapper;
    private MediaLibraryJXTable libraryTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
