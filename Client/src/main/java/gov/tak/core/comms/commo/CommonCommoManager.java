package gov.tak.core.comms.commo;

import com.atakmap.commoncommo.CoTMessageListener;
import com.atakmap.commoncommo.CoTMessageType;
import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.CommoLogger;
import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.ContactPresenceListener;
import com.atakmap.commoncommo.EnrollmentIO;
import com.atakmap.commoncommo.GenericDataListener;
import com.atakmap.commoncommo.InterfaceStatusListener;
import com.atakmap.commoncommo.MissionPackageIO;
import com.atakmap.commoncommo.PhysicalNetInterface;
import com.atakmap.commoncommo.StreamingNetInterface;
import com.atakmap.commoncommo.StreamingTransport;
import com.atakmap.commoncommo.TcpInboundNetInterface;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.chat.IChatService;
import gov.tak.api.comms.commo.BroadcastConnectionConfiguration;
import gov.tak.api.comms.commo.ConnectionConfiguration;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.commo.ICotEventInterceptor;
import gov.tak.api.comms.commo.ICotEventListener;
import gov.tak.api.comms.commo.ICotSendListener;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.contact.IContactService;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.core.chat.protocol.geochat.GeoChatProtocolHandler;
import gov.tak.platform.chat.ChatService;
import gov.takx.api.annotation.CallSign;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.event.ChatServiceStarted;
import gov.takx.api.event.StartBackgroundProjectServices;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.lang.ErrorThrowingRunnable;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static gov.takx.core.project.ServiceOrdering.START_BACKGROUND_CommonCommoManager;
import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_CommServiceManager;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_NETWORK_INTERFACES_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_USE_ALL_NETWORK_INTERFACES;

/**
 * Service to manage access to the TAK Common Commo library and all associated communication operations.
 *
 * @since 3.0
 */
@ApplicationScoped
public class CommonCommoManager implements ICommonCommoManager, CoTMessageListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // A map from each enabled network device to its active UDP output interfaces. commo will be updated when network
    // devices are enabled/disabled or outputs are added/removed/enabled/disabled
    @VisibleForTesting
    Map<NetworkDevice, List<NamedPhysicalNetInterface>> outboundUdpInterfaces = new HashMap<>();
    // A map from each enabled network device to its active UDP input interfaces. commo will be updated when network
    // devices are enabled/disabled or inputs are added/removed/enabled/disabled.
    @VisibleForTesting
    Map<NetworkDevice, List<NamedPhysicalNetInterface>> inboundUdpInterfaces = new HashMap<>();
    // A map from each enabled TCP config name to its interface. commo will be updated when network devices are
    // enabled/disabled
    @VisibleForTesting
    Map<String, TcpInboundNetInterface> inboundTcpInterfaces = new HashMap<>();
    // UDP interfaces added via the API. commo will be updated when inputs are added/removed via the API or network
    // devices are enabled/disabled via user preferences.
    @VisibleForTesting
    Map<ApiManagedInput, Map<NetworkDevice, PhysicalNetInterface>> apiManagedInboundUdpInterfaces = new HashMap<>();
    // TCP interfaces added via the API. commo will be updated when inputs are added/removed via the API.
    @VisibleForTesting
    Map<String, TcpInboundNetInterface> apiManagedInboundTcpInterfaces = new HashMap<>();

    @VisibleForTesting
    List<ICotEventListener> cotEventListeners = new CopyOnWriteArrayList<>();

    @VisibleForTesting
    List<ICotSendListener> cotSendListeners = new CopyOnWriteArrayList<>();

    @VisibleForTesting
    List<ICotEventInterceptor> cotEventInterceptors = new CopyOnWriteArrayList<>();

    @VisibleForTesting
    INetworkDeviceManager networkDeviceManager;
    private final InterfaceStatusListener interfaceStatusListener;
    private final SituationalAwarenessMessageSender saMessageSender;
    private final EnrollmentIO enrollmentIo;
    private final MissionPackageIO missionPackageIO;
    private final UUID nodeUuid;

    @VisibleForTesting
    volatile Commo commo;
    private String callSign;
    private final ContactsPanelController contactsPanelController;
    private final IChatService chatService;
    private final IContactService contactService;
    private final IPreferenceMonitor<String> callSignMonitor = PreferenceEntry.CALL_SIGN.newStringMonitor(this::setCallSign, false);
    private final CommoPreferenceMonitor commoPreferenceMonitor = new CommoPreferenceMonitor();
    private final Event<ChatServiceStarted> chatServiceStartedEvent;

    private volatile CompletableFuture<Void> shutdownFuture;

    /**
     * CDI Constructor.
     */
    @Inject
    CommonCommoManager(INetworkDeviceManager networkDeviceManager, InterfaceStatusListener interfaceStatusListener,
                       SituationalAwarenessMessageSender saMessageSender, EnrollmentIO enrollmentIo, MissionPackageIO missionPackageIO,
                       @NodeUUID UUID nodeUuid, @CallSign String callSign, ContactsPanelController contactsPanelController,
                       IChatService chatService, IContactService contactService, Event<ChatServiceStarted> chatServiceStartedEvent)
    {
        this.networkDeviceManager = networkDeviceManager;
        this.interfaceStatusListener = interfaceStatusListener;
        this.saMessageSender = saMessageSender;
        this.enrollmentIo = enrollmentIo;
        this.missionPackageIO = missionPackageIO;
        this.nodeUuid = nodeUuid;
        this.callSign = callSign;
        this.contactsPanelController = contactsPanelController;
        this.chatService = chatService;
        this.contactService = contactService;
        this.chatServiceStartedEvent = chatServiceStartedEvent;
    }

    /**
     * Start the foreground portion of this service, create the Commo instance.
     * @since 5.2
     */
    @SuppressWarnings("unused")
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event,
                              @PlatformUnderTest boolean underTest)
    {
        if (underTest) return;

        event.getProgressMonitor().setText("Starting TAK Communication Services");

        awaitPriorShutdown();

        try
        {
            commo = new Commo(new ForwardingLogger(), nodeUuid.toString(), callSign);

            commo.addInterfaceStatusListener(interfaceStatusListener);
            commo.addCoTMessageListener(this);
        } catch (CommoException e)
        {
            logger.error("Failed to initialize Common Commo instance", e);
        }
    }

    /**
     * If a prior Commo shutdown was issued, then join with it here to ensure it is completed before restarting.
     * @since 5.2
     */
    private void awaitPriorShutdown()
    {
        if (shutdownFuture != null)
        {
            logger.debug("Waiting for commo.shutdown to complete");

            try
            {
                shutdownFuture.get(10, TimeUnit.SECONDS);
            } catch (Exception e)
            {
                logger.warn("Failed to wait for prior Commo shutdown", e);
            }
            shutdownFuture = null;
        }
    }

    /**
     * Start the background portion of this service, configure our inputs/outputs and mission package handling.
     * @since 5.2
     */
    @SuppressWarnings("unused")
    private void startBackgroundService(@Observes @Priority(START_BACKGROUND_CommonCommoManager) StartBackgroundProjectServices ignored,
                                        @PlatformUnderTest boolean underTest)
    {
        if (underTest) return;

        try
        {
            configureIO();
            configureTakServerUsage();
            startChatService();

            saMessageSender.start(); // Crank up the SA message sender
            callSignMonitor.start(); // Monitor call sign

            commoPreferenceMonitor.start(commo);
        } catch (CommoException e)
        {
            logger.error("Failed to initialize Common Commo instance", e);
        }
    }

    /**
     * Configure our Input and Output connections.
     * @since 5.2
     */
    private void configureIO()
    {
        // Initialize the inputs; note: custom inputs added via API are not persisted
        updateUdpInputs();
        updateTcpInputs();

        // Initialize the default outputs; only UDP is supported
        updateUdpOutputs();

        COMMON_COMMO_USE_ALL_NETWORK_INTERFACES.addPreferenceEntryChangeListener(change -> onDevicePreferencesChanged());
        COMMON_COMMO_NETWORK_INTERFACES_NODE.addPreferenceEntryChangeListener(change -> onDevicePreferencesChanged());

        // Listen for input and output interface preference changes
        COMMON_COMMO_DEFAULT_INPUTS_NODE.addPreferenceEntryChangeListener(change -> onInterfacePreferencesChanged());
        COMMON_COMMO_DEFAULT_OUTPUTS_NODE.addPreferenceEntryChangeListener(change -> onInterfacePreferencesChanged());
    }

    /**
     * Configure Commo to enable enrollment and configure mission package handling.
     * @since 5.2
     */
    private void configureTakServerUsage() throws CommoException
    {
        // Setup Cert Signing Request handling for TAKServer connections
        commo.enableEnrollment(enrollmentIo);

        // Setup mission package handling
        commo.setupMissionPackageIO(missionPackageIO);
        try
        {
            // Mission package sending to peers over LAN requires spinning up the local HTTP server.
            // We can use a throw-away self-generated cert and any open port.
            String password = UUID.randomUUID().toString();
            byte[] certificate = commo.generateSelfSignedCert(password);
            int port;
            try (ServerSocket socket = new ServerSocket(0))
            {
                port = socket.getLocalPort();
            }
            commo.setMissionPackageLocalHttpsParams(port, certificate, password);

        } catch (Exception ex)
        {
            logger.error("Failed to initialize local mission package server, this will prevent peer to peer package sharing", ex);
        }
    }

    /**
     * Start the chat service.
     * @since 5.2
     */
    private void startChatService()
    {
        // Initialize chat service with the configured commo instance
        ((ChatService) chatService).start(new GeoChatProtocolHandler(commo, nodeUuid.toString(), callSign, contactService),
                contactsPanelController, contactService);

        chatServiceStartedEvent.fire(new ChatServiceStarted());
    }

    /**
     * Stop this service, tear down all resources
     */
    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_CommServiceManager) StopProjectServices event,
                             @PlatformUnderTest boolean underTest)
    {
        if (underTest) return;

        if (commo != null)
        {
            ((ChatService) chatService).stop();

            callSignMonitor.stop();
            commoPreferenceMonitor.stop();
            saMessageSender.stop();

            // commo.shutdown can take 3 - 5 seconds to actually finish.  So, kick this operation into the background.
            // If we're exiting, all threads will be killed, so we don't care.
            // If we're closing, we track the operation with a future so we can wait on it when the new project's
            // services are being started.

            shutdownFuture = CompletableFuture.runAsync(() -> {
                commo.shutdown();
                commo = null;
            });

            // Clear the in/outbound interfaces so that when we create a new project the desired interfaces are
            // properly passed to any new commo instance. If we don't clear these, the handling following 'configureIO()' will not
            // see any changes in the lists when comparing to what are in our preference nodes, and thus not add them to commo.
            outboundUdpInterfaces.clear();
            inboundUdpInterfaces.clear();
            inboundTcpInterfaces.clear();
            apiManagedInboundUdpInterfaces.clear();
            apiManagedInboundTcpInterfaces.clear();
        }

        // This isn't ideal, but the contact service is a static singleton provided by takkernel that will live across
        // projects. There is no way to reset it via the API. This manually clears out all the contacts added by the
        // current project.
        contactService.getAllContacts().forEach(contact -> contactService.removeContact(contact.getUid()));
    }

    /**
     * Set the call sign, handing the new value to commo and the SA sender.
     *
     * @param callSign New call sign
     */
    private void setCallSign(String callSign)
    {
        this.callSign = callSign;
        if (commo != null) commo.setCallsign(callSign);
    }

    @Override
    public void addCotEventListener(@Nonnull ICotEventListener listener)
    {
        cotEventListeners.add(listener);
    }

    @Override
    public void removeCotEventListener(@Nonnull ICotEventListener listener)
    {
        cotEventListeners.remove(listener);
    }

    @Override
    public void addCotSendListener(@Nonnull ICotSendListener listener)
    {
        cotSendListeners.add(listener);
    }

    @Override
    public void removeCotSendListener(@Nonnull ICotSendListener listener)
    {
        cotSendListeners.remove(listener);
    }

    @Override
    public void addCotMessageListener(@Nonnull CoTMessageListener listener)
    {
        if (commo != null) commo.addCoTMessageListener(listener);
    }

    @Override
    public void removeCotMessageListener(@Nonnull CoTMessageListener listener)
    {
        if (commo != null) commo.removeCoTMessageListener(listener);
    }

    @Override
    public void addContactPresenceListener(@Nonnull ContactPresenceListener listener)
    {
        if (commo != null) commo.addContactPresenceListener(listener);
    }

    @Override
    public void removeContactPresenceListener(@Nonnull ContactPresenceListener listener)
    {
        if (commo != null) commo.removeContactPresenceListener(listener);
    }

    @Override
    public void addCotEventInterceptor(@Nonnull ICotEventInterceptor interceptor)
    {
        cotEventInterceptors.add(interceptor);
    }

    @Override
    public void removeCotEventInterceptor(@Nonnull ICotEventInterceptor interceptor)
    {
        cotEventInterceptors.remove(interceptor);
    }

    /**
     * Responds to network device preference changes by updating the relevant interfaces that have been or need to be
     * added to commo.
     * <p>
     * Note: this method needs to be synchronized to avoid issues when two threads change preferences that lead to a
     * call to this method.  Consider a quick change of "Use all interfaces" from true to false, and then the user
     * checks a specific interface to enable it.  This leads to two tasks being dropped on the executor, leading
     * to a concurrent call here.  This can cause exceptions based on the state of the specific input.
     *
     * @since 5.4
     */
    private synchronized void onDevicePreferencesChanged()
    {
        updateUdpInputs();
        updateUdpOutputs();

        updateApiManagedUdpInputs(); // managed via API; not affected by interface pref changes
    }

    /**
     * Responds to interface (input and output) preference changes by updating the relevant interfaces that have been or
     * need to be added to commo.
     * <p>
     * Must be synchronized, see {@link #onDevicePreferencesChanged()} for details.
     *
     * @since 5.4
     */
    private synchronized void onInterfacePreferencesChanged()
    {
        updateUdpInputs();
        updateUdpOutputs();

        updateTcpInputs(); // not device-specific; not affected by device pref changes
    }

    /**
     * Adds or removes network device UDP output interfaces according to the preferences set in the
     * {@link PreferenceEntry#COMMON_COMMO_DEFAULT_OUTPUTS_NODE}.
     *
     * @since 5.1
     */
    @VisibleForTesting
    void updateUdpOutputs()
    {
        updateUdpNetworkDeviceInterfaces(
                getEnabledConfigs(COMMON_COMMO_DEFAULT_OUTPUTS_NODE, "udp", BroadcastConnectionConfiguration::new),
                outboundUdpInterfaces,
                (networkDevice, addList) -> addList.forEach(config -> addOutboundUdpInterface(networkDevice, config)),
                pni -> commo.removeBroadcastInterface(pni));
    }

    /**
     * Adds or removes network device UDP input interfaces according to the preferences set in the
     * {@link PreferenceEntry#COMMON_COMMO_DEFAULT_INPUTS_NODE}.
     */
    @VisibleForTesting
    void updateUdpInputs()
    {
        updateUdpNetworkDeviceInterfaces(
                getEnabledConfigs(COMMON_COMMO_DEFAULT_INPUTS_NODE, "udp", ConnectionConfiguration::new),
                inboundUdpInterfaces,
                (networkDevice, addList) -> addList.forEach(config -> addInboundUdpInterface(networkDevice, config.getUri(), false,
                        pni -> inboundUdpInterfaces.get(networkDevice).add(new NamedPhysicalNetInterface(config.getDisplayName(), pni)))),
                pni -> commo.removeInboundInterface(pni));
    }

    /**
     * Adds and removes UDP network device interfaces based on the list of {@code enabledInterfaces}.
     *
     * @param enabledInterfacesConfigurations The list of interface configurations that should be used by enabled network devices
     * @param interfaceMap A map from each network device to its currently enabled {@link PhysicalNetInterface interfaces}
     * @param addHandler Consumer that will add interfaces to commo for the specified network device
     * @param removeHandler Consumer that removes the specified {@link PhysicalNetInterface interface} from commo
     * @param <T> The {@link ConnectionConfiguration} type
     * @since 5.4
     */
    private <T extends ConnectionConfiguration> void updateUdpNetworkDeviceInterfaces(@Nonnull List<T> enabledInterfacesConfigurations,
                                                                                      @Nonnull Map<NetworkDevice, List<NamedPhysicalNetInterface>> interfaceMap,
                                                                                      @Nonnull BiConsumer<NetworkDevice, List<T>> addHandler,
                                                                                      @Nonnull Consumer<PhysicalNetInterface> removeHandler)
    {
        final var enabledInterfaceNames = enabledInterfacesConfigurations.stream().map(ConnectionConfiguration::getDisplayName).toList();

        for (NetworkDevice networkDevice : networkDeviceManager.getNetworkDevices())
        {
            if (enabled(networkDevice))
            {
                final var networkDeviceExists = interfaceMap.containsKey(networkDevice);
                final var networkDeviceInterfaceList = interfaceMap.computeIfAbsent(networkDevice, n -> new ArrayList<>());

                List<T> addList;

                if (networkDeviceExists)
                {
                    // Remove existing interfaces that aren't enabled
                    networkDeviceInterfaceList.stream()
                            .filter(npni -> !enabledInterfaceNames.contains(npni.displayName()))
                            .toList() // terminate stream; list is modified in forEach
                            .forEach(npniToRemove -> {
                                networkDeviceInterfaceList.remove(npniToRemove);
                                removeHandler.accept(npniToRemove.physicalNetInterface());
                            });

                    // Get a list of enabled interfaces that haven't already been added
                    final var names = networkDeviceInterfaceList.stream().map(NamedPhysicalNetInterface::displayName).toList();
                    addList = enabledInterfacesConfigurations.stream()
                            .filter(enabledInterface -> !names.contains(enabledInterface.getDisplayName()))
                            .toList();
                } else
                {
                    // The network device is being initialized, so we need to add all the enabled interfaces
                    addList = enabledInterfacesConfigurations;
                }

                // Add the enabled interfaces to the network devices
                addHandler.accept(networkDevice, addList);
            }
            // If a network device is newly disabled, remove its interfaces
            else
            {
                if (interfaceMap.containsKey(networkDevice)) // The device was previously enabled
                {
                    // Remove each interface
                    interfaceMap.remove(networkDevice).forEach(npni -> {
                        removeHandler.accept(npni.physicalNetInterface());
                        if (logger.isDebugEnabled()) logger.debug("Removed UDP interface {}", npni.displayName());
                    });
                }
            }
        }
    }

    /**
     * Adds or removes network device TCP input interfaces according to the preferences set in the
     * {@link PreferenceEntry#COMMON_COMMO_DEFAULT_INPUTS_NODE}. TCP inputs are not affected by network device changes, 
     * so updates only need to occur when input preferences change.
     *
     * @since 5.4 (renamed)
     */
    @VisibleForTesting
    void updateTcpInputs()
    {
        final List<ConnectionConfiguration> enabledTcpInputs =
                getEnabledConfigs(COMMON_COMMO_DEFAULT_INPUTS_NODE, "tcp", ConnectionConfiguration::new);

        // Remove TCP inputs that have been disabled or deleted
        final List<String> enabledTcpInputNames = enabledTcpInputs.stream().map(ConnectionConfiguration::getDisplayName).toList();
        inboundTcpInterfaces.keySet().stream()
                .filter(name -> !enabledTcpInputNames.contains(name))
                .toList() // terminate stream; list is modified in forEach
                .forEach(name -> commo.removeTcpInboundInterface(inboundTcpInterfaces.remove(name)));

        // Add TCP inputs that don't already exist
        final List<String> existingInterfaceNames = inboundTcpInterfaces.keySet().stream().toList();
        enabledTcpInputs.stream()
                .filter(config -> !existingInterfaceNames.contains(config.getDisplayName()))
                .forEach(input -> addInboundTcpInterface(input.getUri(), tini -> inboundTcpInterfaces.put(input.getDisplayName(), tini)));
    }

    /**
     * Updates custom UDP inputs that have been added via the API when network device preferences change. Note that TCP 
     * interfaces are not associated with network devices, so only UDP inputs are updated by this method.
     *
     * @since 5.4 (renamed)
     */
    private void updateApiManagedUdpInputs()
    {
        networkDeviceManager.getNetworkDevices().stream()
                .filter(nd -> !isMacAwdl(nd))
                .forEach(networkDevice -> {
                    final boolean networkDeviceEnabled = enabled(networkDevice);

                    apiManagedInboundUdpInterfaces.forEach((apiManagedInput, networkDeviceMap) -> {
                        if (networkDeviceEnabled && !networkDeviceMap.containsKey(networkDevice))
                        {
                            addInboundUdpInterface(networkDevice, apiManagedInput.uri, apiManagedInput.genericData,
                                    pni -> networkDeviceMap.put(networkDevice, pni));
                        } else if (!networkDeviceEnabled)
                        {
                            final PhysicalNetInterface pni = networkDeviceMap.remove(networkDevice);
                            if (pni != null) commo.removeInboundInterface(pni);
                        }
                    });
                });
    }

    /**
     * Adds a broadcast interface to the specified network device.
     *
     * @param networkDevice The network device to update
     * @param config The outbound (broadcast) interface configuration
     * @since 5.4
     */
    private void addOutboundUdpInterface(@Nonnull NetworkDevice networkDevice, @Nonnull BroadcastConnectionConfiguration config)
    {
        final URI uri = config.getUri();

        try
        {
            final var displayName = config.getDisplayName();
            final var host = uri.getHost();
            final var port = uri.getPort();
            final var messageTypes = config.getCoTMessageTypes();

            final var physicalNetInterface = isMulticastAddress(host)
                    ? commo.addBroadcastInterface(networkDevice.getMacAddress(), messageTypes, host, port)
                    : commo.addBroadcastInterface(messageTypes, host, port);
            outboundUdpInterfaces.get(networkDevice).add(new NamedPhysicalNetInterface(displayName, physicalNetInterface));

            if (logger.isDebugEnabled()) logger.debug("Added UDP broadcast output {} to {}", displayName, networkDevice.getDisplayName());
        } catch (CommoException e)
        {
            logger.error("Error adding UDP broadcast output {} to {}", uri, networkDevice.getDisplayName(), e);
        }
    }

    /**
     * @param networkDevice The network device
     * @param uri The URI that contains the connection details
     * @param forGenericData Pass {@code true} if the input data will update {@link GenericDataListener}s, {@code false}
     *                       if it will update {@link CoTMessageListener}s.
     * @param interfaceConsumer Consumer that will process the {@link PhysicalNetInterface} returned by commo
     * @since 5.1 (renamed 5.4)
     */
    @VisibleForTesting
    void addInboundUdpInterface(@Nonnull NetworkDevice networkDevice, @Nonnull URI uri, boolean forGenericData,
                                @Nonnull Consumer<PhysicalNetInterface> interfaceConsumer)
    {
        final String host = uri.getHost();
        final String[] mcastAddresses = "0.0.0.0".equals(host) ? new String[0] : new String[]{host};
        try
        {
            final PhysicalNetInterface pni =
                    commo.addInboundInterface(networkDevice.getMacAddress(), uri.getPort(), mcastAddresses, forGenericData);
            interfaceConsumer.accept(pni);

            if (logger.isDebugEnabled()) logger.debug("Added UDP input {} to {}", uri, networkDevice.getDisplayName());
        } catch (CommoException e)
        {
            logger.error("Error adding UDP input {} to {}", uri, networkDevice.getDisplayName(), e);
        }
    }

    /**
     * @param inputUri The TCP input URI
     * @param interfaceConsumer Consumer that will process the {@link TcpInboundNetInterface} returned by commo
     * @since 5.4
     */
    private void addInboundTcpInterface(@Nonnull URI inputUri, @Nonnull Consumer<TcpInboundNetInterface> interfaceConsumer)
    {
        final int port = inputUri.getPort();

        try
        {
            final TcpInboundNetInterface netInterface = commo.addTcpInboundInterface(port);

            if (netInterface != null)
            {
                interfaceConsumer.accept(netInterface);
                logger.debug("Added inbound TCP: {}", inputUri);
            }
        } catch (CommoException e)
        {
            logger.error("Failed to add input interface on port: {}", port, e);
        }
    }

    /**
     * @param networkDevice The device to lookup in the user preferences
     * @return {@code true} if the device is enabled, {@code false} if it isn't; the default is {@code true}
     * @since 5.1
     */
    @VisibleForTesting
    boolean enabled(@Nonnull NetworkDevice networkDevice)
    {
        if (COMMON_COMMO_USE_ALL_NETWORK_INTERFACES.getBooleanValue()) return true;

        // Assume any interfaces not explicitly marked as disabled are enabled
        return COMMON_COMMO_NETWORK_INTERFACES_NODE.node().getBoolean(networkDevice.getDisplayName(), true);
    }

    @Override
    public void addInput(@Nonnull URI inputUri, boolean forGenericData)
    {
        final String scheme = inputUri.getScheme();

        //noinspection EnhancedSwitchMigration
        switch (scheme)
        {
            case "udp":
                final ApiManagedInput apiManagedInput = new ApiManagedInput(inputUri, forGenericData);
                final HashMap<NetworkDevice, PhysicalNetInterface> deviceMap = new HashMap<>();
                apiManagedInboundUdpInterfaces.put(apiManagedInput, deviceMap);

                networkDeviceManager.getNetworkDevices().stream()
                        .filter(nd -> enabled(nd) && !isMacAwdl(nd))
                        .forEach(nd -> addInboundUdpInterface(nd, inputUri, forGenericData, npni -> deviceMap.put(nd, npni)));
                break;
            case "tcp":
                final String key = inputUri.getScheme() + "://" + inputUri.getPort();
                addInboundTcpInterface(inputUri, netInterface -> apiManagedInboundTcpInterfaces.put(key, netInterface));
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported protocol in connect string (%s)", inputUri));
        }
    }

    @Override
    public void removeInput(@Nonnull URI inputUri)
    {
        final String scheme = inputUri.getScheme();
        final int port = inputUri.getPort();

        //noinspection EnhancedSwitchMigration
        switch (scheme)
        {
            case "udp":
                var apiManagedInputEntry = apiManagedInboundUdpInterfaces.entrySet().stream()
                        .filter(entry -> entry.getKey().uri.equals(inputUri))
                        .findFirst().orElse(null);

                if (apiManagedInputEntry != null)
                {
                    apiManagedInboundUdpInterfaces.remove(apiManagedInputEntry.getKey());
                    apiManagedInputEntry.getValue().values().forEach(pnd -> commo.removeInboundInterface(pnd));
                }

                break;
            case "tcp":
                final var key = scheme + "://" + port;
                final var netInterface = apiManagedInboundTcpInterfaces.get(key);
                if (netInterface != null) commo.removeTcpInboundInterface(netInterface);

                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported protocol in connect string (%s)", inputUri));
        }
    }

    /**
     * Creates a streaming TAK Server connection with common commo based on the provided connection configuration.
     *
     * @param connectionConfig The configuration parameters of the connection to be added
     * @return An object representing the interface with commo or {@code null} if the connection wasn't created due to
     * being disabled.
     * @throws IOException If an error occurred creating the connection with commo
     */
    @Nullable
    public StreamingNetInterface addStreamingConnection(@Nonnull StreamingConnectionConfiguration connectionConfig) throws IOException
    {
        if (!connectionConfig.isEnabled()) return null;

        final URI inputUri = connectionConfig.getUri();
        final String host = inputUri.getHost();
        final int port = inputUri.getPort();
        final CoTMessageType[] types = {CoTMessageType.SITUATIONAL_AWARENESS, CoTMessageType.CHAT};
        final byte[] certBytes = connectionConfig.getCertificateBytes();
        final byte[] caCertBytes = connectionConfig.getCertificateAuthorityBytes();
        final String certPass = connectionConfig.getCertificatePassword();
        final String caCertPass = connectionConfig.getCertificateAuthorityPassword();
        final boolean useAuthentication = connectionConfig.isUseAuthentication();
        final String username = useAuthentication ? connectionConfig.getUsername() : null;
        final String password = useAuthentication ? connectionConfig.getPassword() : null;

        final StreamingTransport streamingTransport;

        // In commo, the type of connection attempted (TCP or SSL) is determined by the arguments passed. So, if the
        // user selected SSL, we need to make sure we won't pass null for any of the required arguments for an SSL
        // connection.
        if ("SSL".equalsIgnoreCase(inputUri.getScheme()))
        {
            streamingTransport = StreamingTransport.SSL;

            if (useAuthentication)
            {
                final boolean emptyUsername = StringUtils.isEmpty(username);
                final boolean emptyPassword = StringUtils.isEmpty(password);
                if (emptyUsername || emptyPassword)
                {
                    logger.warn("Cannot create SSL connection using authentication if username and/or password is missing. " +
                            "Username {}, Password {}", emptyUsername ? "EMPTY" : "OK", emptyPassword ? "EMPTY" : "OK");
                    throw new IOException("Cannot create SSL connection using authentication if username and/or password is missing.");
                }
            }

            // Certs are always required for SSL (even if the user provided user/password credentials for LDAP)
            if (certBytes.length == 0 || caCertBytes.length == 0 || StringUtils.isEmpty(certPass) || StringUtils.isEmpty(caCertPass))
            {
                String redactedCaCertPass = caCertPass == null ? null : (caCertPass.isEmpty() ? "<empty>" : "<redacted>");
                String redactedCertPass = certPass == null ? null : (certPass.isEmpty() ? "<empty>" : "<redacted>");
                logger.warn("Cannot create SSL connection if the CA cert, Client cert, or their passwords is missing. " +
                        "CA Cert: {}, Password: {}; Client Cert: {}, Password: {}",
                        caCertBytes, redactedCaCertPass, certBytes, redactedCertPass);
                throw new IOException("Cannot create SSL connection if the CA cert, Client cert, or their passwords is missing.");
            }
        } else
        {
            streamingTransport = StreamingTransport.TCP;
        }

        // Note: ATAK validates the cert here, but it's not necessary for us to do so since commo will take care of it
        // and will throw an exception with a user-friendly error if there is an issue.

        try
        {
            return commo.addStreamingInterface(streamingTransport, host, port, types, certBytes, caCertBytes, certPass, caCertPass, username, password);
        } catch (CommoException e)
        {
            throw new IOException("Failed to connect to " + host + ": " + e.getMessage());
        }
    }

    /**
     * Disconnects the TAK Server connection with common commo represented by the provided interface.
     *
     * @param streamingInterface The interface representing the connection with commo
     * @return {@code true} if the connection is successfully removed by commo, {@code false} if the removal fails
     * @since 3.5.0 (return added)
     */
    public boolean removeStreamingConnection(@Nonnull StreamingNetInterface streamingInterface)
    {
        try
        {
            commo.removeStreamingInterface(streamingInterface);
            return true;
        } catch (Exception e)
        {
            logger.error("Failed to remove streaming interface: {}", streamingInterface, e);
            return false;
        }
    }

    @Override
    public void addGenericDataListener(@Nonnull GenericDataListener listener)
    {
        commo.addGenericDataListener(listener);
    }

    @Override
    public void removeGenericDataListener(@Nonnull GenericDataListener listener)
    {
        commo.removeGenericDataListener(listener);
    }

    @Override
    public void sendMissionPackage(@Nonnull Contact contact, @Nonnull File missionPackageFile) throws CommoException
    {
        sendMissionPackage(List.of(contact), missionPackageFile);
    }

    @Override
    public void sendMissionPackage(@Nonnull List<Contact> contacts, @Nonnull File missionPackageFile) throws CommoException
    {
        String packageFileName = missionPackageFile.getName();
        logger.debug("init mission package transfer for '{}'", packageFileName);

        // Contacts remaining after the 'init' call are offline at the time of invocation, so they won't be receiving the file.
        List<Contact> offlineContacts = new ArrayList<>(contacts);
        int transactionId = commo.sendMissionPackageInit(offlineContacts, missionPackageFile, packageFileName, packageFileName, null, null, null);
        if (!offlineContacts.isEmpty()) logger.debug("{} of {} contacts were not online to receive the mission package", offlineContacts.size(), contacts.size());

        logger.debug("starting mission package transfer for '{}' with transaction-id {}", packageFileName, transactionId);
        commo.startMissionPackageSend(transactionId);
    }

    @Override
    public void broadcastCoT(@Nonnull String cotMessage) throws CommoException
    {
        logger.debug("broadcast: {}", cotMessage);

        cotMessage = notifyCotInterceptorsOnSend(cotMessage);
        commo.broadcastCoT(cotMessage);
        notifyCotSendListeners(cotMessage);
    }

    @Override
    public void broadcastCoT(@Nonnull String cotMessage, @Nonnull CoTSendMethod sendMethod) throws CommoException
    {
        logger.debug("broadcasting over {}: {}", sendMethod, cotMessage);

        cotMessage = notifyCotInterceptorsOnSend(cotMessage);
        commo.broadcastCoT(cotMessage, sendMethod);
        notifyCotSendListeners(cotMessage);
    }

    @Override
    public void sendCotToMission(@Nonnull ITakServerConnection connection, @Nonnull String cotMessage, @Nonnull String missionName) throws CommoException
    {
        // Commo wants the format 'scheme:host:port' which does not
        // match the format of 'connection.getConnectionConfig().getNetConnectString()'
        // which is 'host:port:scheme'
        URI uri = connection.getUri();
        String streamId = uri.getScheme() + ":" + uri.getHost() + ":" + uri.getPort();

        logger.debug("Send to mission {}/{}: {}", streamId, missionName, cotMessage);

        cotMessage = notifyCotInterceptorsOnSend(cotMessage);
        commo.sendCoTToServerMissionDest(streamId, missionName, cotMessage);
        notifyCotSendListeners(cotMessage);
    }

    @Override
    public void sendCoT(@Nonnull Vector<Contact> contacts, @Nonnull String cotMessage, @Nonnull CoTSendMethod sendMethod) throws CommoException
    {
        if (contacts.isEmpty()) throw new IllegalArgumentException("'contacts' must not be empty");

        logger.debug("Send to {}: {}", contacts, cotMessage);

        cotMessage = notifyCotInterceptorsOnSend(cotMessage);
        commo.sendCoT(contacts, cotMessage, sendMethod);
        notifyCotSendListeners(cotMessage);
    }

    @Override
    public void setEnableAddressReuse(boolean enable)
    {
        commo.setEnableAddressReuse(enable);
    }

    @Override
    public void setMulticastLoopbackEnabled(boolean enable)
    {
        commo.setMulticastLoopbackEnabled(enable);
    }

    /**
     * Notify all registered interceptors of a message receipt/send.
     * @since 5.8
     */
    private void notifyCotInterceptorsOnReceive(CotEvent cotEvent)
    {
        ExceptionUtils.safeDoOperation(cotEventInterceptors, l -> l.onReceive(cotEvent));
    }

    /**
     * Notify all registered interceptors of a message send.
     * @param cotXml Original message
     * @return Potentially modified CoT XML
     * @since 5.8
     */
    @Nonnull
    private String notifyCotInterceptorsOnSend(String cotXml)
    {
        if (cotEventInterceptors.isEmpty()) return cotXml;

        // Give the interceptor an actual event object to process
        final var cotEvent = CotEvent.parse(cotXml);
        ExceptionUtils.safeDoOperation(cotEventInterceptors, l -> l.onSend(cotEvent));
        return cotEvent.toString();
    }

    /**
     * Notify all registered listeners of a CoT message being sent.
     * @since 5.3
     */
    private void notifyCotSendListeners(String cotMessage)
    {
        ExceptionUtils.safeDoOperation(cotSendListeners, l -> l.cotMessageSent(cotMessage));
    }

    @Override
    public void cotMessageReceived(String cotMessage, String remoteEndpointId)
    {
        logger.debug("received {}", cotMessage);

        // If it is a valid event - hand it to the listeners
        final CotEvent event = CotEvent.parse(cotMessage);

        notifyCotInterceptorsOnReceive(event);

        if (event.isValid())
        {
            for (ICotEventListener listener : cotEventListeners)
            {
                try
                {
                    listener.cotEventReceived(event);
                } catch (Exception e)
                {
                    logger.error("Failure in CotEvent listener: {}", listener, e);
                }
            }
        } else
        {
            logger.debug("Invalid CoT message: {}", cotMessage);
        }
    }

    /**
     * NOTE: Javadoc copied from CommonCommo.
     * <p/>
     * Initiate an enrollment operation for the given server. The enrollment operation will be initialized and associated
     * with the unique identifier returned.  The enrollment operation will not commence processing until enrollmentStart()
     * is invoked with the returned identifier.  Once started, updates on progress and completion status of the operation
     * will be delivered to the enrollmentIO interface implementation previously registered with this Commo instance via
     * enableEnrollment() (which must be successfully invoked prior to using this method).
     * All arguments are required (non-null) unless otherwise noted.
     *
     * @param host the hostname or IP address of the server to enroll with
     * @param port the port that the specified server is configured to provide the enrollment API on
     * @param verifyHost true to verify that the server's SSL certificate, provided during the https transaction,
     *                  matches the information provided in caCert
     * @param user username to use when authenticating to the server's enrollment process.
     * @param password password to use when authenticating to the server's enrollment process when useTokenAuth is false,
     *                or the token to use for authentication when useTokenAuth is true
     * @param useTokenAuth specifies authentication mode and how the password parameter is interpreted.
     *                   Passing true will result in token based authentication being used and false will result in
     *                    basic username/password authentication.
     * @param caCert a PKCS#12 certificate store to use when performing https transactions with the server.
     *              Supplying {@code null} will result in https operations not checking the server certificate
     * @param caCertPassword password used to decrypt caCert. May be {@code null} if (and only if) caCert is null
     * @param clientCertPassword password the enrollment operation should use to encrypt the client certificate
     *                          resulting from the enrollment
     * @param enrolledTrustPassword password the enrollment operation should use to encrypt the trust/ca store resulting
     *                             from the enrollment
     * @param keyPassword password the enrollment operation should use to encrypt the private key generated
     * @param keyLength length of private key to generate, in bits
     * @param clientVersionInfo version identifier the enrollment operation should present to the server, or {@code null} to not present an identifier
     * @return integer identifier that uniquely identifies this enrollment operation
     * @throws CommoException if an error occurs, including but not limited to caCert being invalid, invalid port number,
     *                       or missing/invalid arguments
     * @see #enrollmentStart(int)
     *
     * @since 3.3
     */
    public int enrollmentInit(@Nonnull String host, int port, boolean verifyHost,
                              @Nonnull String user, @Nonnull String password, boolean useTokenAuth,
                              @Nullable byte[] caCert, @Nullable String caCertPassword,
                              @Nonnull String clientCertPassword, @Nonnull String enrolledTrustPassword,
                              @Nonnull String keyPassword, int keyLength, @Nullable String clientVersionInfo)
            throws CommoException
    {
        return commo.enrollmentInit(host, port, verifyHost, user, password, useTokenAuth, caCert, caCertPassword,
                clientCertPassword, enrolledTrustPassword, keyPassword, keyLength, clientVersionInfo);
    }

    /**
     * NOTE: Javadoc copied from CommonCommo.
     * <p/>
     * Commences an enrollment operation previously initialized via a call to {@link #enrollmentInit}.
     *
     * @param id identifier of the previously initialized transfer
     * @throws CommoException if this Commo instance has not yet had enrollment enabled by calling enableEnrollment or
     *        the supplied id does not identify an initialized (but not already started) enrollment operation
     * @since 3.3
     */
    public void enrollmentStart(int id) throws CommoException
    {
        commo.enrollmentStart(id);
    }

    @Override
    public Contact configKnownEndpointForContact(@Nonnull String uid, @Nullable String callsign, @Nullable String endpointId)
    {
        if ((callsign == null) != (endpointId == null))
        {
            throw new IllegalArgumentException("callsign and endpointId must both have values to config or both be null to clear");
        }

        try
        {
            return commo.configKnownEndpointForContact(uid, callsign, endpointId);
        } catch (CommoException e)
        {
            logger.warn("Failed to add endpoint for contact. UID: {}, callsign: {}, streamingId: {}", uid, callsign, endpointId);
            return null;
        }
    }

    /**
     * @param preferenceEntry The preference entry containing the connection configurations
     * @param scheme The scheme to use when filtering the configuration list
     * @param nodeToConfigFunction A function that processes a preference node and returns a configuration object
     * @return A list of configurations for the specified scheme
     * @param <T> The configuration type
     * @since 5.4
     */
    @VisibleForTesting
    @Nonnull
    <T extends ConnectionConfiguration> List<T> getEnabledConfigs(@Nonnull PreferenceEntry preferenceEntry,
                                                                  @Nonnull String scheme,
                                                                  @Nonnull Function<PreferencesNode, T> nodeToConfigFunction)
    {
        return preferenceEntry.node().childrenToMap(nodeToConfigFunction).values().stream()
                .filter(config -> config.isEnabled() && scheme.equals(config.getUri().getScheme()))
                .toList();
    }

    /**
     * Detects Mac Apple Wireless Direct Link devices that will cause errors if we attempt to bind.
     *
     * @param networkDevice The network device to check
     * @return {@code true} iff on macOS and the network device is an AWDL device
     * @since 5.5
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isMacAwdl(@Nonnull NetworkDevice networkDevice)
    {
        final var awdl = SystemUtils.IS_OS_MAC && networkDevice.getDisplayName().startsWith("awdl");

        if (awdl) logger.debug("Detected AWDL device {}. Device will be skipped.", networkDevice);

        return awdl;
    }

    /**
     * @param address The address to check
     * @return {@code true} if the address is a multicast address, {@code false} if it's an invalid String or not
     * a multicast address
     * @since 5.4
     */
    private boolean isMulticastAddress(String address)
    {
        try
        {
            return Inet4Address.getByName(address).isMulticastAddress();
        } catch (Exception ignored)
        {
            return false;
        }
    }

    /**
     * Holds data required to create/restore custom inputs.
     *
     * @param uri the input URI
     * @param genericData flag indicating if the data is generic
     * @see #addInput(URI, boolean)
     * @since 5.1
     */
    @VisibleForTesting
    record ApiManagedInput(URI uri, boolean genericData)
    {
    }

    /**
     * Facade to forward CommoLogger operations to our logger.
     */
    private static final class ForwardingLogger implements CommoLogger
    {
        private static final String MSG_FMT = "{}: {}, detail={}";

        @Override
        public void log(Level level, Type type, String message, LoggingDetail detail)
        {
            //noinspection EnhancedSwitchMigration
            switch (level)
            {
                case VERBOSE:
                    if (logger.isTraceEnabled()) logger.trace(MSG_FMT, type, message, makeDetailString(detail));
                    break;
                case DEBUG:
                    if (logger.isDebugEnabled()) logger.debug(MSG_FMT, type, message, makeDetailString(detail));
                    break;
                case INFO:
                    if (logger.isInfoEnabled()) logger.info(MSG_FMT, type, message, makeDetailString(detail));
                    break;
                case WARNING:
                    logger.warn(MSG_FMT, type, message, makeDetailString(detail));
                    break;
                case ERROR:
                    logger.error(MSG_FMT, type, message, makeDetailString(detail));
                    break;
            }
        }

        private String makeDetailString(LoggingDetail detail)
        {
            if (detail instanceof NetworkDetail nd)
            {
                return "port " + nd.port;
            } else if (detail instanceof ParsingDetail pd)
            {
                return String.format("from [%s]: %s%n%s", pd.remoteEndpointId, pd.errorDetail, new String(pd.messageData));
            } else
            {
                return String.valueOf(detail);
            }
        }
    }

    /**
     * Initializes commo with its {@link PreferenceEntry} values and applies updates when the values change.
     *
     * @since 5.0
     */
    private static class CommoPreferenceMonitor
    {
        private static final String AUTHENTICATION_DATABASE_CREDENTIAL_TYPE = "takx_networking";
        private static final String AUTHENTICATION_DATABASE_SITE = "gov.tak.core.comms.commo.meshkey";
        private static final String AUTHENTICATION_DATABASE_USERNAME = "takx";

        private Commo commo;

        private final IPreferenceMonitor<Integer> multicastTtlMonitor =
                PreferenceEntry.COMMON_COMMO_MULTICAST_TTL.newIntegerMonitor(i -> commo.setTTL(i), false);
        private final IPreferenceMonitor<Boolean> enableAddressReuseMonitor =
                PreferenceEntry.COMMON_COMMO_ENABLE_ADDRESS_REUSE.newBooleanMonitor(b -> commo.setEnableAddressReuse(b), false);
        private final IPreferenceMonitor<Boolean> enableMulticastLoopbackMonitor =
                PreferenceEntry.COMMON_COMMO_ENABLE_MULTICAST_LOOPBACK.newBooleanMonitor(b -> commo.setMulticastLoopbackEnabled(b), false);
        private final IPreferenceMonitor<Integer> tcpConnectTimeoutMonitor =
                PreferenceEntry.COMMON_COMMO_TCP_CONNECTION_TIMEOUT.newIntegerMonitor(i -> commo.setTcpConnTimeout(i), false);
        private final IPreferenceMonitor<Boolean> addDestUidMonitor =
                PreferenceEntry.COMMON_COMMO_ADD_DEST_UID.newBooleanMonitor(b -> commo.setDestUidInsertionEnabled(b), false);

        // Secure server API port
        private final IPreferenceMonitor<Integer> secureApiPortMonitor =
                PreferenceEntry.COMMON_COMMO_TAK_SERVER_SECURE_API_PORT
                        .newIntegerMonitor(i -> runWithExceptionHandling(() -> commo.setMissionPackageHttpsPort(i),
                                "Error setting server secure API port to " + i), false);
        // Un-secure server API port
        private final IPreferenceMonitor<Integer> unsecureApiPortMonitor =
                PreferenceEntry.COMMON_COMMO_TAK_SERVER_UNSECURE_API_PORT
                        .newIntegerMonitor(i -> runWithExceptionHandling(() -> commo.setMissionPackageHttpPort(i),
                                "Error setting server unsecure API port to " + i), false);
        private final IPreferenceMonitor<String> meshEncryptionKeyMonitor =
                PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.newStringMonitor(this::onCryptoKeysPreferenceChange, false);

        /**
         * Initializes commo with its {@link PreferenceEntry} values and starts the {@link IPreferenceMonitor}s.
         *
         * @param commo The commo instance
         * @throws CommoException thrown if there are exceptions initializing commo
         */
        public void start(@Nonnull Commo commo) throws CommoException
        {
            this.commo = commo;

            commo.setTTL(multicastTtlMonitor.getCurrentValue());
            commo.setEnableAddressReuse(enableAddressReuseMonitor.getCurrentValue());
            commo.setMulticastLoopbackEnabled(enableMulticastLoopbackMonitor.getCurrentValue());
            commo.setDestUidInsertionEnabled(addDestUidMonitor.getCurrentValue());
            commo.setTcpConnTimeout(tcpConnectTimeoutMonitor.getCurrentValue());
            commo.setMissionPackageHttpsPort(secureApiPortMonitor.getCurrentValue());
            commo.setMissionPackageHttpPort(unsecureApiPortMonitor.getCurrentValue());

            setCryptoKeysToDatabaseValue();

            multicastTtlMonitor.start();
            enableAddressReuseMonitor.start();
            enableMulticastLoopbackMonitor.start();
            addDestUidMonitor.start();
            tcpConnectTimeoutMonitor.start();
            secureApiPortMonitor.start();
            unsecureApiPortMonitor.start();
            meshEncryptionKeyMonitor.start();
        }

        /**
         * Stops the {@link IPreferenceMonitor}s and clears the commo instance field.
         */
        public void stop()
        {
            multicastTtlMonitor.stop();
            enableAddressReuseMonitor.stop();
            enableMulticastLoopbackMonitor.stop();
            addDestUidMonitor.stop();
            tcpConnectTimeoutMonitor.stop();
            secureApiPortMonitor.stop();
            unsecureApiPortMonitor.stop();
            meshEncryptionKeyMonitor.stop();

            commo = null;
        }

        /**
         * Sets the commo crypto keys based on the value stored in {@link PreferenceEntry#COMMON_COMMO_MESH_ENCRYPTION_KEY}.
         * The key value is stored in the {@link AtakAuthenticationDatabase}, commo is updated and the preference value
         * is cleared. This is a little funky, but mirrors ATAK and will allow developers to change the key
         * programmatically via the {@link PreferenceEntry}.
         *
         * @param keys The String that contains the keys
         */
        private void onCryptoKeysPreferenceChange(@Nonnull String keys)
        {
            if (keys.length() != 64) keys = "";

            // Save the keys to the database; null will clear the credentials
            AtakAuthenticationDatabase.saveCredentials(AUTHENTICATION_DATABASE_CREDENTIAL_TYPE,
                    AUTHENTICATION_DATABASE_SITE, AUTHENTICATION_DATABASE_USERNAME, keys, false);

            // Update commo
            setCryptoKeysToDatabaseValue();

            // Clear the preference
            meshEncryptionKeyMonitor.stop();
            PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.put("");
            meshEncryptionKeyMonitor.start();
        }

        /**
         * Updates the commo mesh crypto keys from the value stored in the {@link AtakAuthenticationDatabase}.
         */
        private void setCryptoKeysToDatabaseValue()
        {
            // Get the keys from the database
            final AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                    .getCredentials(AUTHENTICATION_DATABASE_CREDENTIAL_TYPE, AUTHENTICATION_DATABASE_SITE);

            // If the credentials are valid, update commo
            if (credentials != null && Strings.isNotBlank(credentials.password) && credentials.password.length() == 64)
            {
                var authenticationKey = credentials.password.substring(0, 32).getBytes(StandardCharsets.UTF_8);
                var cryptoKey = credentials.password.substring(32, 64).getBytes(StandardCharsets.UTF_8);

                runWithExceptionHandling(() -> commo.setCryptoKeys(authenticationKey, cryptoKey),
                        "Error retrieving mesh encryption key from String \"[content hidden]..." +
                                credentials.password.substring(62) + "\"");
            } else
            {
                runWithExceptionHandling(() -> commo.setCryptoKeys(null, null),
                        "Error clearing the mesh encryption key");
            }
        }

        /**
         * Convenience method that calls a {@link ErrorThrowingRunnable} and catches and logs {@link CommoException}s.
         *
         * @param runnable A functional interface that calls a commo method
         * @param errorMessage The error message to log if there is a {@link CommoException}.
         */
        private void runWithExceptionHandling(@Nonnull ErrorThrowingRunnable<CommoException> runnable, @Nonnull String errorMessage)
        {
            try
            {
                runnable.run();
            } catch (CommoException e)
            {
                logger.error(errorMessage, e);
            }
        }
    }

    /**
     * Associates a {@link PhysicalNetInterface} with its display name. The association allows us to respond to
     * network input and output preference changes (add/remove/enable/disable).
     *
     * @param displayName The display name used for the input or output; the UI guarantees the names are unique
     * @param physicalNetInterface The interface created by commo when the input or output is added
     * @since 5.4
     */
    private record NamedPhysicalNetInterface(@Nonnull String displayName, @Nonnull PhysicalNetInterface physicalNetInterface)
    {
    }
}
