package gov.takx.api.plugin.connection;

/**
 * Internal core identifier of a connection state.
 *
 * @since 1.6
 */
public enum ConnectionState
{
    Connected,
    Connecting,
    Error,
    Disconnected;

    public static ConnectionState getState(boolean isConnected)
    {
        if (isConnected)
        {
            return Connected;
        } else
        {
            return Disconnected;
        }
    }
}
