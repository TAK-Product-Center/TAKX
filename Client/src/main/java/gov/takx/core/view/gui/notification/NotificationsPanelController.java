package gov.takx.core.view.gui.notification;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import gov.takx.api.notification.Notification;
import gov.takx.core.notification.NotificationAdded;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import javax.swing.Icon;

import java.awt.Color;

/**
 * Manages a {@link NotificationPanel}.
 *
 * @since 5.2
 */
@ApplicationScoped
public class NotificationsPanelController
{
    // Set colors before creating icons
    private static final Color INFO_COLOR = Color.decode("#0288d1");
    private static final Color SUCCESS_COLOR = Color.decode("#2e7d32");
    private static final Color WARNING_COLOR = Color.decode("#ed6c02");
    private static final Color ERROR_COLOR = Color.decode("#d32f2f");

    // These may eventually be dynamic, don't put them in the Notification.Type enum
    private static final Icon INFO_ICON = createIcon(Notification.Type.Info);
    private static final Icon SUCCESS_ICON = createIcon(Notification.Type.Success);
    private static final Icon WARN_ICON = createIcon(Notification.Type.Warning);
    private static final Icon ERROR_ICON = createIcon(Notification.Type.Error);

    private final NotificationsPanel notificationsPanel = new NotificationsPanel();

    /**
     * @return The panel managed by this controller
     */
    public ATitledPanel getPanel()
    {
        return notificationsPanel;
    }

    @SuppressWarnings("unused")
    private void onNotificationAdded(@Observes NotificationAdded event)
    {
        final Notification notification = event.notification();
        notificationsPanel.addEntry(new NotificationEntry(notification.message(), getIcon(notification.type()), event.instant()));
    }

    /**
     * @param type The notification type
     * @return The notification type's icon
     */
    @Nonnull
    private Icon getIcon(@Nonnull Notification.Type type)
    {
        return switch (type)
        {
            case Info -> INFO_ICON;
            case Success -> SUCCESS_ICON;
            case Warning -> WARN_ICON;
            case Error -> ERROR_ICON;
        };
    }

    /**
     * @param type The notification type
     * @return A new instance of the notification type's icon
     */
    @Nonnull
    private static Icon createIcon(@Nonnull Notification.Type type)
    {
        final var colorFilter = new FlatSVGIcon.ColorFilter();
        colorFilter.add(new Color(150, 150, 150), getColor(type));

        final var url = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS,
                "notification_" + type.name().toLowerCase() + ".svg", true);
        final var svgIcon = new FlatSVGIcon(url);
        svgIcon.setColorFilter(colorFilter);

        return svgIcon;
    }

    /**
     * @param type The notification type
     * @return The notification type's color
     */
    @Nonnull
    private static Color getColor(@Nonnull Notification.Type type)
    {
        return switch (type)
        {
            case Info -> INFO_COLOR;
            case Success -> SUCCESS_COLOR;
            case Warning -> WARNING_COLOR;
            case Error -> ERROR_COLOR;
        };
    }
}
