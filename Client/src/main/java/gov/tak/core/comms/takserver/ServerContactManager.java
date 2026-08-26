package gov.tak.core.comms.takserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.takserver.IServerContactManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.file.FilePath;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to manage the collection of server contacts from each TAK server connection established.
 *
 * @since 3.1
 */
@ApplicationScoped
public class ServerContactManager implements IServerContactManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String CONTACTS_DIR = "Contacts";
    private static final int SCAN_PERIOD_SECONDS = 30; // TODO: TAKX-1962 This should be a preference value
    private static final TypeReference<Map<String, Map<String, String>>> mapTypeReference = new TypeReference<>()
    {
    };

    // Map of maps.  Top-level keyed by the server UUID.  Second level keyed by the contact UID, mapping to a callsign.
    private final Map<String, Map<String, String>> contactsPerServer = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ClientEndpoint>> clientsPerServer = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> contactScanningTasksPerServer = new ConcurrentHashMap<>();
    private final IVirtualFileSystemManager vfsManager;
    private final ICoreExecutorService executorService;
    private final TakServerApiClient takServerApiClient;
    private final INotificationService notificationService;
    private File contactsCacheFile;

    @Inject
    ServerContactManager(@VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager vfsManager,
                         ICoreExecutorService executorService, TakServerApiClient takServerApiClient, INotificationService notificationService)
    {
        this.vfsManager = vfsManager;
        this.executorService = executorService;
        this.takServerApiClient = takServerApiClient;
        this.notificationService = notificationService;
    }

    private void handleProjectOpened(@Observes ProjectOpened event)
    {
        contactsCacheFile = new File(vfsManager.getRoot(new FilePath(FILES_DIR, CONTACTS_DIR).toString()), "contact-cache.json");
        if (contactsCacheFile.exists())
        {
            ObjectMapper mapper = new ObjectMapper();
            try
            {
                contactsPerServer.putAll(mapper.readValue(contactsCacheFile, mapTypeReference));
            } catch (IOException ex)
            {
                logger.error("Failed to parse existing contacts map", ex);
            }
        }
    }

    private void handleProjectClosing(@Observes ProjectClosing event)
    {
        try
        {
            if (contactsCacheFile != null)
            {
                File parentDir = contactsCacheFile.getParentFile();
                if (!parentDir.exists()) parentDir.mkdirs();

                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(contactsCacheFile, contactsPerServer);
            }
        } catch (IOException ex)
        {
            logger.error("Failed to write contacts map", ex);
        }
    }

    @Nonnull
    @Override
    public String getContactCallsign(@Nonnull UUID serverUuid, @Nonnull String contactUid)
    {
        return contactsPerServer.getOrDefault(serverUuid.toString(), Collections.emptyMap())
                .getOrDefault(contactUid, contactUid);
    }

    @Nonnull
    @Override
    public Map<UUID, Map<String, String>> getContactsPerServer()
    {
        return contactsPerServer.entrySet().stream()
                .collect(Collectors.toMap(e -> UUID.fromString(e.getKey()), Map.Entry::getValue));
    }

    @Nonnull
    @Override
    public Map<String, ClientEndpoint> getServerClientEndpoints(@Nonnull UUID serverUuid)
    {
        return clientsPerServer.getOrDefault(serverUuid.toString(), Collections.emptyMap());
    }

    /**
     * Handle a connection to a TAK Server being opened.
     * <p/>
     * Begin scanning on a loop for active contacts on the server.
     */
    private void onServerConnected(@Observes TakServerConnectionOpened openedEvent)
    {
        final ITakServerConnection connection = openedEvent.getConnection();

        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() -> queryContacts(connection), 0, SCAN_PERIOD_SECONDS, TimeUnit.SECONDS);
        contactScanningTasksPerServer.put(connection.getId().toString(), scheduledFuture);
    }

    /**
     * Handle a connection to a TAK Server being closed.
     * <p/>
     * Stop scanning for contacts on the associated server.
     */
    private void onServerDisconnected(@Observes TakServerConnectionClosed closedEvent)
    {
        stopQueryTask(closedEvent.getConnection());
    }

    /**
     * Handle a connection to a TAK Server being removed.
     * <p/>
     * Stop scanning for contacts on the associated server.
     *
     * @since 5.1
     */
    private void onServerDisconnected(@Observes TakServerConnectionRemoved removedEvent)
    {
        stopQueryTask(removedEvent.getConnection());

    }

    /**
     * @param connection Connection to stop scanning for contacts.
     *
     * @since 5.1
     */
    private void stopQueryTask(@Nonnull ITakServerConnection connection)
    {
        Future<?> future = contactScanningTasksPerServer.get(connection.getId().toString());
        if (future != null) future.cancel(true);
    }

    /**
     * Gets the {@link ClientEndpoint client endpoint list} from the server and updates the {@link #contactsPerServer} map.
     *
     * @param connection Connection to a TAK Server.
     */
    private void queryContacts(@Nonnull ITakServerConnection connection)
    {
        Thread.currentThread().setName("tak-server-contact-update");
        if (!connection.isConnected() || !connection.isEnabled()) return;
        final StreamingConnectionConfiguration connectionConfig = connection.getConnectionConfig();
        takServerApiClient.contactManagerEndpoints(connection)
                .getClientEndpoints().asyncGet(executorService).thenAccept(clients -> addContacts(connectionConfig, clients))
                .exceptionally(throwable -> {
                    String connectionName = connection.getDisplayName();
                    String errorMessage = String.format("Failed to query Contact List from: %s", connectionName);
                    TakServerUtilities.handleConnectionError(notificationService, connectionName, throwable, errorMessage);
                    logger.debug(errorMessage, throwable);
                    return null;
                });
    }

    /**
     * Set the contact list for the given server key using the given contact list.
     *
     * @param connectionConfig Config associated with the given server.
     * @param contactList List of contacts to use to create the UID to Callsign mapping
     */
    private void addContacts(StreamingConnectionConfiguration connectionConfig, List<ClientEndpoint> contactList)
    {
        final UUID serverUuid = connectionConfig.getId();

        // Sort the contact list by date, so newer mappings come last (taking precedence in the final mapping)
        final Map<String, String> contactMap = contactList.stream()
                .sorted(Comparator.comparing(ClientEndpoint::lastEventTime))
                .collect(Collectors.toMap(ClientEndpoint::uid, ClientEndpoint::callsign,
                        (existing, replacement) -> replacement, LinkedHashMap::new));

        contactsPerServer.merge(serverUuid.toString(), contactMap, (base, newMap) -> {
            base.putAll(newMap);
            return base;
        });

        final Map<String, ClientEndpoint> clientEndpointMap = contactList.stream()
                .collect(Collectors.toMap(ClientEndpoint::uid, Function.identity(),
                        (existing, replacement) -> replacement, HashMap::new));

        clientsPerServer.put(serverUuid.toString(), clientEndpointMap);
    }

    /**
     * Stop this service, tear down and release all resources.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        contactsPerServer.clear();
        contactScanningTasksPerServer.forEach((id, future) -> future.cancel(true));
    }
}
