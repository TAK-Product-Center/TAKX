package gov.takx.api.messages;

/**
 * An exception class used to indicate that an error occurred during the setting or getting of a field within an
 * {@link IRaptorDataMessage} Structure.  Typically, because a named field did not exist on the structure
 * defining the message being accessed.
 *
 */
public class StructureAccessException extends RuntimeException
{
    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public StructureAccessException(String msg)
    {
        super(msg);
    }
}
