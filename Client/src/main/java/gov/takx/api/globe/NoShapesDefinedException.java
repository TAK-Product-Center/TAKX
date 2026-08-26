package gov.takx.api.globe;

/**
 * Exception thrown during the creation of a new globe layer when the shape file contains no shapes.
 *
 */
public class NoShapesDefinedException extends RuntimeException
{
    /**
     * Default constructor.
     */
    public NoShapesDefinedException()
    {
        super("No shapes defined");
    }
}
