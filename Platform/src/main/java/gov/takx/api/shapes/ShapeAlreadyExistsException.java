package gov.takx.api.shapes;

/**
 * This exception is thrown when a request is made to create a new shape and the shape name is already
 * in use.
 *
 */
public class ShapeAlreadyExistsException extends Exception
{

    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public ShapeAlreadyExistsException(String msg)
    {
        super(msg);
    }
}
