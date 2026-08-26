package gov.takx.core.connection;

import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.NoSuchConnectionControllerException;
import gov.takx.api.plugin.connection.TooManyConnectionsException;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.connection.TestConnectionPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit test for {@link gov.takx.core.connection.ConnectionManager}.
 */
public class ConnectionManagerTest extends AIntegrationTest
{
    private final IConnectionManager connectionManager;

    public ConnectionManagerTest()
    {
        setPluginsToRegister(TestConnectionPlugin.class);

        connectionManager = getBean(IConnectionManager.class);
    }

    /**
     * Remove any connections left over from previous tests.
     */
    @AfterEach
    public void cleanup()
    {
        Collection<IComposedConnectionController> connectionControllers = connectionManager.getConnectionControllers();

        for (IComposedConnectionController connectionController : connectionControllers)
        {
            connectionManager.removeConnection(connectionController.getUniqueKey(), true);
        }
    }

    /**
     * Verifies that a connection is created when a plugin attempts to add it via the IConnectionManagerDelegate API.
     */
    @Test
    public void testAddConfiguredConnection() throws TooManyConnectionsException
    {
        connectionManager.createAndAddConfiguredConnection(TestConnectionPlugin.FAMILY, TestConnectionPlugin.TYPE, null, true);

        assertThat(connectionManager.getConnectionCount(TestConnectionPlugin.FAMILY, TestConnectionPlugin.TYPE)).withFailMessage("Connection missing").isEqualTo(1);
        assertThat(connectionManager.getConnectionControllers()).withFailMessage("Connection controller missing").hasSize(1);

        connectionManager.getConnectionControllers().forEach(IComposedConnectionController::removeConnection);
        assertThat(connectionManager.getConnectionControllers()).withFailMessage("Connection controllers were not all removed").isEmpty();
    }

    /**
     * Verifies that an existing connection can be reopened when a plugin attempts to open it via the
     * IConnectionManagerDelegate API.
     */
    @Test
    public void testOpenConfiguredConnection() throws TooManyConnectionsException, NoSuchConnectionControllerException
    {
        // Add the connection and disconnect it.
        String uniqueKey = createTestConnection();

        assertThat(connectionManager.getConnectionCount(TestConnectionPlugin.FAMILY, TestConnectionPlugin.TYPE)).withFailMessage("Connection missing").isEqualTo(1);
        assertThat(connectionManager.getConnectionControllers()).withFailMessage("Connection controller missing").hasSize(1);

        IComposedConnectionController controller = connectionManager.getControllerFromConnectionKey(uniqueKey);
        controller.setConnectionState(ConnectionState.Disconnected);

        connectionManager.openConfiguredConnection(uniqueKey, null, true);

        assertThat(controller.getState()).withFailMessage("Connection not re-opened").isEqualTo(ConnectionState.Connected);
        assertThat(connectionManager.isManagedConnection(uniqueKey)).withFailMessage("Connection not managed").isTrue();

        controller.removeConnection();
        assertThat(connectionManager.getConnectionControllers()).withFailMessage("Connection controllers were not all removed").isEmpty();
        assertThat(connectionManager.isManagedConnection(uniqueKey)).withFailMessage("Connection still managed").isFalse();
    }

    /**
     * Test the creating of a controller, adding a connection and removing a connection and the affect that each
     * has on whether or not the connection manager tracks the connection as being managed.  A connection should not
     * be deemed managed until added as such to the connection manager.
     *
     * @since 2.36
     */
    @Test
    public void testCreateRemoveManaged()
    {
        try
        {
            IComposedConnectionController controller = connectionManager.createConnectionController(TestConnectionPlugin.FAMILY, TestConnectionPlugin.TYPE);
            assertThat(connectionManager.isManagedConnection(controller.getUniqueKey())).withFailMessage("Connection went managed too early").isFalse();
            controller.removeConnection();  // test remove connection that is not managed

            controller = connectionManager.createConnectionController(TestConnectionPlugin.FAMILY, TestConnectionPlugin.TYPE);
            connectionManager.addActiveConnection(controller);
            assertThat(connectionManager.isManagedConnection(controller.getUniqueKey())).withFailMessage("Connection not managed").isTrue();
            controller.removeConnection();
            assertThat(connectionManager.isManagedConnection(controller.getUniqueKey())).withFailMessage("Connection still managed").isFalse();
        } catch (TooManyConnectionsException e)
        {
            fail("Error creating IComposedConnectionController", e);
        }
    }

    /**
     * Verifies that an exception occurs when a bad unique key is provided for opening an existing connection.
     */
    @Test
    public void testOpenConfiguredConnection_badUniqueKey()
    {
        String uniqueKey = "junk";
        boolean exceptionThrown = false;

        try
        {
            connectionManager.openConfiguredConnection(uniqueKey, null, true);
        } catch (NoSuchConnectionControllerException e)
        {
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).withFailMessage("Exception not thrown for bad unique key").isTrue();
        assertThat(connectionManager.isManagedConnection(uniqueKey)).withFailMessage("Invalid connection key being reported as being associated with an managed connection").isFalse();
    }

    /**
     * Creates a connection of the specified family and type.
     *
     * @return The unique key of the connection controller
     * @throws TooManyConnectionsException if the maximum number of connections has already been reached
     * @since 2.18
     */
    private String createTestConnection() throws TooManyConnectionsException
    {
        IComposedConnectionController controller = connectionManager.createConnectionController(TestConnectionPlugin.FAMILY, TestConnectionPlugin.TYPE);
        connectionManager.addActiveConnection(controller);
        return controller.getUniqueKey();
    }
}
