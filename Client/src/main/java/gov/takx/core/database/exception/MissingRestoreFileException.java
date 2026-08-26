package gov.takx.core.database.exception;

/**
 * Exceptional condition used to indicate that the expected restore file cannot be located.
 *
 * @since 1.4
 */
public class MissingRestoreFileException extends RuntimeException
{
    public MissingRestoreFileException(String msg)
    {
        super(msg);
    }
}
