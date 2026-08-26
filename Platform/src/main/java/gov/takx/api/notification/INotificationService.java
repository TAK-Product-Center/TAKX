package gov.takx.api.notification;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Contract for a service that displays notifications.
 *
 * @see Notification
 * @since 5.2
 */
@InjectableService
@NoPluginImplementations
@IncubatingApi(since = "5.2")
public interface INotificationService
{
    @SuppressWarnings("unused")
    long MANUAL_CLOSE = Long.MAX_VALUE;

    /**
     * Displays a notification for the configured duration.
     *
     * @param notification The notification to display
     * @see gov.takx.platform.preferences.PreferenceEntry#NOTIFICATIONS_DURATION
     */
    void show(@Nonnull Notification notification);

    /**
     * Displays the provided notification for the specified duration.
     *
     * @param notification The notification to display
     * @param duration The length of time to display the notification, in milliseconds. Pass {@link #MANUAL_CLOSE} to
     *                 keep the notification open until closed by the user.
     */
    void show(@Nonnull Notification notification, long duration);
}
