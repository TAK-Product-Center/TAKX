package gov.takx.core.view.gui.notification;

import jakarta.annotation.Nonnull;

import javax.swing.Icon;

import java.time.Instant;

/**
 * Holds the info required to display a {@link gov.takx.api.notification.Notification} to a user in the {@link NotificationPanel}.
 *
 * @param message The notification message
 * @param icon The notification icon
 * @param instant The time the notification was added
 * @since 5.2
 */
record NotificationEntry(@Nonnull String message, @Nonnull Icon icon, @Nonnull Instant instant)
{
}
