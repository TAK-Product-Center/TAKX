package gov.takx.api.plugin.connection;

/**
 * Thrown to indicate that the request to launch a connection wizard via a plugin delegate has failed because the
 * {@link IConnectionPlugin#getMaxSupportedConnections()} threshold has already been
 * met and no new connections are possible.
 *
 * @since 1.14
 */
public class TooManyConnectionsException extends Exception
{
    private final String family;
    private final String type;
    private final int maxConnections;

    /**
     * @param family         Connection plugin family
     * @param type           Connection plugin type
     * @param message        Message to report
     * @param maxConnections Configured max number of connections
     * @since 2.20
     */
    public TooManyConnectionsException(String family, String type, String message, int maxConnections)
    {
        super(message);

        this.family = family;
        this.type = type;
        this.maxConnections = maxConnections;
    }

    /**
     * @return the connection plugin family
     */
    public String getFamily()
    {
        return family;
    }

    /**
     * @return the connection plugin type
     */
    public String getType()
    {
        return type;
    }

    /**
     * @return The configured maximum number of connections
     * @since 2.20
     */
    public int getMaxConnections()
    {
        return maxConnections;
    }
}
