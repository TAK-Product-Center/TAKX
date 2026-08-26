package gov.takx.core.database.exception;

/**
 * Exceptional condition indicating a fatal persistence layer I/O exception.
 *
 * @since 1.4
 */
public class DatabaseIOException extends RuntimeException
{
    public DatabaseIOException(String msg, Throwable e)
    {
        super(msg, e);
    }

    public DatabaseIOException(String msg)
    {
        super(msg);
    }

    public DatabaseIOException(Throwable e)
    {
        super(e);
    }
}
