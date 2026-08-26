package gov.takx.api.globe;

/**
 * Exception that is issued when invalid map data, either imagery or elevation, is selected for conversion into a layer.
 * The data may be invalid because it is not proper map data or because it is in a format that is not handled.
 *
 * @since 1.49
 */
public class InvalidMapDataException extends Exception
{
    public InvalidMapDataException(String message)
    {
        super(message);
    }
}
