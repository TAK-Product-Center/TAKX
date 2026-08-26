package gov.takx.api.messages;

/**
 * An exception used to indicate that the specified structure cannot be found.  Presently the Query service and the
 * Device Gateway mechanisms (command messages, data messages, etc.) will throw this exception instead of returning
 * a {@code null}.
 * <p/>
 * While not an encouraged practice, this exception extends RuntimeException so that plugin developers are not forced
 * to catch this exception every time they need to lookup a structure if they are confident that their structures
 * exist.
 *
 */
public class NoSuchStructureException extends RuntimeException
{
    private String structure;

    /**
     * Construct the exception.
     *
     * @param structure Name of the structure that couldn't be found
     */
    public NoSuchStructureException(String structure)
    {
        super("Unable to find a structure named '" + structure + "'");
        this.structure = structure;
    }

    /**
     * Construct the exception.
     *
     * @param structure Name of the structure that couldn't be found
     */
    public NoSuchStructureException(String structure, Throwable causedBy)
    {
        super("Unable to find a structure named '" + structure + "'", causedBy);
        this.structure = structure;
    }

    /**
     * Identifies the name of the structure that couldn't be found.
     *
     * @return Name of the missing structure
     */
    public String getStructureName()
    {
        return structure;
    }
}
