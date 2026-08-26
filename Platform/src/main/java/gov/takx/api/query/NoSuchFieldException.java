package gov.takx.api.query;

/**
 * An exception used to indicate that the specified field can not be found in the message structure.
 *
 */
public class NoSuchFieldException extends Exception
{

    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public NoSuchFieldException(String msg)
    {
        super(msg);
    }
}
