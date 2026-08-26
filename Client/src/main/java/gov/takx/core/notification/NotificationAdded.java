package gov.takx.core.notification;

import gov.takx.api.notification.Notification;
import jakarta.annotation.Nonnull;

import java.time.Instant;

/**
 * Event fired when a new {@link Notification} is added.
 *
 * @param notification The notification that was added to the UI
 * @param instant The time the notification was added
 * @since 5.2
 */
public record NotificationAdded(@Nonnull Notification notification, @Nonnull Instant instant)
{
}
