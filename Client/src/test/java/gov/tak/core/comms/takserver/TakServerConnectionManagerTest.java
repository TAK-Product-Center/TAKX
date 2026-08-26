package gov.tak.core.comms.takserver;

import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.MethodBehavior;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.commo.CommonCommoManager;
import gov.takx.api.event.takserver.TakServerConnectionAdded;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TakServerConnectionManager.
 * @since 5.3
 */
public class TakServerConnectionManagerTest extends AContainerTest
{
    @Mocked(methodBehavior = MethodBehavior.RETURN_DEFAULT_VALUES)
    private Event<?> anyEvent;

    @Mocked
    private CommonCommoManager commonCommoManager;

    @Mocked
    private Event<TakServerConnectionAdded> connectionAdded;

    @Mocked
    private Event<TakServerConnectionRemoved> connectionRemoved;

    private TakServerConnectionManager connectionManager;

    private final URI uri = URI.create("ssl://yeti.pargovernment.net:8089");

    public TakServerConnectionManagerTest()
    {
        // TakServerConnectionManager must be updated for mocking
        var connectionManagerBean = getBean(TakServerConnectionManager.class);
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(connectionManagerBean));
        connectionManager = spy(connectionManagerBean);
    }

    @BeforeEach
    public void setup() throws IOException
    {
        setupMocks();
    }

    @Test
    public void addTakServerConnection()
    {
        UUID uuid = UUIDUtils.generateType1UUID();

        var takServerConnection = connectionManager.addTakServerConnection(setupBasicConnConfiguration(uuid, uri));
        verify(connectionAdded, atLeastOnce()).fire(any(TakServerConnectionAdded.class));

        // returned server conn's streamingConnectionConfig should match after connection built
        var streamingConnectionConfig = takServerConnection.getConnectionConfig();
        assertThat(streamingConnectionConfig.getId()).isEqualTo(uuid);
        assertThat(streamingConnectionConfig.getUri()).isEqualTo(uri);

        assertThat(connectionManager.findConnectionByName("Yeti")).isNotNull();
        assertThat(connectionManager.findConnectionByUuid(uuid)).isEqualTo(takServerConnection);

    }

    /**
     * Exercises {@link TakServerConnectionManager#updateTakServerConnection(UUID, StreamingConnectionConfiguration)}
     * verify StreamingConnectionConfiguration's settings match params given
     */
    @Test
    public void updateTakServerConnection()
    {
        UUID uuid = UUIDUtils.generateType1UUID();
        var streamingConnectionConfig = setupBasicConnConfiguration(uuid, uri);
        var takServerConnection = connectionManager.addTakServerConnection(streamingConnectionConfig);

        // modify the configuration and update the connection
        URI uri2 = URI.create("ssl://yeti2.net:8089");
        var modifiedStreamConnConfig = new StreamingConnectionConfiguration(uuid);
        modifiedStreamConnConfig.setUri(uri2);

        // takserverconnectionmanager's method should find difference that I added in comparing configurations
        assertThat(connectionManager.isNewConnectionNeeded(streamingConnectionConfig, modifiedStreamConnConfig)).isTrue();
        connectionManager.updateTakServerConnection(uuid, modifiedStreamConnConfig);

        // re-getting the configuration from connection, after update URI should have changed to URI2
        var returnedStreamingConnectionConfig = takServerConnection.getConnectionConfig();
        assertThat(returnedStreamingConnectionConfig.getId()).isEqualTo(uuid);
        assertThat(returnedStreamingConnectionConfig.getUri()).isEqualTo(uri2);
    }

    /**
     * Exercises {@link TakServerConnectionManager#removeTakServerConnection(UUID)}
     * can also verify {@link TakServerConnectionManager#getTakServerConnections(boolean)} is empty
     */
    @Test
    public void removeTakServerConnection()
    {
        UUID uuid = UUIDUtils.generateType1UUID();
        var takServerConnection = connectionManager.addTakServerConnection(setupBasicConnConfiguration(uuid, uri));

        var streamingConnectionConfig = takServerConnection.getConnectionConfig();
        assertThat(streamingConnectionConfig.getId()).isSameAs(uuid);
        assertThat(streamingConnectionConfig.getUri()).isSameAs(uri);

        connectionManager.removeTakServerConnection(uuid);

        // since the events are not firing (because mocks) system is not removing (and not adding) but we can track an attempt was fired to remove
        verify(connectionRemoved, atLeastOnce()).fire(any(TakServerConnectionRemoved.class));

        var connections = connectionManager.getTakServerConnections(true);
        assertThat(connections).isEmpty();
    }

    private StreamingConnectionConfiguration setupBasicConnConfiguration(UUID uuid, URI uri)
    {
        var configuration = new StreamingConnectionConfiguration(uuid);
        configuration.setDisplayName("Yeti");
        configuration.setEnabled(false);
        configuration.setUri(uri);
        return configuration;
    }

    public void setupMocks() throws IOException
    {
        when(commonCommoManager.addStreamingConnection(any(StreamingConnectionConfiguration.class))).thenReturn(null);
        doNothing().when(anyEvent).fire(any());
    }
}