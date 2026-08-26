package gov.tak.core.comms.takserver;

import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

/**
 * Various utilities related to TAK Server operations
 * @since 5.2
 */
public class TakServerUtilities
{
    /**
     * Determine if the root cause of a connection error is something we can recognize and notify the user.
     * @param notificationService The {@link INotificationService} to use.
     * @param connectionName The name of the TAK Server connection to report the error on.
     * @param throwable The error thrown that we are checking on.
     * @param errorMessage An optional message about what this error caused to fail
     */
    public static void handleConnectionError(@Nonnull INotificationService notificationService, @Nonnull String connectionName, @Nonnull Throwable throwable, @Nullable String errorMessage)
    {
        Throwable rootCause = throwable;
        while (rootCause != null)
        {
            rootCause = rootCause.getCause();
            if (rootCause instanceof UnknownHostException)
            {
                notificationService.show(Notification.builder("Host network interface down, please check internet connection",
                        Notification.Type.Error).build());
                break;
            } else if (rootCause instanceof NoRouteToHostException)
            {
                notificationService.show(Notification.builder("Target server unreachable, please check network route to: " + connectionName,
                        Notification.Type.Error).build());
                break;
            }
        }
        if (!StringUtils.isEmpty(errorMessage)) notificationService.show(Notification.builder(errorMessage, Notification.Type.Error).build());
    }

    private TakServerUtilities()
    {
        // No instances
    }
}
