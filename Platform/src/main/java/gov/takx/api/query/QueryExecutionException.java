package gov.takx.api.query;

/**
 * An exception class used to indicate that an error occurred during the execution of a query.  The underlying
 * cause may be determined by calling {@link #getCause()}.
 *
 */
public class QueryExecutionException extends RuntimeException
{
    /**
     * Construct the exception.
     *
     * @param msg   Message
     * @param cause Cause of exception
     */
    public QueryExecutionException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public QueryExecutionException(String msg)
    {
        super(msg);
    }
}
