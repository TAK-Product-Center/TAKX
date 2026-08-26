package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.MissionDownloadStatus;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionUtils;
import gov.tak.api.contact.IContactNameResolver;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * List entry representing a {@link Mission} in {@link DataFeedListPanel}.
 *
 * @since 5.0
 */
public class DataFeedListEntry extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ImageIcon EMPTY_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "empty.png");
    private static final ImageIcon LOCK_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "lock.png");
    private static final ImageIcon DELETE_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "delete.png", new Dimension(24, 24), true);
    private static final ImageIcon DOWNLOAD_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "cloud_download.png", new Dimension(24, 24), true);
    private static final ImageIcon SYNC_OK_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-OK_36x36.png", new Dimension(24, 24), true);
    private static final ImageIcon SYNC_OK_NOT_SUBBED_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-OK-NotSub_36x36.png", new Dimension(24, 24), true);
    private static final ImageIcon SYNC_PROGRESS_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-Progress_36x36.png", new Dimension(24, 24), true);
    private static final ImageIcon SYNC_DISCONNECTED_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "Sync-Not-Available_36x36.png", new Dimension(24, 24), true);
    private final IDataSyncManager dataSyncManager;
    private final IContactNameResolver contactNameResolver;
    private final StandardTimeFormatting timeFormatting;
    private final DataFeedPanelController controller;
    private ITakServerConnection currentConnection;
    private Mission currentMission;
    private Instant lastMissionUpdate;
    private boolean userRequestedRemoteDeletion;

    /**
     * @param controller Data feed UI controller.
     * @param dataSyncManager Sync manager to handle operations with.
     * @param contactNameResolver Name resolver for derivative UI elements that need to resolve contact uids.
     * @param timeFormatting Formatter for timestamps.
     */
    public DataFeedListEntry(@Nonnull DataFeedPanelController controller, @Nonnull IDataSyncManager dataSyncManager,
                             @Nonnull IContactNameResolver contactNameResolver, @Nonnull StandardTimeFormatting timeFormatting)
    {
        this.controller = controller;
        this.dataSyncManager = dataSyncManager;
        this.contactNameResolver = contactNameResolver;
        this.timeFormatting = timeFormatting;

        initComponents();

        // Default icon states
        buttonDelete.setIcon(DELETE_ICON);
        buttonDownload.setIcon(DOWNLOAD_ICON);
        buttonSync.setIcon(SYNC_DISCONNECTED_ICON);
    }

    /**
     * When a user goes through the {@link DataFeedDeleteDialog} UI to request deletion of this data feed, and that deletion was a success
     * we mark this entry as having a deletion pending. This way when {@link DataFeedListPanel} is told that the feed was removed by
     * {@link gov.tak.api.comms.takserver.IDataSyncMissionListener} it can know whether it was initiated by the user, or by some remote person.
     *
     * @return {@code true} when a user previously requested this feed be deleted.
     */
    public boolean hasUserRequestedRemoteDeletion()
    {
        return userRequestedRemoteDeletion;
    }

    /**
     * Updates the availability of actions.
     *
     * @param isConnected Connection status to TAK server.
     */
    public void updateConnectionState(boolean isConnected)
    {
        boolean isSubscribed = false;
        boolean isOwner = false;
        boolean isTracked = false;
        MissionDownloadStatus downloadStatus = MissionDownloadStatus.NOT_DOWNLOADED;
        if (currentConnection != null && currentMission != null)
        {
            String name = currentMission.name();
            downloadStatus = dataSyncManager.getDownloadState(currentConnection, name);
            isSubscribed = dataSyncManager.isSubscribed(currentConnection, name);
            isOwner = dataSyncManager.isMissionOwner(currentConnection, name);
            isTracked = dataSyncManager.isTracked(currentConnection, name);
        }
        boolean isPartiallyDownloaded = downloadStatus.isAtLeastPartiallyDownloaded();

        // Downloading can only be done while we are connected.
        buttonDownload.setEnabled(isConnected);

        // Download button should be shown when not subscribed (auto-updating)
        // or if it's a mission we made and can delete remotely.
        buttonDownload.setVisible(!isSubscribed && isTracked);

        // Sync button shows the mission info, so for single-downloads that are not sync'd we still want to show it.
        buttonSync.setVisible(isPartiallyDownloaded);

        // Delete acts as deletion and removing subscriptions, so we show that for being downloaded or being sync'd.
        buttonDelete.setVisible(isPartiallyDownloaded || isOwner);

        // Update icons
        iconLabel.setIcon(currentMission.passwordProtected() ? LOCK_ICON : EMPTY_ICON);
        if (isSubscribed)
        {
            buttonSync.setIcon(downloadStatus.isInProgress() ? (isConnected ? SYNC_PROGRESS_ICON : SYNC_DISCONNECTED_ICON) : isTracked ? SYNC_OK_ICON : SYNC_OK_NOT_SUBBED_ICON);
        } else
        {
            buttonSync.setIcon(downloadStatus.isInProgress() ? (isConnected ? SYNC_PROGRESS_ICON : SYNC_DISCONNECTED_ICON) : SYNC_OK_NOT_SUBBED_ICON);
        }
    }

    /**
     * Updates the listed details of the mission.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission to list details of.
     */
    public void updateMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission)
    {
        setCurrentConnection(connection);

        currentMission = mission;
        lastMissionUpdate = MissionUtils.getLastUpdateTime(mission);

        // Time is shown as the date for anything before yesterday, otherwise just the time from today
        final String lastModified = lastMissionUpdate.isAfter(Instant.now().truncatedTo(ChronoUnit.DAYS))
                ? timeFormatting.formatTimeOnlyWithCurrent(lastMissionUpdate.toEpochMilli())
                : timeFormatting.formatDateOnlyWithCurrent(lastMissionUpdate.toEpochMilli());

        // Compute item count from markers + resources, but size just from resources
        List<MissionAdd.TString> uids = mission.uids() == null ? Collections.emptyList() : mission.uids();
        List<MissionAdd.TResource> resources = mission.contents() == null ? Collections.emptyList() : mission.contents();
        int itemCount = uids.size() + resources.size();
        String itemSize = SizeFormatter.formatEstimate(resources.stream().mapToLong(c -> c.data().size()).sum());

        // Update labels
        titleLabel.setText(mission.name());
        infoLabel.setText(String.format("%s, %d items, %s", lastModified, itemCount, itemSize));

        // Initial action state
        updateConnectionState(true);
    }

    /**
     * Show mission details panel if current details are populated.
     */
    private void openMissionDetailsPanel()
    {
        if (currentConnection != null && currentMission != null)
        {
            controller.openMissionDetails(currentConnection, currentMission);
        }
    }

    /**
     * Show download popup if current details are populated.
     */
    private void showDownloadPopup()
    {
        if (currentConnection != null && currentMission != null)
        {
            String name = currentMission.name();
            String uri = currentConnection.getUri().toString();
            Window window = SwingUtilities.windowForComponent(this);
            Consumer<IDataSyncManager.MissionDownload> onSuccess = download -> {
                controller.openMissionDetails(currentConnection, currentMission);
                updateConnectionState(true);
            };
            Consumer<IDataSyncManager.MissionDownload> onFailure = download -> handleApiError(name, uri, "downloading", download.requireError());
            DataFeedDownloadDialog dialog = new DataFeedDownloadDialog(window, dataSyncManager, contactNameResolver, currentConnection, currentMission, onSuccess, onFailure);
            dialog.setVisible(true);
        }
    }

    /**
     * Show delete popup if current details are populated.
     */
    private void showDeletePopup()
    {
        if (currentConnection != null && currentMission != null)
        {
            String name = currentMission.name();
            String uri = currentConnection.getUri().toString();
            Window window = SwingUtilities.windowForComponent(this);
            Consumer<IDataSyncManager.MissionDeletion> onDelete = deletion -> {
                userRequestedRemoteDeletion = deletion.remote();
                setEnabled(false);
                updateConnectionState(true);
            };
            Consumer<IDataSyncManager.MissionDeletion> onFailure = deletion -> handleApiError(name, uri, "deleting", deletion.requireError());
            DataFeedDeleteDialog dialog = new DataFeedDeleteDialog(window, dataSyncManager, currentConnection, currentMission, onDelete, onFailure);
            dialog.setVisible(true);
        }
    }

    /**
     * Called when {@link #showDeletePopup()} or {@link #showDownloadPopup()}'s actions fail.
     *
     * @param name Feed name.
     * @param uri TAK Server uri.
     * @param verb Action that failed.
     * @param error Failure reason
     */
    private void handleApiError(@Nonnull String name, @Nonnull String uri, @Nonnull String verb, @Nonnull ApiException error)
    {
        String errorMessage;
        if (error.isConnectionError())
        {
            // Just noting, errors like this take a bit to show up in the UI.
            // By default, the API will schedule a couple of re-try attempts if the connection is spotty.
            errorMessage = String.format("Failed %s feed '%s' on %s\n\nConnection not available", verb, name, uri);
        } else if (error.isAuthenticationError())
        {
            // This generally shouldn't happen since the feed list should only populate by this point after you've been authenticated.
            // And even if your session expires, the API will re-up your session a bit before the validity window closes.
            errorMessage = String.format("Failed %s feed '%s' on %s\n\n%s is not authenticated", verb, name, uri, currentConnection.getConnectionConfig().getUsername());
        } else if (error.isUntrackedServer())
        {
            // Another case of this not really being feasible for the same reason as the auth-problem above.
            errorMessage = String.format("Failed %s feed '%s' on %s\n\nServer not tracked by data-sync manager", verb, name, uri);
        } else
        {
            // Anything not covered by those cases goes here.
            errorMessage = String.format("Failed %s feed '%s' on %s\n\nUnknown error", verb, name, uri);
        }
        logger.error(errorMessage.replace("\n\n", " - "), error);
        String title = "Failed " + verb + " feed '" + name + "'";
        JOptionPane.showMessageDialog(this, title, errorMessage, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * @param currentConnection Current connection to server declaring the {@link #getCurrentMission() current mission}.
     * @since 5.3
     */
    public void setCurrentConnection(@Nonnull ITakServerConnection currentConnection)
    {
        this.currentConnection = currentConnection;
    }

    /**
     * @return Current connection to server declaring the {@link #getCurrentMission() current mission}.
     */
    @Nullable
    public ITakServerConnection getCurrentConnection()
    {
        return currentConnection;
    }

    /**
     * @return Current mission represented by this entry.
     */
    @Nullable
    public Mission getCurrentMission()
    {
        return currentMission;
    }

    /**
     * @return Last time the {@link #getCurrentMission() current mission} was updated.
     */
    @Nullable
    public Instant getLastMissionUpdate()
    {
        return lastMissionUpdate;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        infoPanel = new JPanel();
        iconLabel = new JLabel();
        titleLabel = new JLabel();
        infoLabel = new JLabel();
        actionsPanel = new JPanel();
        buttonDownload = new JButton();
        buttonSync = new JButton();
        buttonDelete = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "pref:grow, $lcgap, right:pref",
                "pref"));

        //======== infoPanel ========
        {
            infoPanel.setName("infoPanel");
            infoPanel.setLayout(new FormLayout(
                    "default, pref:grow",
                    "2*(pref)"));

            //---- iconLabel ----
            iconLabel.setMaximumSize(null);
            iconLabel.setMinimumSize(null);
            iconLabel.setName("iconLabel");
            infoPanel.add(iconLabel, cc.xywh(1, 1, 1, 2));

            //---- titleLabel ----
            titleLabel.setText("text");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            titleLabel.setName("titleLabel");
            infoPanel.add(titleLabel, cc.xy(2, 1));

            //---- infoLabel ----
            infoLabel.setText("text");
            infoLabel.setForeground(Color.gray);
            infoLabel.setName("infoLabel");
            infoPanel.add(infoLabel, cc.xy(2, 2));
        }
        add(infoPanel, cc.xy(1, 1));

        //======== actionsPanel ========
        {
            actionsPanel.setName("actionsPanel");
            actionsPanel.setLayout(new FormLayout(
                    "pref:grow, default:grow, pref:grow",
                    "fill:pref"));

            //---- buttonDownload ----
            buttonDownload.setToolTipText("Download");
            buttonDownload.setName("buttonDownload");
            buttonDownload.addActionListener(e -> showDownloadPopup());
            actionsPanel.add(buttonDownload, cc.xy(1, 1));

            //---- buttonSync ----
            buttonSync.setToolTipText("Sync");
            buttonSync.setName("buttonSync");
            buttonSync.addActionListener(e -> openMissionDetailsPanel());
            actionsPanel.add(buttonSync, cc.xy(2, 1));

            //---- buttonDelete ----
            buttonDelete.setToolTipText("Delete");
            buttonDelete.setName("buttonDelete");
            buttonDelete.addActionListener(e -> showDeletePopup());
            actionsPanel.add(buttonDelete, cc.xy(3, 1));
        }
        add(actionsPanel, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel infoPanel;
    private JLabel iconLabel;
    private JLabel titleLabel;
    private JLabel infoLabel;
    private JPanel actionsPanel;
    private JButton buttonDownload;
    private JButton buttonSync;
    private JButton buttonDelete;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
