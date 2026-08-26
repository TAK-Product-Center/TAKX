package gov.takx.core.view.gui.contacts;

import gov.takx.api.ui.IUISpaceManager;
import gov.takx.core.view.gui.contacts.conversation.UnreadMessageIconUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.JPanel;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for updating the Contacts and Chats icons displayed on the toolbar to include the unread message count
 * when new chat messages are received.
 *
 * @since 3.2
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@ApplicationScoped
public class ContactsIconUpdater
{
    private final Map<Integer, BufferedImage> countToCountIconMap = new HashMap<>();
    private final List<PanelInfo> panelInfoList = new ArrayList<>();
    private final IUISpaceManager uiSpaceManager;

    @Inject
    ContactsIconUpdater(IUISpaceManager uiSpaceManager)
    {
        this.uiSpaceManager = uiSpaceManager;
    }

    /**
     * @param panel The panel to add. An unread message icon will be displayed on the panel's toolbar button and tab
     *              (if applicable) when there are unread messages
     * @param icon The panel's toolbar button icon
     * @param callback Optional callback that will be run when the unread message count changes
     * @since 5.2
     * @see IUISpaceManager#setComponentIconImage(Component, BufferedImage, BufferedImage)
     */
    public void addPanelListener(@Nonnull JPanel panel, @Nonnull BufferedImage icon, @Nullable Runnable callback)
    {
        panelInfoList.add(new PanelInfo(panel, icon, new HashMap<>(), callback));
    }

    /**
     * @param panel The panel to remove
     * @since 5.2
     */
    public void removePanelListener(@Nonnull JPanel panel)
    {
        final PanelInfo panelInfo = panelInfoList.stream().filter(pi -> pi.panel == panel).findFirst().orElse(null);

        if (panelInfo != null)
        {
            resetIcon(panelInfo);
            panelInfoList.remove(panelInfo);
        }
    }

    /**
     * Updates the toolbar button and component tab icons to indicate that there are unread messages. If there are any
     * unread messages, a badge will be added to the button icon that displays the total number of unread chat messages and
     * a notification dot will be added to the component tab icon.
     *
     * @param count The total number of unread chat messages
     */
    void updateUnreadMessageCount(final int count)
    {
        if (count > 0)
        {
            // Get the count icon
            final BufferedImage countIcon =
                    countToCountIconMap.computeIfAbsent(
                            Math.min(count, UnreadMessageIconUtils.MESSAGE_COUNT_LABEL_THRESHOLD), // everything over the max gets the same icon
                            UnreadMessageIconUtils::createCountImage);

            // Update each panel's button and tab icons with the count icon
            panelInfoList.forEach(panelInfo -> {
                final BufferedImage icon =
                        panelInfo.countToButtonIconMap
                                .computeIfAbsent(count, c -> UnreadMessageIconUtils.createOverlaidImage(panelInfo.icon, countIcon));
                uiSpaceManager.setComponentIconImage(panelInfo.panel, icon, UnreadMessageIconUtils.NOTIFICATION_DOT);
                if (panelInfo.callback != null) panelInfo.callback.run();
            });
        } else
        {
            // Reset each panel's button and tab icons
            panelInfoList.forEach(panelInfo -> {
                resetIcon(panelInfo);
                if (panelInfo.callback != null) panelInfo.callback.run();
            });
        }
    }

    /**
     * @param panelInfo The record containing the panel whose toolbar button icon should be restored to its default
     * @since 5.2
     */
    private void resetIcon(@Nonnull PanelInfo panelInfo)
    {
        uiSpaceManager.setComponentIconImage(panelInfo.panel, panelInfo.icon, null);
    }

    /**
     * Holds a {@link JPanel} whose toolbar and tab entries will be updated when the unread message count changes, an
     * optional callback that will be notified of changes, and the panel's button icon.
     *
     * @param panel The panel
     * @param countToButtonIconMap Map that will be used to store overlaid button icons as they are created for each count
     * @param callback The optional callback
     * @since 5.2
     */
    private record PanelInfo(@Nonnull JPanel panel, @Nonnull BufferedImage icon,
            @Nonnull Map<Integer, BufferedImage> countToButtonIconMap, @Nullable Runnable callback)
    {
    }
}
