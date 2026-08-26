package gov.tak.core.comms.takserver;

import com.atakmap.commoncommo.InterfaceStatusListener;
import com.atakmap.commoncommo.NetInterface;
import com.atakmap.commoncommo.NetInterfaceErrorCode;
import com.atakmap.commoncommo.PhysicalNetInterface;
import com.atakmap.commoncommo.StreamingNetInterface;
import com.atakmap.commoncommo.TcpInboundNetInterface;
import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.core.comms.commo.CommonCommoManager;
import gov.tak.core.comms.security.SslCertificateManager;
import gov.takx.api.event.StartBackgroundProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.takserver.TakServerConnectionAdded;
import gov.takx.api.event.takserver.TakServerConnectionChanged;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.commons.security.EncodingUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static gov.takx.core.project.ServiceOrdering.START_BACKGROUND_GenericService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to manage TAK server connections.
 *
 * @since 3.0
 */
@ApplicationScoped
public class TakServerConnectionManager implements ITakServerConnectionManager, InterfaceStatusListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<UUID, TakServerConnection> connections = new ConcurrentHashMap<>();

    private final PreferencesNode takServersPrefNode = PreferenceEntry.COMMON_COMMO_TAK_SERVERS_NODE.node();

    // Key is connection's id as a string
    private final Map<String, StreamingConnectionConfiguration> connectionPrefs =
            takServersPrefNode.childrenToMap(StreamingConnectionConfiguration::new);

    private final SslCertificateManager certManager;
    private final CommonCommoManager commonCommoManager;
    private final IPreferenceManager preferenceManager;
    @VisibleForTesting
    final Event<TakServerConnectionAdded> connectionAdded;
    private final Event<TakServerConnectionChanged> connectionChanged;
    @VisibleForTesting
    final Event<TakServerConnectionRemoved> connectionRemoved;
    private final Event<TakServerConnectionOpened> connectionOpened;
    private final Event<TakServerConnectionClosed> connectionClosed;
    private final INotificationService notificationService;
    private final Set<NetInterface> interfaceErrorNotificationShown = new HashSet<>();

    @Inject
    TakServerConnectionManager(SslCertificateManager certManager, CommonCommoManager commonCommoManager,
                               IPreferenceManager preferenceManager,
                               INotificationService notificationService,
                               Event<TakServerConnectionAdded> connectionAdded,
                               Event<TakServerConnectionChanged> connectionChanged,
                               Event<TakServerConnectionRemoved> connectionRemoved,
                               Event<TakServerConnectionOpened> connectionOpened,
                               Event<TakServerConnectionClosed> connectionClosed)
    {
        this.certManager = certManager;
        this.commonCommoManager = commonCommoManager;
        this.preferenceManager = preferenceManager;
        this.notificationService = notificationService;
        this.connectionAdded = connectionAdded;
        this.connectionChanged = connectionChanged;
        this.connectionRemoved = connectionRemoved;
        this.connectionOpened = connectionOpened;
        this.connectionClosed = connectionClosed;
    }

    /**
     * Start this service.  Needs to start just after CommonCommoManager.
     */
    private void startService(@Observes @Priority(START_BACKGROUND_GenericService) StartBackgroundProjectServices ignored)
    {
        // Restore active TAK server connections
        connectionPrefs.values().forEach(this::addTakServerConnection);
    }

    /**
     * Stop this service, tear down and release all resources
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
    }

    @Override
    public Collection<? extends ITakServerConnection> getTakServerConnections(boolean connectedOnly)
    {
        // Create copies so the UI doesn't modify the underlying connections
        return connections.values().stream()
                .filter(conn -> !connectedOnly || conn.isConnected())
                .map(TakServerConnection::new)
                .sorted(Comparator.comparing(TakServerConnection::getCreationTime))
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public ITakServerConnection findConnectionByName(@Nonnull String name)
    {
        return connections.values().stream()
                .filter(c -> c.getDisplayName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    @Override
    public ITakServerConnection findConnectionByUuid(@Nonnull UUID uuid)
    {
        return connections.get(uuid);
    }

    @Nonnull
    @Override
    public ITakServerConnection addTakServerConnection(@Nonnull StreamingConnectionConfiguration connectionConfig)
    {
        final var connectionId = connectionConfig.getId();
        final var connection = new TakServerConnection(connectionId, null, connectionConfig);

        // Add the connection to the manager and fire the added event before we actually try to establish the connection
        // so that the UI can be updated with the connection info while the connection logic happens in the background.
        connections.put(connectionId, connection);
        connectionAdded.fire(new TakServerConnectionAdded(new TakServerConnection(connection)));

        if (connectionConfig.isEnabled()) doConnect(connectionConfig, connection, true);

        if (connectionPrefs.putIfAbsent(connectionId.toString(), connectionConfig) == null) preferenceManager.savePreferences();

        notificationService.show(Notification.builder("TAK Server Added: " + connection.getDisplayName(),
                Notification.Type.Info).build());
        return connection;
    }

    @Override
    public void updateTakServerConnection(@Nonnull UUID connectionId, @Nonnull StreamingConnectionConfiguration connectionConfig)
    {
        final var connection = connections.get(connectionId);
        if (connection == null)
        {
            logger.warn("No connection found for id '{}'; ignoring update: {}", connectionId, connectionConfig);
            return;
        }

        final var oldConfig = connection.getConnectionConfig();

        if (isNewConnectionNeeded(oldConfig, connectionConfig))
        {
            final var streamingNetInterface = connection.getStreamingNetInterface();
            if (streamingNetInterface != null)
            {
                commonCommoManager.removeStreamingConnection(streamingNetInterface);
                connection.setStreamingNetInterface(null);
                connection.setConnected(false);
            }

            if (connectionConfig.isEnabled())
            {
                final boolean enrollmentChanged = !oldConfig.isEnrollForClientCert() && connectionConfig.isEnrollForClientCert();
                doConnect(connectionConfig, connection, enrollmentChanged || needReEnrollment(oldConfig, connectionConfig));
            }
        }

        connection.setConnectionConfig(connectionConfig);
        connectionChanged.fire(new TakServerConnectionChanged(new TakServerConnection(connection)));

        // The change event can be fired from checking/unchecking 'enabled' in the TAK Server list UI.
        // When enabled changes from enabled to disabled, we need to send a closed event.
        // Sending of the opened event is handled in the interfaceUp method.
        if (!connection.isEnabled() && oldConfig.isEnabled())
        {
            connectionClosed.fire(new TakServerConnectionClosed(new TakServerConnection(connection)));
            notificationService.show(Notification.builder("TAK Server Disconnected: " + connection.getDisplayName(),
                    Notification.Type.Info).build());
        }

        connectionPrefs.put(connectionId.toString(), connectionConfig);
        preferenceManager.savePreferences();
    }

    /**
     * Actually attempt the connection using Commo.
     *
     * @param updateClientCert Pass {@code true} if the client cert needs to be updated, ignored if the connection
     *                         is not marked as using enrollment
     * @since 5.4
     */
    private void doConnect(@Nonnull StreamingConnectionConfiguration connectionConfig, @Nonnull TakServerConnection connection, boolean updateClientCert)
    {
        try
        {
            if (connectionConfig.isEnrollForClientCert() && updateClientCert)
            {
                updateConfigWithClientCertIfNecessary(connectionConfig);
            }

            connection.setStreamingNetInterface(commonCommoManager.addStreamingConnection(connectionConfig));
        } catch (IOException e)
        {
            logger.error("Error connecting to {}", connectionConfig.getNetConnectString(), e);
            notificationService.show(Notification.builder("TAK Server connection Error: " + connection.getDisplayName(),
                    Notification.Type.Error).build());
        }
    }

    /**
     * Checks if certs already exist (from enrolling previously). If no certs exist yet, it performs the enrollment and
     * updates the configuration with the file names and passwords for the generated client certs.
     *
     * @param connectionConfig The connection configuration parameters
     */
    private void updateConfigWithClientCertIfNecessary(StreamingConnectionConfiguration connectionConfig)
    {
        // TODO KMB: There is some logic in WinTAK for renewing expired certs. We probably need that too. See TAKX-692.

        // First check if the certs exists; if they do, no need to enroll again
        final String clientCertFileName = connectionConfig.getCertificateFileName();
        final String caCertFileName = connectionConfig.getCertificateAuthorityFileName();
        if (StringUtils.isEmpty(clientCertFileName) || !certManager.certExists(clientCertFileName) ||
                StringUtils.isEmpty(caCertFileName) || !certManager.certExists(caCertFileName))
        {
            try
            {
                // As long as this is successful, the config will be updated with the file paths and passwords for
                // the new certs.
                certManager.enrollForClientCertificate(connectionConfig);
                notificationService.show(Notification.builder("TAK Server Certificate Enrollment Success: " + connectionConfig.getDisplayName(),
                        Notification.Type.Success).build());
            } catch (Exception e)
            {
                logger.error("Error enrolling for client cert", e);
                notificationService.show(Notification.builder("TAK Server Certificate Enrollment Error: " + connectionConfig.getDisplayName(),
                        Notification.Type.Error).build());
            }
        }
    }

    /**
     * Determines if any parameters changed that would require a new connection with common commo.
     *
     * @param oldConfig The original connection configuration
     * @param newConfig The new connection configuration
     * @return True if any parameters affecting the connection with common commo changed.
     */
    @VisibleForTesting
    boolean isNewConnectionNeeded(StreamingConnectionConfiguration oldConfig, StreamingConnectionConfiguration newConfig)
    {
        return !Objects.equals(oldConfig.getUri(), newConfig.getUri()) ||
                oldConfig.isEnabled() != newConfig.isEnabled() ||
                !Arrays.equals(oldConfig.getCertificateBytes(), newConfig.getCertificateBytes()) ||
                !Arrays.equals(oldConfig.getCertificateAuthorityBytes(), newConfig.getCertificateAuthorityBytes()) ||
                !Objects.equals(oldConfig.getCertificatePassword(), newConfig.getCertificatePassword()) ||
                !Objects.equals(oldConfig.getCertificateAuthorityPassword(), newConfig.getCertificateAuthorityPassword()) ||
                !Objects.equals(oldConfig.getUsername(), newConfig.getUsername()) ||
                !Objects.equals(oldConfig.getPassword(), newConfig.getPassword()) ||
                oldConfig.isUseAuthentication() != newConfig.isUseAuthentication() ||
                oldConfig.isEnrollForClientCert() != newConfig.isEnrollForClientCert() ||
                needReEnrollment(oldConfig, newConfig);
    }

    /**
     * Determine if we have the case where the user is trying to enroll for a cert, but failed authentication the last time.
     * If the cert is empty, then enrollment failed, and we want to try again.
     *
     * @since 3.4
     */
    private boolean needReEnrollment(StreamingConnectionConfiguration oldConfig, StreamingConnectionConfiguration newConfig)
    {
        return oldConfig.isEnrollForClientCert() && newConfig.isEnrollForClientCert() && StringUtils.isEmpty(oldConfig.getCertificateFileName(), true);
    }

    @Override
    public boolean removeTakServerConnection(@Nonnull UUID connectionId)
    {
        final TakServerConnection connection = connections.remove(connectionId);
        boolean removed = false;
        if (connection != null)
        {
            final var streamingNetInterface = connection.getStreamingNetInterface();
            if (streamingNetInterface != null) removed = commonCommoManager.removeStreamingConnection(streamingNetInterface);

            connectionRemoved.fire(new TakServerConnectionRemoved(new TakServerConnection(connection)));
            notificationService.show(Notification.builder("TAK Server Removed: " + connection.getDisplayName(),
                    Notification.Type.Info).build());

            final String idString = connectionId.toString();
            StreamingConnectionConfiguration connectionPref = connectionPrefs.remove(idString);
            if (connectionPref != null)
            {
                takServersPrefNode.removeChildNode(idString);
                preferenceManager.savePreferences();
            }
        }
        return removed;
    }

    @Override
    public void interfaceUp(NetInterface netInterface)
    {
        interfaceErrorNotificationShown.remove(netInterface);
        if (logger.isDebugEnabled()) logger.debug("Commo Interface Up: {}", describeNetInterface(netInterface));

        if (netInterface instanceof StreamingNetInterface)
        {
            TakServerConnection serverConnection = getTakServerConnectionForNetInterface(netInterface);
            if (serverConnection != null)
            {
                serverConnection.setConnected(true);
                connectionOpened.fire(new TakServerConnectionOpened(new TakServerConnection(serverConnection)));
                notificationService.show(Notification.builder("TAK Server Connected: " + serverConnection.getDisplayName(),
                        Notification.Type.Success).build());
            }
        }
    }

    /**
     * Gets the TAK Server connection that contains the provided net interface, if one exists.
     *
     * @param netInterface The net interface
     * @return The connection containing the interface or {@code null} if none found.
     */
    @Nullable
    private TakServerConnection getTakServerConnectionForNetInterface(NetInterface netInterface)
    {
        return connections.values().stream()
                .filter(connection -> connection.getStreamingNetInterface() == netInterface)
                .findFirst().orElse(null);
    }

    @Override
    public void interfaceDown(NetInterface netInterface)
    {
        if (logger.isDebugEnabled()) logger.debug("Commo Interface Down: {}", describeNetInterface(netInterface));

        if (netInterface instanceof StreamingNetInterface)
        {
            TakServerConnection serverConnection = getTakServerConnectionForNetInterface(netInterface);
            if (serverConnection != null && serverConnection.isConnected())
            {
                serverConnection.setConnected(false);
                connectionClosed.fire(new TakServerConnectionClosed(new TakServerConnection(serverConnection)));
                notificationService.show(Notification.builder("Network Down, TAK Server Disconnected: " + serverConnection.getDisplayName(),
                        Notification.Type.Error).build());
            }
        }
    }

    @Override
    public void interfaceError(NetInterface netInterface, NetInterfaceErrorCode netInterfaceErrorCode)
    {
        if (!interfaceErrorNotificationShown.add(netInterface)) return;
        logger.error("Commo Interface Error on: {}: {}", describeNetInterface(netInterface), netInterfaceErrorCode);
        TakServerConnection connection = getTakServerConnectionForNetInterface(netInterface);
        if (connection != null && connection.isConnected())
        {
            connection.setConnected(false);
            connectionClosed.fire(new TakServerConnectionClosed(new TakServerConnection(connection)));
            notificationService.show(Notification.builder("Network Error, TAK Server Not Connected: " + connection.getDisplayName() + "\nReason: " + netInterfaceErrorCode,
                    Notification.Type.Error).build());
        }
    }

    private String describeNetInterface(NetInterface netInterface)
    {
        final Class<?> interfaceClass = netInterface.getClass();
        final StringBuilder sb = new StringBuilder(netInterface.getClass().getSimpleName());

        if (interfaceClass == PhysicalNetInterface.class)
        {
            final byte[] hardwareAddress = ((PhysicalNetInterface) netInterface).getHardwareAddress();
            sb.append(": HW interface ").append(EncodingUtils.bytesToHex(hardwareAddress).toLowerCase());
        } else if (interfaceClass == TcpInboundNetInterface.class)
        {
            sb.append(": port ").append(((TcpInboundNetInterface) netInterface).getPort());
        } else if (interfaceClass == StreamingNetInterface.class)
        {
            sb.append(": id ").append(((StreamingNetInterface) netInterface).streamId);
        }
        return sb.toString();
    }
}
