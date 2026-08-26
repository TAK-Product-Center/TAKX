package gov.takx.core.view.gui.contacts.datapackage;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.chat.ChatDataPackageService;
import gov.takx.core.chat.ChatLink;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.mapobject.gui.MapObjectAttachmentsPanel;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.JPanel;

import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Manages the {@link ChatLinksPanel}.
 *
 * @since 5.2
 */
@ApplicationScoped
public class ChatLinksPanelController
{
    private static final URL PAPERCLIP_URL = UIResources.getUrl(PLATFORM_ICONS, "paperclip.png");

    private final IMapObjectFinder mapObjectFinder;
    private final IGlobeManager globeManager;
    private final ChatLinksPanel chatLinksPanel;
    private final WidgetLayerManager widgetLayerManager;
    private final ChatDataPackageService chatDataPackageService;
    private final IUISpaceManager spaceManager;

    @Inject
    ChatLinksPanelController(IMapObjectFinder mapObjectFinder, IGlobeManager globeManager,
                             WidgetLayerManager widgetLayerManager, IUISpaceManager spaceManager,
                             ChatDataPackageService chatDataPackageService)
    {
        this.mapObjectFinder = mapObjectFinder;
        this.globeManager = globeManager;
        this.spaceManager = spaceManager;
        this.widgetLayerManager = widgetLayerManager;
        this.chatDataPackageService = chatDataPackageService;

        chatLinksPanel = new ChatLinksPanel(e -> FileUtils.browseFile(chatDataPackageService.getAttachmentsDirectory()));
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        chatLinksPanel.clear();
    }

    /**
     * @param conversationName The name of the conversation where the data package was sent or received
     * @param chatLinks The chat links contained in the data package
     * @param received Boolean indicating if the package was received from a remote contact (vs sent by the local user)
     */
    public void setChatLinks(@Nonnull String conversationName, @Nonnull List<ChatLink> chatLinks, boolean received)
    {
        final List<ChatLinkPanel> panels = chatLinks.stream()
                .sorted(Comparator.comparing(ChatLink::name))
                .map(link -> {
                    final String name = link.name();
                    final String uid = link.uid();
                    final IMapObject mapObject = StringUtils.isEmpty(uid) ? null : mapObjectFinder.findMapObjectByUid(uid);
                    return new ChatLinkPanel(name,
                            mapObject != null ? getOpenAttachmentRunnable(mapObject) : getOpenAttachmentRunnable(name),
                            mapObject != null ? getGoToRunnable(mapObject) : null,
                            mapObject != null ? getOpenMenuRunnable(mapObject) : null);
                })
                .toList();

        chatLinksPanel.add(conversationName, panels);

        // Only show the folder button if the package was received from a remote contact and it contains at least one
        // file (implied by a ChatLink with an empty UID)
        chatLinksPanel.setFolderButtonVisible(received && chatLinks.stream().anyMatch(chatLink -> chatLink.uid().isEmpty()));
    }

    /**
     * @return The panel managed by this controller
     */
    @Nonnull
    public JPanel getPanel()
    {
        return chatLinksPanel;
    }

    /**
     * @param mapObject The map object to act on
     * @return A runnable that will open the map object's attachment or {@code null} if the map object does not have any 
     * attachments
     */
    @Nullable
    private Runnable getOpenAttachmentRunnable(@Nonnull IMapObject mapObject)
    {
        // Show all map object attachments if there are any
        final List<File> attachments = mapObject.getAttachments();
        return attachments.isEmpty() ? null : () -> {
            var mapObjectAttachmentsPanel = new MapObjectAttachmentsPanel(mapObject, false);
            var componentDescriptorBuilder = new ComponentDescriptorBuilder(mapObjectAttachmentsPanel, "Attachments")
                    .setComponentPosition(ComponentPosition.RIGHT)
                    .setExternalizable(false)
                    .setPinned(true)
                    .setToolbarButtonVisible(false)
                    .setIcon(PAPERCLIP_URL);

            spaceManager.addComponent(componentDescriptorBuilder.build(), mapObjectAttachmentsPanel);
            spaceManager.showComponent(mapObjectAttachmentsPanel);
        };
    }

    /**
     * @param fileName The attached file name to act on
     * @return A runnable that will open the chat attachment or {@code null}
     * if there is no local associated copy of the attachment file.
     * @since 5.7
     */
    @Nullable
    private Runnable getOpenAttachmentRunnable(@Nonnull String fileName)
    {
        File file = chatDataPackageService.getAttachmentByName(fileName);
        return file == null ? null : () -> FileUtils.launchFile(file);
    }

    /**
     * @param mapObject The map object to act on
     * @return A runnable that will go to the map object's location on the globe
     */
    @Nonnull
    private Runnable getGoToRunnable(@Nonnull IMapObject mapObject)
    {
        return () -> {
            widgetLayerManager.closeOpenRadialMenus();
            //noinspection DataFlowIssue
            globeManager.getLastFocusedGlobe().goTo(mapObject.getLocation());
        };
    }

    /**
     * @param mapObject The map object to act on
     * @return A runnable that will open the map object's radial menu
     */
    @Nonnull
    private Runnable getOpenMenuRunnable(@Nonnull IMapObject mapObject)
    {
        return () -> {
            final IGlobeDelegate globeDelegate = globeManager.getLastFocusedGlobe();

            final ILocation location = mapObject.getLocation();
            //noinspection DataFlowIssue
            mapObject.getController().createPopupMenu(globeDelegate, mapObject.getLocation());
            globeDelegate.goTo(location);
        };
    }
}
