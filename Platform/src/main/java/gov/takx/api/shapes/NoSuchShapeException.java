package gov.takx.api.shapes;

/**
 * This exception is thrown when a request is made to delete a shape and the named shape doesn't exist.
 *
 */
public class NoSuchShapeException extends Exception
{

    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public NoSuchShapeException(String msg)
    {
        super(msg);
    }
}
