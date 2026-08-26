package gov.takx.core.breadcrumb;

import com.atakmap.commoncommo.CommoException;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.platform.comms.commo.DeviceDisplayType;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.mapobject.gui.SelectContactsDialog;
import gov.takx.core.view.gui.breadcrumb.TrackSendOptionsDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Objects;

/**
 * Service used to export {@link gov.takx.api.playback.IMarker} {@link ITrack tracks} to files and optionally send the
 * tracks to other users or a TAK server.
 *
 * @since 5.4
 */
@ApplicationScoped
public class TrackExportService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ImageIcon TAK_SERVER_ICON =
            UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "tak_server.png");

    /**
     * Track export file type options
     */
    public enum FileType
    {
        CSV, KML, KMZ, SERVER
    }

    private final INotificationService notificationService;
    private final TrackFileUtils trackFileUtils;
    private final ServerTrackPublishService publishService;
    private final ITakServerConnectionManager serverConnectionManager;
    private final IMapObjectManager mapObjectManager;
    private final String nodeUuid;
    private final IExtendedDataPackageManager dataPackageManager;
    private final IContactManager commoContactManager;

    @Inject
    TrackExportService(TrackFileUtils trackFileUtils, INotificationService notificationService,
                       ServerTrackPublishService publishService, ITakServerConnectionManager serverConnectionManager,
                       IMapObjectManager mapObjectManager, @NodeUUID String nodeUuid,
                       IExtendedDataPackageManager dataPackageManager, IContactManager commoContactManager)
    {
        this.trackFileUtils = trackFileUtils;
        this.notificationService = notificationService;
        this.publishService = publishService;
        this.serverConnectionManager = serverConnectionManager;
        this.mapObjectManager = mapObjectManager;
        this.nodeUuid = nodeUuid;
        this.dataPackageManager = dataPackageManager;
        this.commoContactManager = commoContactManager;
    }

    /**
     * Return {@code true} if publishing is supported for the {@link gov.takx.api.playback.IMarker} with the provided
     * UID. Publishing is only supported for the self marker (parity with ATAK/WinTAK).
     *
     * @param markerUid The marker's UID
     * @return {@code true} if the marker can publish its tracks to TAK servers
     * @since 5.5
     */
    public boolean publishingSupported(@Nonnull String markerUid)
    {
        final var selfMarker = mapObjectManager.getSelfMarker();
        return selfMarker != null && selfMarker.getUid().equals(markerUid);
    }

    /**
     * @param track The track to export
     * @param fileType The type of file to create
     */
    public void export(@Nonnull ITrack track, @Nonnull FileType fileType)
    {
        // "exporting" to a server publishes the track to the server. Alternatively, the user may choose to export the
        // track to a file and then send it to a server. That is a different operation that is handled below.
        if (fileType == FileType.SERVER)
        {
            publishTrackFileToServer(track);
            return;
        }

        final var file = getDestinationFileFromUser(track, fileType);

        if (file != null && createFile(track, fileType, file))
        {
            notificationService.show(
                    Notification.builder("Exported track " + track.getName() + " to file " + file.getName())
                            .setType(Notification.Type.Success)
                            .build());

            offerSendOptions(track, file);
        }
    }

    /**
     * Prompts the user to select a destination file for a track export.
     * 
     * @param track The track
     * @param fileType The type of file to create
     * @return The destination file where data should be written or {@code null} if the selection operation was cancelled 
     */
    @Nullable
    @InvokeAndWaitOnEDT
    private File getDestinationFileFromUser(@Nonnull ITrack track, @Nonnull FileType fileType)
    {
        final var trackName = track.getName();
        final var extension = fileType.name().toLowerCase();

        final var chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.setDialogTitle("Export Track: " + trackName);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.resetChoosableFileFilters();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Track File (." + extension + ")", extension));
        chooser.setSelectedFile(new File(FileUtils.sanitizeForFilename(trackName + "." + extension)));

        return chooser.showSaveDialog(JFrame.getFrames()[0]) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile()
                : null;
    }

    /**
     * Presents the user with file sending options.
     *
     * @param track The track
     * @param file The track file
     */
    @InvokeOnEDT
    private void offerSendOptions(@Nonnull ITrack track, @Nonnull File file)
    {
        new TrackSendOptionsDialog(file,
                e -> new SelectContactsDialog(
                        "Send " + file.getName() + " to contacts",
                        contactUids -> sendTrackFileToContacts(file, contactUids),
                        null)
                        .setVisible(true),
                e -> sendTrackFileToServer(file))
                .setVisible(true);
    }

    /**
     * @param track The track to export
     * @param fileType The type of file to create
     * @param file The file where data should be written
     * @return {@code true} if the file was successfully created
     */
    private boolean createFile(@Nonnull ITrack track, @Nonnull FileType fileType, @Nonnull File file)
    {
        boolean success = false;

        try
        {
            success = switch (fileType)
            {
                case CSV -> trackFileUtils.exportTrackCSV(track, file);
                case KML -> trackFileUtils.exportTrackKml(track, file);
                case KMZ -> trackFileUtils.exportTrackKmz(track, file);
                case SERVER -> throw new IllegalStateException("Publishing not supported via this method");
            };
        } catch (Exception e)
        {
            logger.error(getFileCreationErrorString(track, fileType), e);
        }

        if (!success)
        {
            notificationService.show(
                    Notification.builder(getFileCreationErrorString(track, fileType))
                            .setType(Notification.Type.Error)
                            .build());
        }

        return success;
    }

    /**
     * @param track The track that was being exported
     * @param fileType The export file type
     * @return A message indicating that track export failed
     */
    private String getFileCreationErrorString(@Nonnull ITrack track, @Nonnull FileType fileType)
    {
        return "Error creating " + fileType.name() + " for track " + track.getName();
    }

    /**
     * @param file The exported {@link ITrack} file to send to contacts
     * @param uids The contact UIDs
     */
    private void sendTrackFileToContacts(@Nonnull File file, @Nonnull Collection<String> uids)
    {
        try
        {
            final var dataPackageFile = dataPackageManager.createNewTempDataPackage(file.getName());
            dataPackageManager.addFileToDataPackage(dataPackageFile, file);

            final var commoContacts = uids.stream().map(commoContactManager::findContactByUid).filter(Objects::nonNull).toList();
            if (!commoContacts.isEmpty())
            {
                dataPackageManager.sendPackageToContacts(dataPackageFile, commoContacts);
                notificationService.show(Notification.builder("Sent track file " + file + " to selected users", Notification.Type.Success).build());
            }
        } catch (IOException e)
        {
            final var message = "Error creating data package for track file " + file;
            logger.error(message, e);
            notificationService.show(Notification.builder(message, Notification.Type.Error).build());
        } catch (CommoException e)
        {
            final var message = "Error sending data package for track file " + file;
            logger.error(message, e);
            notificationService.show(Notification.builder(message, Notification.Type.Error).build());
        }
    }

    /**
     * @param file The exported {@link ITrack} file to send to a server
     */
    private void sendTrackFileToServer(@Nonnull File file)
    {
        final ITakServerConnection server = getDestinationServer();

        if (server == null) return; // user cancelled

        try
        {
            final var dataPackage = dataPackageManager.createNewDataPackage(file.getName());
            dataPackageManager.addFileToDataPackage(dataPackage, file);
            dataPackageManager.sendPackageToServer(dataPackage, server)
                    .whenComplete((response, t) -> showTrackFileSendResult(file, server, t != null));
        } catch (IOException e)
        {
            showTrackFileSendResult(file, server, true);
        }
    }

    private void showTrackFileSendResult(@Nonnull File trackFile, @Nonnull ITakServerConnection server, boolean error)
    {
        final var message = ((error ? "Error sending" : "Sent") + " track file %s to TAK server %s")
                .formatted(trackFile.getName(), server.getDisplayName());
        final var type = error ? Notification.Type.Error : Notification.Type.Success;

        notificationService.show(Notification.builder(message).setType(type).build());
    }

    /**
     * Publishes a {@link ITrack track} to a TAK server. The track must belong to the self marker.
     *
     * @param track The track to publish
     * @throws IllegalArgumentException if the track does not belong to the self marker
     * @since 5.5
     */
    private void publishTrackFileToServer(@Nonnull ITrack track)
    {
        if (!publishingSupported(track.getMarkerUid())) throw new IllegalArgumentException("Only self marker tracks can be published");

        File file = null;

        try
        {
            file = File.createTempFile("track", "xml");
            file.deleteOnExit();

            if (!trackFileUtils.exportTrackKml(track, file))
            {
                notificationService.show(Notification.builder("Error creating KML file for publishing").setType(Notification.Type.Error).build());
                return;
            }

            final var server = getDestinationServer();

            if (server == null) return; // No servers available or the operation was cancelled by user

            // Publish the track
            publishService.publishTrack(server, nodeUuid, PreferenceEntry.CALL_SIGN.getStringValue(),
                    PreferenceEntry.SA_REPORTING_DISPLAY_TYPE.getValueAs(DeviceDisplayType.class).toString(),
                    PreferenceEntry.SA_REPORTING_TEAM.getStringValue(),
                    PreferenceEntry.SA_REPORTING_ROLE.getStringValue(),
                    file)
                    .thenAccept(response -> {
                        final var status = response.status();

                        if (status == HttpStatus.SC_OK)
                        {
                            notificationService.show(
                                    Notification.builder("Published track " + track.getName() + " to server " + server.getDisplayName())
                                            .setType(Notification.Type.Success)
                                            .build());
                        } else
                        {
                            logger.error("Error publishing track file to server: ({}) {}", status, response.reason());
                            notificationService.show(
                                    Notification.builder("Error publishing track file to server. See log for details.")
                                            .setType(Notification.Type.Error)
                                            .build());
                        }
                    });
        } catch (IOException e)
        {
            logger.error("Error {} to publish to server", file != null ? ("writing to file " + file) : "creating temp file", e);
            notificationService.show(
                    Notification.builder("Error creating track file to publish to server").setType(Notification.Type.Error).build());
        }
    }

    /**
     * @return The destination server or {@code null} if there are no servers available or the user cancelled the
     * operation
     * @since 5.5
     */
    @Nullable
    private ITakServerConnection getDestinationServer()
    {
        final var servers = serverConnectionManager.getTakServerConnections(true);

        if (servers.isEmpty())
        {
            notificationService.show(Notification.builder("No server available for publishing").setType(Notification.Type.Info).build());
            return null;
        }

        if (servers.size() == 1) return servers.stream().findFirst().get();

        // Ask the user to choose a destination server
        final var comboBox = new JComboBox<>(servers.toArray(new ITakServerConnection[0]));
        comboBox.setRenderer(new ServerRenderer());
        var result = AlwaysOnTopJOptionPane.showOptionDialog(comboBox, "Select a TAK Server", JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_OPTION, TAK_SERVER_ICON, new Object[]{"Select", "Cancel"}, null);

        return result == JOptionPane.OK_OPTION ? (ITakServerConnection) comboBox.getSelectedItem() : null;
    }

    /**
     * Renders an {@link ITakServerConnection}.
     *
     * @since 5.5
     */
    private static class ServerRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            setText(((ITakServerConnection) value).getDisplayName());
            return this;
        }
    }
}
