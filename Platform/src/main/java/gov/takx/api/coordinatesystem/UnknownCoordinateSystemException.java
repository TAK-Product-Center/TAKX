package gov.takx.api.coordinatesystem;

/**
 * Exception used to indicate an unknown coordinate system was attempted to be resolved.
 *
 */
public class UnknownCoordinateSystemException extends Exception
{
    public UnknownCoordinateSystemException(String coordinateSystem)
    {
        super(coordinateSystem);
    }
}
