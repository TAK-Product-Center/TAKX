package gov.takx.api.plugin.connection;

/**
 * Thrown to indicate that no such connection controller is defined within the connection manager.
 *
 * @since 1.6
 */
public class NoSuchConnectionControllerException extends Exception
{
    public NoSuchConnectionControllerException(String key)
    {
        super("No controller presently exists for key " + key);
    }
}
