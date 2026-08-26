package gov.takx.api.query;

/**
 * An exception used to indicate that the specified field has a type that is not supported for use in field constraints.
 * <p/>
 * Note: this is a subtype of RuntimeException (instead of a checked exception) in order to avoid breaking the API.
 *
 */
public class InvalidFieldTypeException extends RuntimeException
{
    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public InvalidFieldTypeException(String msg)
    {
        super(msg);
    }
}
