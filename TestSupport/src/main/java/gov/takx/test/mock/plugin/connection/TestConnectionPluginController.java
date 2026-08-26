package gov.takx.test.mock.plugin.connection;

import gov.takx.api.plugin.connection.AConnectionPluginController;
import gov.takx.api.plugin.connection.ConnectionState;

import java.util.Map;

/**
 * A simple connection plugin controller for integration testing.
 *
 * @since 2.0.0
 */
public class TestConnectionPluginController extends AConnectionPluginController
{
    /**
     * Generates a unique key based on the family and type.  Also, defines the panel label to be the type, and declares
     * that auto-reconnection is not supported by this controller.
     *
     * @param family Connection family name
     * @param type   Connection type name
     */
    protected TestConnectionPluginController(String family, String type)
    {
        super(family, type);
    }

    @Override
    public void onNewConnection()
    {
        delegate.setConnectionState(ConnectionState.Connected);
    }

    @Override
    public void onOpenConnection(Map<String, Object> configuration, boolean autoStart)
    {
        if (autoStart)
        {
            delegate.setConnectionState(ConnectionState.Connected);
        }
    }

    @Override
    public void onCloseConnection()
    {
        delegate.setConnectionState(ConnectionState.Disconnected);
    }

    @Override
    public void onShutdown()
    {

    }
}
