package gov.takx.core.database.exception;

/**
 * Exceptional condition indicating that the ProgramProperties.xml incorrectly defines the path to the database
 * runtime environment, or that the XML completely omits the requisite database path key.
 *
 * @since 1.4
 */
public class MissingDatabasePathException extends RuntimeException
{
    public MissingDatabasePathException(String msg)
    {
        super(msg);
    }
}
