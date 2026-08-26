package gov.takx.api.shapes;

/**
 * This exception is thrown when a request is made to define a shape filter builder with a value that is not appropriate
 * to the field it corresponds to. For a list of valid fields and their corresponding accepted value types
 * see {@link IShapeFilter}
 *
 */
public class InvalidShapeFilterException extends Exception
{

    /**
     * Construct the exception.
     *
     * @param msg the message to display when the InvalidShapeFilterException is thrown.
     */
    public InvalidShapeFilterException(String msg)
    {
        super(msg);
    }
}
