package gov.takx.core.view.gui.breadcrumb;

import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.breadcrumb.BreadcrumbManager;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import java.time.Instant;

/**
 * Provides support for the tracks button popup menu.
 *
 * @since 5.5
 */
@ApplicationScoped
public class TracksUiUtils
{
    private static final Icon CLEAR_TRACKS_ICON =
            UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "Clear-Tracks_32x32.png");
    private static final Icon NEW_TRACK_ICON =
            UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "New-Track_32x32.png");

    private final BreadcrumbManager breadcrumbManager;
    private final IMapObjectManager mapObjectManager;
    private final INotificationService notificationService;

    @Inject
    public TracksUiUtils(BreadcrumbManager breadcrumbManager, IMapObjectManager mapObjectManager,
                         INotificationService notificationService)
    {
        this.breadcrumbManager = breadcrumbManager;
        this.mapObjectManager = mapObjectManager;
        this.notificationService = notificationService;
    }

    /**
     * Presents a dialog that prompts the user to provide a {@link gov.takx.api.breadcrumb.ITrack track} name and
     * creates a new track.
     */
    public void handleNewTrackRequest()
    {
        final var trackName = (String) AlwaysOnTopJOptionPane.showInputDialog(null,
                "Enter a name for the new self marker track", "New Self Marker Track",
                JOptionPane.QUESTION_MESSAGE, NEW_TRACK_ICON, null,
                breadcrumbManager.getDefaultTrackName(Instant.now()));

        // Operation cancelled
        if (trackName == null) return;

        if (StringUtils.isEmpty(trackName, true))
        {
            notificationService.show(
                    Notification.builder("A track name is required. No track created.")
                            .setType(Notification.Type.Warning).build());
            return;
        }

        breadcrumbManager.startNewTrack(mapObjectManager.getSelfMarker().getUid(),
                PreferenceEntry.CALL_SIGN.getStringValue(), trackName.trim(), false);
        notificationService.show(Notification.builder("Created new track " + trackName, Notification.Type.Success).build());
    }

    /**
     * Presents a dialog that asks the user to confirm that they'd like to clear all
     * {@link gov.takx.api.breadcrumb.ITrack tracks} from the globe. If confirmed, all tracks are hidden.
     */
    public void handleClearRequest()
    {
        final var result = AlwaysOnTopJOptionPane.showConfirmDialog(null,
                "Clear all tracks from map?", "Clear Tracks", JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION, CLEAR_TRACKS_ICON);

        if (result == JOptionPane.YES_OPTION)
        {
            final var count = breadcrumbManager.hideAllTracks();
            notificationService.show(Notification.builder(count + " tracks cleared", Notification.Type.Success).build());
        }
    }
}
