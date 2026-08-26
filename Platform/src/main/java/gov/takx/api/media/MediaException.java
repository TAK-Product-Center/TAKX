package gov.takx.api.media;

import jakarta.annotation.Nonnull;

/**
 * An exception thrown during media setup or processing.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
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
}
