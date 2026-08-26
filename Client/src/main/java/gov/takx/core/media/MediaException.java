package gov.takx.core.media;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An exception thrown during media setup or processing.
 *
 * @since 5.1.0
 */
public class MediaException extends Exception
{
    /**
     * Create an exception with the given message.
     *
     * @param message A message describing the exception.
     */
    public MediaException(@Nonnull String message)
    {
        super(message);
    }

    /**
     * Create an exception with the given message, and an underlying cause.
     *
     * @param message A message describing the exception
     * @param cause   The underlying cause for the exception.
     */
    public MediaException(@Nonnull String message, @Nullable Throwable cause)
    {
        super(message, cause);
    }
}
