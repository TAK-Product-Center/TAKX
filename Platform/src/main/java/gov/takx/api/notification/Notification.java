package gov.takx.api.notification;

import com.google.auto.value.AutoBuilder;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * Defines a notification to be displayed to a user. Use a {@link Builder} to create new messages. A message String is
 * required for all Notifications. Defaults will be used for all other options if they are not explicitly set.
 *
 * @param message Text of message to display
 * @param type Message type
 * @param logged Pass {@code true} to have the message recorded in the Notifications panel, {@code false} to not
 *
 * @see INotificationService
 * @since 5.2 (logged added in 5.8)
 */
@IncubatingApi(since = "5.2")
public record Notification(@Nonnull String message, @Nonnull Type type, boolean logged)
{
    /**
     * Constructor defaulting the {@link #logged} value to {@code true}.
     * @since 5.8
     */
    public Notification(@Nonnull String message, @Nonnull Type type)
    {
        this(message, type, true);
    }

    /**
     * The {@link Notification} types. Notifications are styled according to their types (color, icon)
     */
    public enum Type
    {
        Info,
        Success,
        Warning,
        Error
    }

    /**
     * @param message The notification message
     * @return A new Builder initialized with the provided message and default values
     */
    @Nonnull
    public static Builder builder(@Nonnull String message)
    {
        return new AutoBuilder_Notification_Builder()
                .setLogged(true)
                .setMessage(message)
                .setType(Type.Info); // initialize with default
    }

    /**
     * @param message The notification message
     * @param type The notification {@link Type}
     * @return A new Builder initialized with the provided message and {@link Type}
     */
    public static Builder builder(@Nonnull String message, @Nonnull Type type)
    {
        return new AutoBuilder_Notification_Builder()
                .setLogged(true)
                .setMessage(message)
                .setType(type);
    }

    @SuppressWarnings("NullableProblems")
    @AutoBuilder
    public abstract static class Builder
    {
        /**
         * @param message The notification message
         * @return This builder
         */
        @Nonnull
        public abstract Builder setMessage(@Nonnull String message);

        /**
         * @param type The notification type
         * @return This builder
         */
        @Nonnull
        public abstract Builder setType(@Nonnull Type type);

        /**
         * @param logged Pass {@code true} to have the notification logged in the Notifications panel, {@code false} to not
         * @return This builder
         * @since 5.8
         */
        @Nonnull
        public abstract Builder setLogged(boolean logged);

        /**
         * @return A new notification
         */
        @Nonnull
        public abstract Notification build();
    }
}
