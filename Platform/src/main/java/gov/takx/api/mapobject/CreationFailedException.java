package gov.takx.api.mapobject;

/**
 * Exception thrown when the creation of a Map Object fails for any reason. Typically, that will be caused by
 * another exception, which will be available via {@link #getCause()}.
 *
 * @since 3.2
 */
public class CreationFailedException extends RuntimeException
{
    public CreationFailedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
