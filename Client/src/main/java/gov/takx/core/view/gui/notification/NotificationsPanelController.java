package gov.takx.core.view.gui.notification;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import gov.takx.api.notification.Notification.Type;
import gov.takx.core.notification.NotificationAdded;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import javax.swing.Icon;

import java.awt.Color;
import java.util.Map;

/**
 * Manages a {@link NotificationPanel}.
 *
 * @since 5.2
 */
@ApplicationScoped
public class NotificationsPanelController
{
    // Set colors before creating icons
    private static final Map<Type, Color> COLORS = Map.of(
            Type.Info, Color.decode("#0288d1"),
            Type.Success, Color.decode("#2e7d32"),
            Type.Warning, Color.decode("#ed6c02"),
            Type.Error, Color.decode("#d32f2f"));

    // These may eventually be dynamic, don't put them in the Notification.Type enum
    private static final Map<Type, Icon> ICONS = Map.of(
            Type.Info, createIcon(Type.Info),
            Type.Success, createIcon(Type.Success),
            Type.Warning, createIcon(Type.Warning),
            Type.Error, createIcon(Type.Error));

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
        final var notification = event.notification();
        if (notification.logged())
        {
            notificationsPanel.addEntry(new NotificationEntry(notification.message(), ICONS.get(notification.type()), event.instant()));
        }
    }

    /**
     * @param type The notification type
     * @return A new instance of the notification type's icon
     */
    @Nonnull
    private static Icon createIcon(@Nonnull Type type)
    {
        final var colorFilter = new FlatSVGIcon.ColorFilter();
        colorFilter.add(new Color(150, 150, 150), COLORS.get(type));

        final var url = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS,
                "notification_" + type.name().toLowerCase() + ".svg", true);
        final var svgIcon = new FlatSVGIcon(url);
        svgIcon.setColorFilter(colorFilter);

        return svgIcon;
    }
}
