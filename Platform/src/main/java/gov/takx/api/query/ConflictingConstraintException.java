package gov.takx.api.query;

/**
 * An exception class used to indicate that an attempt to add a conflicting constraint to an
 * {@link IQueryConstraints} instance was made.
 *
 */
public class ConflictingConstraintException extends Exception
{

    /**
     * Construct the exception.
     *
     * @param msg Message
     */
    public ConflictingConstraintException(String msg)
    {
        super(msg);
    }
}
