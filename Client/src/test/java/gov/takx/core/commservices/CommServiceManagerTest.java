package gov.takx.core.commservices;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.plugin.commservices.ICommManager;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.connection.ICommServiceDelegate;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Unit Tests for {@link CommServiceManager}.
 * 
 * @since 3.4
 */
public final class CommServiceManagerTest extends AIntegrationTest
{
    private final ICommServiceManager commServiceManager;
    private final ICommServiceDelegate commServiceDelegate;

    public CommServiceManagerTest()
    {
        commServiceManager = getBean(ICommServiceManager.class);
        commServiceDelegate = getBean(ICommServiceDelegate.class);
    }

    @Test
    public void test_default_serial_port_rx_timeout() throws IOException
    {
        SerialCommService commService = (SerialCommService) commServiceManager.createSerialCommPath("fred", 4800, false, "format");
        assertThat(commService.getProperties().containsKey(CommServiceConstants.RX_TIMEOUT.toString())).isTrue();
        assertThat(commService.getProperties().get(CommServiceConstants.RX_TIMEOUT.toString())).isEqualTo(0);
    }

    @Test
    public void test_set_serial_port_rx_timeout() throws IOException
    {
        SerialCommService commService = (SerialCommService) commServiceManager.createSerialCommPath("wilma", 4800, false, "echo", "format", 0, 100);
        assertThat(commService.getProperties().containsKey(CommServiceConstants.RX_TIMEOUT.toString())).isTrue();
        assertThat(commService.getProperties().get(CommServiceConstants.RX_TIMEOUT.toString())).isEqualTo(100);
    }

    /**
     * Configures service with the 'SSL_SERVER_SOCKET' service type code path in CommServiceManager.createCommPath
     * after which validates service format, and type was set in manager
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenSslServerSocketCommType_createCommPath_opensSslServerSocket() throws IOException
    {
        String host = "127.0.0.1";
        int port = findOpenPort();
        String protocolFormat = "TCP";
        CommServiceType commType = CommServiceType.SSL_SERVER_SOCKET;

        commServiceManager.createCommPath(
                commType,
                (host + ':' + port),
                protocolFormat,
                8192,
                null);

        ICommPath commPath = commServiceManager.getServerSocketCommPath(port, InetAddress.getByName(host));
        assertThat(commPath).isNotNull();
        var propertiesMap = commPath.getCommPathProperties();
        assertThat(propertiesMap.get("PORT")).isEqualTo(String.valueOf(port));
        assertThat(propertiesMap.get("TYPE")).isEqualTo(commType.toString());
        assertThat(propertiesMap.get("FORMAT")).isEqualTo(protocolFormat);
    }

    /**
     * Tests 'SERIAL' service type code path in CommServiceManager.createCommPath, validates
     * comm path, com type was set in manager and service (using getCommService)
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenSerialCommType_createSerialCommPath_opensSerialComm() throws IOException
    {
        String commPathName = "COM1";
        String protocolFormat = "Serial";
        CommServiceType commType = CommServiceType.SERIAL;
        String echoPort = String.valueOf(findOpenPort());
        commServiceManager.createSerialCommPath(
                commPathName,
                4800,
                false,
                echoPort,
                protocolFormat);

        // get the service and verify type, name in property / settings
        validateServiceSettings(commPathName, commType, protocolFormat);
    }

    /**
     * Negative test -goal is attempt ethernet type/ CLIENT_SOCKET connection to nonexistent
     * service, and since that socket requires a connection, we expect IOException and null return.
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenNoServerSocketOpen_createClientSocketCommPath_throws() throws IOException
    {
        String host = "127.0.0.1";
        int port = findOpenPort();
        String commPathName = host + ":" + port;
        String protocolFormat = "TCP";
        CommServiceType commType = CommServiceType.CLIENT_SOCKET;

        assertThatIOException().isThrownBy(() -> {
            commServiceManager.createCommPath(
                    commType,
                    commPathName,
                    protocolFormat,
                    8192,
                    null);

            var service = commServiceManager.getCommService(commType, commPathName);
            assertThat(service).isNotNull();
            commServiceManager.addCommService(service);
        });
        assertThat(commServiceManager.getCommService(commType, commPathName)).isNull();
    }

    /**
     * happy path client socket type will connect to server socket opened by test
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenSocketOpen_createClientSocketCommPath_opensEthernetSocket() throws IOException
    {
        String host = "127.0.0.1";
        int port = findOpenPort();
        String commPathName = host + ":" + port;
        String protocolFormat = "TCP";
        CommServiceType commType = CommServiceType.CLIENT_SOCKET;

        ServerSocket serverSocket = new ServerSocket(port);
        commServiceManager.createCommPath(
                commType,
                commPathName,
                protocolFormat,
                8192,
                null);

        var service = commServiceManager.getCommService(commType, commPathName);
        assertThat(service).isNotNull();
        assertThat(service.isCommOpen()).isTrue();
        validateServiceSettings(commPathName, commType, protocolFormat);

        serverSocket.close();
    }

    /**
     * Tests 'DATAGRAM_SOCKET' service type code path in CommServiceManager.createCommPath, validates
     * com name, com type was set in manager and service (using getCommService)
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenDatagramClientType_createCommPath_opensDatagramSocket() throws IOException
    {
        String host = "127.0.0.1";
        int port = findOpenPort();
        String commPathName = host + ":" + port;
        String protocolFormat = "UDP";
        CommServiceType commType = CommServiceType.DATAGRAM_CLIENT;
        ICommManager commManager = commServiceManager.createCommPath(
                commType,
                commPathName,
                protocolFormat,
                8192,
                null);

        validateCommManagerSettings(commManager, commPathName);
        validateServiceSettings(commPathName, commType, protocolFormat);
        var service = commServiceManager.getCommService(commType, commPathName);
        assertThat(service.isCommOpen()).isTrue();
    }

    /**
     * Tests 'NONE' service type code path in CommServiceManager.createCommPath, validates
     * properties i.e. comm path, com type was set in manager and service (using getCommService)
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenNoneCommType_createCommPath_returnsNullCommService() throws IOException
    {
        String commPathName = "none";
        CommServiceType commType = CommServiceType.NONE;
        String protocolFormat = "TCP";
        ICommManager commManager = commServiceManager.createCommPath(
                commType,
                commPathName,
                protocolFormat,
                8192,
                null);

        validateCommManagerSettings(commManager, commPathName);
        validateServiceSettings(commPathName, commType, protocolFormat);
    }

    /**
     * Passes a new service into commServiceManager's createCommPath, gets the service again with commServiceManager
     * verifies properties of a service, after calling startCommService with commServiceManager
     */
    @SuppressLogging(ACommService.class)
    @Test
    void whenCustomServiceType_createCommPath_createsCustomCommService() throws IOException
    {
        String host = "127.0.0.1";
        int port = findOpenPort();
        String commPathName = host + ":" + port;
        CommServiceType commType = CommServiceType.CUSTOM;
        String protocolFormat = "TCP";
        commServiceDelegate.createCommPath(new CustomTesterCommService(commType, commPathName));

        Map<String, Serializable> props = new HashMap<>();
        props.put(CommServiceConstants.PORT_TYPE.toString(), commType.toString());
        props.put(CommServiceConstants.PORT_NAME.toString(), commPathName);
        props.put(CommServiceConstants.PORT_FORMAT.toString(), protocolFormat);
        ACommService testService = (ACommService) commServiceManager.getCommService(commType, commPathName);

        assertThat(testService).isNotNull();
        testService.startCommService(props);

        // get the service and verify type, name in property / settings
        validateServiceSettings(commPathName, commType, protocolFormat);
    }

    private void validateCommManagerSettings(ICommManager commManager, String commPathName)
    {
        // for most of these protocols the commServiceManager is not null and populated
        // with configs relating to our service path
        assertThat(commManager).isNotNull();
        assertThat(commManager.getName()).isEqualTo(commPathName);
    }

    private void validateServiceSettings(String commPathName, CommServiceType commType, String protocolFormat)
    {
        // get the service and verify type, name in property / settings
        ICommService commService = commServiceManager.getCommService(commType, commPathName);

        assertThat(commService).isNotNull();
        assertThat(commService.getCommPathProperties().get("FORMAT")).isEqualTo(protocolFormat);
        assertThat(commService.getType()).isEqualTo(commType);
        assertThat(commService.getName()).isEqualTo(commPathName);
    }

    private int findOpenPort() throws IOException
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
    }
}
