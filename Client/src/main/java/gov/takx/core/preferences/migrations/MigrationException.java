package gov.takx.core.preferences.migrations;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Specialized exception for any failure while applying migrations.
 *
 * @since 5.1
 */
public class MigrationException extends Exception
{
    /**
     * @param message Detail message
     */
    public MigrationException(@Nonnull String message)
    {
        super(message, null);
    }

    /**
     * @param message Detail message
     * @param cause root cause, may be {@code null}
     */
    public MigrationException(@Nonnull String message, @Nullable Throwable cause)
    {
        super(message, cause);
    }
}
