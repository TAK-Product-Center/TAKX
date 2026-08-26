package gov.takx.api.query;

/**
 * Exception thrown to indicate that an invalid {@link FieldSelector} was detected.  This is caused when the
 * selector contains extended field names but has no structure defined by {@link FieldSelector#setStructure(String)}.
 *
 * @since 1.24
 */
public class InvalidFieldSelectorException extends Exception
{
    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public InvalidFieldSelectorException(String msg)
    {
        super(msg);
    }
}
