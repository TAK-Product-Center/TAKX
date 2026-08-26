package gov.takx.core.notification;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raven.toast.Notifications;
import raven.toast.ToastClientProperties;
import raven.toast.ui.ToastNotificationPanel;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import java.awt.Font;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.EnumMap;

/**
 * Service that displays {@link Notification Notifications} to users.
 * 
 * @since 5.2
 */
@ApplicationScoped
public class NotificationService implements INotificationService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Maps our local Notification.Type to its Raven Notifications.Type. We don't want our API dependent on the library
    private static final EnumMap<Notification.Type, Notifications.Type> TYPE_MAP = new EnumMap<>(Notification.Type.class);

    static
    {
        TYPE_MAP.put(Notification.Type.Info, Notifications.Type.INFO);
        TYPE_MAP.put(Notification.Type.Success, Notifications.Type.SUCCESS);
        TYPE_MAP.put(Notification.Type.Warning, Notifications.Type.WARNING);
        TYPE_MAP.put(Notification.Type.Error, Notifications.Type.ERROR);
    }

    private static final Notifications.Location LOCATION = Notifications.Location.BOTTOM_RIGHT;

    private final Notifications ravenNotificationService = new TakxNotifications();
    private final Event<NotificationAdded> notificationAddedEvent;
    private final boolean headless;

    @Inject
    NotificationService(Event<NotificationAdded> notificationAddedEvent, @Headless boolean headless)
    {
        this.notificationAddedEvent = notificationAddedEvent;
        this.headless = headless;

        // Configure the Raven Toast library
        UIManager.put("Toast.maximumWidth", 600);
        UIManager.put("Toast.closeIcon", UIResources.CANCEL_ICON);
        UIManager.put("Toast.outlineWidth", 1);

        if (!headless) ravenNotificationService.setJFrame((JFrame) JFrame.getFrames()[0]);
    }

    @Override
    public void show(@Nonnull Notification notification)
    {
        show(notification, PreferenceEntry.NOTIFICATIONS_DURATION.getIntegerValue());
    }

    @Override
    public void show(@Nonnull Notification notification, long duration)
    {
        final Notification.Type type = notification.type();
        final String message = notification.message();
        final boolean showPopup = !headless && PreferenceEntry.NOTIFICATIONS_POPUPS_ENABLED.getBooleanValue();

        logger.info("{}: \"{}\" ({})", type.name(), message, showPopup ? duration + "ms" : "popups disabled");
        notificationAddedEvent.fire(new NotificationAdded(notification, Instant.now()));

        if (showPopup) showPopup(duration, type, message);
    }

    /** @since 5.3 */
    @InvokeOnEDT
    private void showPopup(long duration, Notification.Type type, String message)
    {
        ravenNotificationService.show(TYPE_MAP.get(type), LOCATION, duration, message);
    }

    /**
     * {@link Notifications} subclass that customizes the look of notifications.
     */
    private static class TakxNotifications extends Notifications
    {
        private Font customFont;

        @Override
        protected ToastNotificationPanel createNotification(Type type, String message)
        {
            final var toastNotificationPanel = super.createNotification(type, message);

            setCustomFont(toastNotificationPanel);

            return toastNotificationPanel;
        }

        private void setCustomFont(@Nonnull ToastNotificationPanel panel)
        {
            final var textPane = (JTextPane) panel.getClientProperty(ToastClientProperties.TOAST_COMPONENT);

            if (customFont == null)
            {
                final var font = textPane.getFont();
                customFont = font.deriveFont(font.getSize() + 1f); // bump up font size
            }

            textPane.setFont(customFont);
        }
    }
}
