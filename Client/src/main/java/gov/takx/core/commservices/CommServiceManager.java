package gov.takx.core.commservices;

import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.commservices.ICommEventDelegate;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ICommServiceTerminationManager;
import gov.takx.api.commservices.ISerialPortStatus;
import gov.takx.api.commservices.IServerSocketListener;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.connection.ConnectionAdded;
import gov.takx.api.event.connection.ConnectionRemoved;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommManager;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.commservices.ICommSocketListener;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.ui.event.CommunicationsEvent;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manager for comm services that manages serial, ethernet, server and UDP comm services (and more to come!)
 * at the device gateway level.
 *
 */
@ApplicationScoped
public class CommServiceManager implements ICommServiceManager, ICommServiceTerminationManager, ICommEventDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static InetAddress defaultInetAddress;

    private TimerTask portScannerTask = null;

    private final Map<String, IServerSocketListener> socketListeners = new ConcurrentHashMap<>();
    private final Map<String, ICommService> commServices = new ConcurrentHashMap<>(); // TODO: Change this to a map of maps, for each type of comm service
    private PortBaudRates portBaudRates;

    private final boolean platformUnderTest;
    private final IPersistServiceProvider persistServiceProducer;
    private final ITimerService timerService;
    private final boolean enableCommLogging;
    private final boolean enableSerialPorts;
    private final Event<ConnectionAdded> connectionAddedEvent;
    private final Event<ConnectionRemoved> connectionRemovedEvent;
    private final Event<CommunicationsEvent> communicationsEvent;

    @Inject
    CommServiceManager(IPersistServiceProvider persistServiceProducer, ITimerService timerService,
                       Event<ConnectionAdded> connectionAddedEvent, Event<ConnectionRemoved> connectionRemovedEvent,
                       Event<CommunicationsEvent> communicationsEvent, @PlatformUnderTest boolean platformUnderTest)
    {
        this.platformUnderTest = platformUnderTest;
        this.persistServiceProducer = persistServiceProducer;
        this.timerService = timerService;
        this.connectionAddedEvent = connectionAddedEvent;
        this.connectionRemovedEvent = connectionRemovedEvent;
        this.communicationsEvent = communicationsEvent;

        enableCommLogging = PreferenceEntry.ENABLE_COMM_LOGGING.getBooleanValue();
        enableSerialPorts = PreferenceEntry.ENABLE_SERIAL_PORTS.getBooleanValue();
    }

    @Override
    public ICommManager createCommPath(String absoluteFilePath, InputStream inputStream, String commFormat) throws IOException
    {
        if (null == inputStream) throw new IOException("InputStream must not be null.");

        logger.debug("createCommPath type = FILE, name = {}", absoluteFilePath);

        ICommService commService = getExistingCommService(CommServiceType.FILE, absoluteFilePath);

        if (null != commService)
        {
            throw new IOException("Comm already open under the comm path name " + absoluteFilePath);
        }

        logger.debug("Comm Path Name: {}", absoluteFilePath);

        // If a null format was passed in, we will use a default value instead
        String format = (commFormat == null) ? "RawInputStream" : commFormat;

        Map<String, Serializable> props = new HashMap<>();
        props.put(CommServiceConstants.RUNNING.toString(), true);
        props.put(CommServiceConstants.PORT_NAME.toString(), absoluteFilePath);
        props.put(CommServiceConstants.PORT_FORMAT.toString(), format);

        ICommService fileService = new FileCommService(props, inputStream);

        if (fileService.startCommService(null))
        {
            addCommService(fileService);
        }

        ICommManager manager = getExistingCommService(CommServiceType.FILE, absoluteFilePath);

        if (manager == null)
        {
            throw new IOException("Unable to create comm path.");
        }

        return manager;
    }

    /**
     * Create a comm path based on the type and name.  Adds a comm format string to the properties so
     * that the plugin developer can open ports with a custom format name, and choose when to or when not
     * to create parsers and framers for that format.
     *
     * @param commType     string type of port, i.e. "ETHERNET", "SERIAL"
     * @param commPathName String name of comm path "COM1", "127.0.0.1:1024"
     * @param commFormat   String custom format "MyDeviceComms", etc.
     * @return ICommManager manager for the comm service to allow for writing data.
     * @throws IOException if an error occurs while creating the comm path, such as a comm port being open already.
     * @see CommServiceType
     */
    @Override
    public ICommManager createCommPath(CommServiceType commType, String commPathName, String commFormat)
            throws IOException
    {
        return createCommPath(commType, commPathName, commFormat, -1, null);
    }

    @Override
    public ICommManager createCommPath(ICommService commService) throws IOException
    {
        Map<String, Serializable> props = new HashMap<>();

        if (null == commService)
        {
            throw new IllegalArgumentException("Parameter 'commService' must not be null");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Request to create comm service of type:  {} name:  {}",
                    CommServiceType.CUSTOM, commService.getName());
        }

        if (null != getExistingCommService(CommServiceType.CUSTOM, commService.getName()) && commService.isCommOpen())
        {
            throw new IOException("Comm already open.");
        }

        props.put(CommServiceConstants.RUNNING.toString(), true);
        props.put(CommServiceConstants.PORT_TYPE.toString(), CommServiceType.CUSTOM.toString());
        props.put(CommServiceConstants.PORT_NAME.toString(), commService.getName());

        commService.startCommService(props);
        addCommService(commService);

        return getExistingCommService(CommServiceType.CUSTOM, commService.getName());
    }

    /**
     * Create a comm path based on the type and name.  Adds a comm format string to the properties so
     * that the plugin developer can open ports with a custom format name, and choose when to or when not
     * to create parsers and framers for that format.
     *
     * @param commType     string type of port, i.e. "ETHERNET", "SERIAL"
     * @param commPathName String name of comm path "COM1", "127.0.0.1:1024"
     * @param commFormat   String custom format "MyDeviceComms", etc.
     * @param bufferSize   Requested size of the read buffer.  Comm service will use as it sees fit.
     * @return ICommManager manager for the comm service to allow for writing data.
     * @throws IOException if an error occurs while creating the comm path, such as a comm port being open already.
     * @see CommServiceType
     */
    @Override
    public ICommManager createCommPath(CommServiceType commType, String commPathName, String commFormat, int bufferSize) throws IOException
    {
        return createCommPath(commType, commPathName, commFormat, bufferSize, null);
    }

    /**
     * Create a comm path based on the type and name.  Adds a comm format string to the properties so
     * that the plugin developer can open ports with a custom format name, and choose when to or when not
     * to create parsers and framers for that format.
     *
     * @param commType     string type of port, i.e. "ETHERNET", "SERIAL"
     * @param commPathName String name of comm path "COM1", "127.0.0.1:1024"
     * @param commFormat   String custom format "MyDeviceComms", etc.
     * @param bufferSize   Requested size of the read buffer.  Comm service will use as it sees fit.
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state fo the comm path. {@code null} can be provided here if no listeners
     *                     want to be registered at this time. Listeners can be registered later using the
     *                     {@link ICommManager#addCommPathListener(ICommListener)} method. With a commType of
     *                     SERVER_SOCKET or SSL_SERVER_SOCKET, this commListener must be of type ICommSocketListener.
     * @return ICommManager manager for the comm service to allow for writing data. For commType of SERVER_SOCKET or
     * SSL_SERVER_SOCKET the return value will always be {@code null}. See ICommSocketListener or
     * ICommServiceDelegate#addServerSocketListener() for more details.
     * @throws IOException if an error occurs while creating the comm path, such as a comm port being open already.
     * @see CommServiceType
     */
    @Override
    public ICommManager createCommPath(CommServiceType commType, String commPathName, String commFormat, int bufferSize,
                                       ICommListener commListener)
            throws IOException
    {
        ICommManager commManager = null;

        logger.debug("Request to create comm service of type:  {} name:  {}", commType, commPathName);

        ICommService commService = getExistingCommService(commType, commPathName);

        if (null != commService)
        {
            //
            // GSM == SERIAL, but logically different
            if (commType == CommServiceType.SERIAL)
            {
                if (commService.isCommOpen())
                {
                    throw new IOException("Comm already open.");
                }
            } else
            {
                throw new IOException("Comm already open.");
            }
        }

        Map<String, Serializable> props = new HashMap<>();

        switch (commType)
        {
            case FILE:
                throw new UnsupportedOperationException("CommServiceType.FILE is not supported by this method.  Use createCommPath(String, InputStream) instead");
            case NONE:
                logger.debug("Requesting a null (empty) service of name {}", commPathName);

                if (null == commPathName)
                {
                    throw new IllegalArgumentException("CommServiceType.NONE must have a non-null commPathName");
                }

                ICommService nullCommService = new NullCommService(commPathName, commListener);

                props.put(CommServiceConstants.RUNNING.toString(), true);
                props.put(CommServiceConstants.PORT_FORMAT.toString(), commFormat);
                props.put(CommServiceConstants.PORT_TYPE.toString(), CommServiceType.NONE.toString());
                props.put(CommServiceConstants.PORT_NAME.toString(), commPathName);

                nullCommService.startCommService(props);
                addCommService(nullCommService);

                commManager = getExistingCommService(commType, commPathName);
                break;
            case CLIENT_SOCKET:
                logger.debug("Requesting CLIENT_SOCKET service of name: {}", commPathName);
                String[] portVars = commPathName.split(":");
                String hostName = portVars[0];
                String portNumber = portVars[1];
                if (hostName != null && portNumber != null)
                {
                    try
                    {
                        if (startClientSocket(commPathName, commFormat, bufferSize, commListener))
                        {
                            commManager = getExistingCommService(commType, commPathName);
                        } else
                        {
                            throw new IOException("Unable to connect to port on host " + hostName);
                        }
                    } catch (NumberFormatException e)
                    {
                        logger.error("Error parsing the port number from the ethernet port name");
                        throw e;
                    }
                } else
                {
                    logger.error("Error creating connection. Missing IP: {} or PORT: {}", hostName, portNumber);
                }
                break;
            case SERIAL:
                // TODO: Need to allow for custom baud rate
                logger.debug("Requesting SERIAL service of name: {}", commPathName);

                //noinspection VariableNotUsedInsideIf
                if (null == commService)
                {
                    commManager = createSerialCommPath(commPathName, 115200, false, null, commFormat, 0, 0, commListener);
                } else
                {
                    resetSerialPort(commPathName, commListener);
                }
                break;
            case SERVER_SOCKET:
            case SSL_SERVER_SOCKET:
                ICommSocketListener commSocketListener = null;
                if (commListener != null) // Checking for null here to prevent the error logged below if the caller does not want to provide an ICommSocketListener
                {
                    if (commListener instanceof ICommSocketListener)
                    {
                        commSocketListener = (ICommSocketListener) commListener;
                    } else
                    {
                        logger.error("When calling the createCommPath() method with a CommType of SERVER_SOCKET or SSL_SERVER_SOCKET, the ICommListener parameter must be of type ICommSocketListener.  " +
                                "It is also recommended to use the ICommServiceDelegate#addServerSocketListener() method instead of this createCommPath() method.");
                    }
                }

                // Parse out port, bind address, client address
                logger.debug("Requesting SERVER SOCKET service of name: {}", commPathName);
                String[] addressAndPort = commPathName.split(":");
                if (addressAndPort.length == 1)         // Just port
                {
                    // Start server socket listener
                    openServerSocket(Integer.parseInt(addressAndPort[0]), null, commSocketListener, commFormat,
                            CommServiceType.SSL_SERVER_SOCKET == commType);
                } else if (addressAndPort.length == 2)    // Just port and 1 address
                {
                    try
                    {
                        int port = Integer.parseInt(addressAndPort[1]);
                        // Start server socket listener
                        openServerSocket(port, InetAddress.getByName(addressAndPort[0]), commSocketListener, commFormat,
                                CommServiceType.SSL_SERVER_SOCKET == commType);
                    } catch (NumberFormatException e)         // Port not second value
                    {
                        logger.error("Invalid service name: {}", e.getMessage());
                    } catch (UnknownHostException e)
                    {
                        logger.error("Unknown host provided: {}", e.getMessage());
                    }
                }

                commManager = getExistingCommService(commType, commPathName);
                break;
            case DATAGRAM_SOCKET:
            case DATAGRAM_CLIENT:
                String[] dgPortVars = commPathName.split(":");
                logger.debug("Requesting DATAGRAM service of name: {}", commPathName);

                try
                {
                    int port;
                    String address;

                    if (dgPortVars.length == 1)        // At least have port
                    {
                        address = InetAddress.getByName("localhost").getHostAddress();
                        port = Integer.parseInt(dgPortVars[0]);
                    } else if (dgPortVars.length == 2)  // Have port and address
                    {
                        address = InetAddress.getByName(dgPortVars[0]).getHostAddress();
                        port = Integer.parseInt(dgPortVars[1]);
                    } else
                    {
                        logger.error("Invalid service name: {}", commPathName);
                        throw new IOException("Invalid service name: " + commPathName);
                    }

                    if (startUDPPort(port, address, commFormat, commType, bufferSize, commListener))
                    {
                        commManager = getExistingCommService(commType, address + ":" + port);
                    }
                } catch (NumberFormatException e)
                {
                    logger.error("Invalid service name: {}", e.getMessage());
                    throw new IOException(e);
                } catch (UnknownHostException e)
                {
                    logger.error("Can't get localhost IP: {}", e.getMessage());
                    throw new IOException(e);
                }
                break;

            default:
                logger.error("The CommServiceType: {} is not supported by the createCommPath method.", commType);
        }

        return commManager;
    }

    @Override
    public ICommManager createSerialCommPath(String commPathName, int baudRate, boolean echo, String commFormat)
            throws IOException
    {
        return createSerialCommPath(commPathName, baudRate, echo, null, commFormat);
    }

    @Override
    public ICommManager createSerialCommPath(String commPathName, int baudRate, boolean echo, String echoPort, String commFormat)
            throws IOException
    {
        return createSerialCommPath(commPathName, baudRate, echo, echoPort, commFormat, 0);
    }

    @Override
    public ICommManager createSerialCommPath(String commPathName, int baudRate, boolean echo, String echoPort, String commFormat, int txTimeout)
            throws IOException
    {
        return createSerialCommPath(commPathName, baudRate, echo, echoPort, commFormat, txTimeout, 0, null);
    }

    @Override
    public ICommManager createSerialCommPath(String commPathName, int baudRate, boolean echo, String echoPort, String commFormat, int txTimeout, int rxTimeout)
            throws IOException
    {
        return createSerialCommPath(commPathName, baudRate, echo, echoPort, commFormat, txTimeout, rxTimeout, null);
    }

    /**
     * Request through the API to get an ICommPath for an existing comm service.
     *
     * @param commType     String type of comm port
     * @param commPathName String name of the comm port
     * @return valid ICommPath if it exists, otherwise, {@code null}
     * @see CommServiceType
     */
    @Override
    public ICommPath getExistingCommPath(CommServiceType commType, String commPathName)
    {
        return getExistingCommService(commType, commPathName);
    }

    /**
     * Allows, through the API to start a server that listens on a port for connections.
     *
     * @param port        port number
     * @param bindAddress Address to bind to, in the case of multiple NIC cards (not required)
     * @param listener    A listener for listening to accepted connections.
     * @param commFormat  String name for the format of the data
     * @return true on success, or false if the port is already in use by another plugin.
     */
    @Override
    public boolean openSSLServerSocket(int port, InetAddress bindAddress, ICommSocketListener listener, String commFormat)
    {
        return openServerSocket(port, bindAddress, listener, commFormat, true);
    }

    /**
     * Allows, through the API to start a server that listens on a port for connections.
     *
     * @param port        port number
     * @param bindAddress Address to bind to, in the case of multiple NIC cards (not required)
     * @param listener    A listener for listening to accepted connections.
     * @param commFormat  String name for the format of the data
     * @return true on success, or false if the port is already in use by another plugin.
     */
    @Override
    public boolean openServerSocket(int port, InetAddress bindAddress, ICommSocketListener listener, String commFormat)
    {
        return openServerSocket(port, bindAddress, listener, commFormat, false);
    }

    /**
     * Allows, through the API to take ownership of a pre-configured server socket and listen for connections.
     *
     * @param serverSocket The server socket to take ownership of
     * @param listener     A listener for listening to accepted connections.
     * @param commFormat   String name for the format of the data
     * @return true on success, or false if the port is already in use by another plugin.
     */
    @Override
    public boolean handleServerSocket(ServerSocket serverSocket, ICommSocketListener listener, String commFormat)
    {
        String serviceName = getServiceName(serverSocket.getInetAddress(), serverSocket.getLocalPort());
        IServerSocketListener socketListener = socketListeners.get(serviceName);
        if (socketListener == null)
        {
            final CommServiceType commServiceType = serverSocket instanceof SSLServerSocket
                    ? CommServiceType.SSL_SERVER_SOCKET
                    : CommServiceType.SERVER_SOCKET;

            socketListener = new ServerSocketListener(serverSocket, listener, commFormat, commServiceType);
            socketListeners.put(serviceName, socketListener);
            logger.debug("Started server socket on port:  {}", serviceName);
            return true;
        } else
        {
            logger.warn("Request to open Server socket already running on port {}", serviceName);
            return false;
        }
    }

    /**
     * Close a server socket listener that was started with {@code openServerSocket} or {@code handleServerSocket}
     *
     * @param port        port of server to shutdown
     * @param bindAddress address that server is bound to
     * @see #openServerSocket
     */
    @Override
    public void closeServerSocket(int port, InetAddress bindAddress)
    {
        stopServerSocketListener(port, bindAddress);
    }

    @Override
    public ICommPath getServerSocketCommPath(int port, InetAddress bindAddress)
    {
        return socketListeners.get(getServiceName(bindAddress, port));
    }

    /**
     * Get comm services for a comm port type (Currently only implemented for type "SERIAL"
     *
     * @return a list of the status of comm services of the type requested.
     */
    @Override
    public Collection<ISerialPortStatus> getSerialPortCommStatus()
    {
        final Collection<ISerialPortStatus> commList = new ArrayList<>();
        for (String name : SerialCommService.getPortList())
        {
            if (logger.isTraceEnabled()) logger.trace("Get status for port: {}", name);

            boolean open = false;
            boolean echo = false;
            int baud = portBaudRates.getBaudRate(name);

            final ICommService commService = getExistingCommService(CommServiceType.SERIAL, name);
            if (commService != null)
            {
                final Map<String, Serializable> props = commService.getProperties();
                baud = (Integer) props.get(CommServiceConstants.BAUD.toString());
                echo = (Boolean) props.get(CommServiceConstants.ECHO.toString());
                open = commService.isCommOpen();
            }

            commList.add(new SerialPortStatus(name, baud, open, echo));
        }

        if (logger.isTraceEnabled()) logger.trace("Port list: {}", commList);

        return commList;
    }

    /**
     * Ability through the API to allow the reset of a serial port that may have gone south.
     *
     * @param sourceName name of serial port.
     */
    @Override
    public ICommService resetSerialPort(String sourceName)
    {
        return resetSerialPort(sourceName, null);
    }

    /**
     * Add a comm service and notify listener.  Exposed as a ICommServiceRegistry method so that, for instance,
     * a Server Socket can start comm services and notify the manager
     *
     * @param commService comm service being added.
     */
    @Override
    public void addCommService(final ICommService commService)
    {
        try
        {
            commServices.computeIfAbsent(KeyUtils.getUniqueKey(commService), (key) -> {
                logger.info("Added comm service number {}:  {}", commServices.size(), key);

                //
                // Do NOT create file logger if it is a FILE connection.
                //
                if (enableCommLogging &&
                        !commService.getType().toString().equals(CommServiceType.FILE.toString()) &&
                        !commService.getType().toString().equals(CommServiceType.NONE.toString()))
                {
                    logger.warn("Starting file logger for comm service {};, for testing use only!", key);

                    //
                    // Create a new instance of a file logger class that is ICommListener that echos all bytes[] to a file
                    //
                    commService.addCommPathListener(
                            new FileLoggerListener(commService.getType(), commService.getName()));
                }

                connectionAddedEvent.fire(new ConnectionAdded(commService));

                return commService;
            });
        }

        // Belt and Suspenders?  Or expecting a null commService?
        catch (NullPointerException e)
        {
            logger.error("Unable to add comm service", e);
        }
    }

    @Override
    public void flushPersistenceCache()
    {
        persistServiceProducer.cleanupPersistService();
    }

    /**
     * Notification from the comm service that said {@code ICommService} has been removed. Remove the
     * service from the list of services and the set list. Notify the listeners the commServices(s) are
     * being removed. Tell the comm service to "cleanup".
     *
     * @param commService comm service being removed
     */
    @Override
    public void terminateCommService(ICommService commService)
    {
        if (logger.isDebugEnabled()) logger.debug("Removing comm service {}", commService.getName());

        if (commService != null)
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Removed comm service:  {}", KeyUtils.getUniqueKey(commService));
                }
                commServices.remove(KeyUtils.getUniqueKey(commService));
                connectionRemovedEvent.fire(new ConnectionRemoved(commService));

                commService.cleanup();
            } catch (NullPointerException e)
            {
                logger.error("Unable to remove comm service", e);
            }
        }
    }

    @Override
    public ICommService getCommService(CommServiceType type, String name)
    {
        for (ICommService commService : commServices.values())
        {
            String commName = commService.getName();
            CommServiceType sourceType = commService.getType();
            if (sourceType == type && commName.equalsIgnoreCase(name))
            {
                return commService;
            }
        }
        return null;
    }

    @Override
    public void publishCommIn(String source)
    {
        communicationsEvent.fire(new CommunicationsEvent(source, CommunicationsEvent.CommDirection.CommIn));
    }

    @Override
    public void publishCommOut(String source)
    {
        communicationsEvent.fire(new CommunicationsEvent(source, CommunicationsEvent.CommDirection.CommOut));
    }

    /**
     * Crank up our service on the project.  Load our state from the DB.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_CommServiceManager) StartProjectServices event)
    {
        event.getProgressMonitor().setText("Starting Communication Services");
        logger.debug("Starting");

        portBaudRates = UserPreferenceEntry.COMM_PORTS_SERIAL_NODE.getNodeAs(PortBaudRates.class);

        // Start scanning for serial ports, if enabled
        if (enableSerialPorts)
        {
            portScannerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    getSerialPortCommStatus();
                }
            };

            // Check for new comm ports every 10 seconds
            timerService.scheduleAtFixedRate(portScannerTask, 0, TimeUnit.SECONDS.toMillis(10));
        }
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_CommServiceManager) StopProjectServices event)
    {
        event.getProgressMonitor().setText("Stopping Communication Services");
        logger.debug("Stopping");

        if (portScannerTask != null)
        {
            portScannerTask.cancel();
            portScannerTask = null;
        }

        stopAllOpenCommPorts();
        stopAllServerSocketListeners();
    }

    /**
     * Triggers the reset of a Serial Port. Also, if commListener is not {@code null}, the listener is registered as a
     * listener of the connection.
     *
     * @param sourceName   The name of the serial port comm path.
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state fo the comm path. {@code null} can be provided here if no listeners
     *                     want to be registered at this time. Listeners can be registered later using the
     *                     {@link ICommManager#addCommPathListener(ICommListener)} method.
     * @return The ICommService linked to the Serial Port connection.
     */
    private ICommService resetSerialPort(String sourceName, ICommListener commListener)
    {
        ICommService srv = null;

        for (ICommService commService : commServices.values())
        {
            Map<String, Serializable> props = commService.getProperties();
            if (props != null)
            {
                final String commName = commService.getName();

                if (CommServiceType.SERIAL == commService.getType() && commName.equalsIgnoreCase(sourceName))
                {
                    int baud = (Integer) props.get(CommServiceConstants.BAUD.toString());
                    boolean echo = (Boolean) props.get(CommServiceConstants.ECHO.toString());
                    String echoPort = (String) props.get(CommServiceConstants.ECHO_PORT.toString());
                    int txTimeout = (Integer) props.get(CommServiceConstants.TX_TIMEOUT.toString());
                    int rxTimeout = (Integer) props.get(CommServiceConstants.RX_TIMEOUT.toString());

                    stopSerialPort(sourceName);

                    String commFormat = (String) props.get(CommServiceConstants.PORT_FORMAT.toString());
                    srv = startSerialPort(sourceName, baud, true, echo, echoPort, commFormat, txTimeout, rxTimeout, commListener);
                    break;
                }
            }
        }

        return srv;
    }

    private ICommService getExistingCommService(CommServiceType commType, String commPathName)
    {
        ICommService srv = null;
        boolean wasFound = false;

        logger.trace("Getting existing comm service name:  {} of type {}", commPathName, commType);
        for (ICommService commService : commServices.values())
        {
            String commName = commService.getName();
            CommServiceType sourceType = commService.getType();

            logger.trace("Comparing with type {} with name {}", sourceType, commName);

            if (sourceType == commType && commName.equalsIgnoreCase(commPathName))
            {
                // If serial port is not open, open first
                if (!commService.isCommOpen() && CommServiceType.SERIAL == sourceType)
                {
                    srv = resetSerialPort(commName);
                } else
                {
                    srv = commService;
                }

                wasFound = true;

                logger.debug("Found existing comm service name:  {}", commPathName);
                break;
            }
        }

        if (!wasFound)
        {
            logger.trace("Couldn't find {} of type {} when looking through existing comm services", commPathName, commType);
        }

        return srv;
    }

    /**
     * Starts an ethernet port comm service.  If the ethernet service ALREADY exists (since services
     * can now be stopped without being removed), then just restart the service.
     *
     * @param sourceName   The comm service name
     * @param commFormat   String data format on the port.
     * @param bufferSize   buffer to use for each TCP/IP read operation, value of -1 will use the default.
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state fo the comm path. {@code null} can be provided here if no listeners
     *                     want to be registered at this time. Listeners can be registered later using the
     *                     {@link ICommManager#addCommPathListener(ICommListener)} method.
     * @return false on error, true on success
     * @see EthernetCommIn
     */
    private boolean startClientSocket(String sourceName, String commFormat, int bufferSize, ICommListener commListener)
    {
        if (logger.isDebugEnabled()) logger.debug("startEthernetPort sourceName = {}", sourceName);

        ICommService service = getExistingCommService(CommServiceType.CLIENT_SOCKET, sourceName);
        if (service == null)
        {
            Map<String, Serializable> props = new HashMap<>();
            props.put(CommServiceConstants.RUNNING.toString(), true);
            props.put(CommServiceConstants.PORT_TYPE.toString(), CommServiceType.CLIENT_SOCKET.toString());
            props.put(CommServiceConstants.PORT_NAME.toString(), sourceName);
            props.put(CommServiceConstants.PORT_FORMAT.toString(), commFormat);

            ICommService ethernetService = new EthernetCommService(bufferSize, commListener);
            if (logger.isDebugEnabled()) logger.debug("Attempting to open ethernet port {}", sourceName);
            if (ethernetService.startCommService(props))
            {
                addCommService(ethernetService);
                if (logger.isDebugEnabled()) logger.debug("Success opening ethernet port {}", sourceName);
                return true;
            } else    // error starting comm service
            {
                if (logger.isDebugEnabled()) logger.debug("Unsuccessful opening ethernet port {}", sourceName);
                return false;
            }
        } else
        {
            if (!service.isCommOpen())
            {
                EthernetCommService ethernetService = (EthernetCommService) service;
                if (ethernetService.restart())
                {
                    if (logger.isDebugEnabled()) logger.debug("Success re-opening ethernet port {}", sourceName);
                    return true;
                } else    // error starting comm service
                {
                    if (logger.isDebugEnabled()) logger.debug("Unsuccessful re-opening ethernet port {}", sourceName);
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Allows, through the API to start a server that listens on a port for connections.
     *
     * @param port        port number
     * @param bindAddress Address to bind to, in the case of multiple NIC cards (not required)
     * @param listener    A listener for listening to accepted connections.
     * @param commFormat  String name for the format of the data
     * @param ssl         Flag indicating if the connection should use SSL.
     * @return true on success, or false if the port is already in use by another plugin.
     */
    private boolean openServerSocket(int port, InetAddress bindAddress, ICommSocketListener listener, String commFormat, boolean ssl)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Starting a server socket bound to address: {} on port: {} with listener: {}", bindAddress, port, listener);
        }

        // Start server socket listener
        return startServerSocketListener(port, bindAddress, listener, commFormat, ssl);
    }

    /**
     * The common method that is leveraged by all the other "Create" methods for Serial Comm Path.
     */
    private ICommManager createSerialCommPath(String commPathName, int baudRate, boolean echo, String echoPort,
                                              String commFormat, int txTimeout, int rxTimeout, ICommListener commListener)
            throws IOException
    {
        ICommService commService = getExistingCommService(CommServiceType.SERIAL, commPathName);

        logger.debug("Requesting SERIAL port of name: {}", commPathName);
        if (null == commService)
        {
            ICommService newService = startSerialPort(commPathName, baudRate, true, echo, echoPort, commFormat, txTimeout, rxTimeout, commListener);
            if (!newService.isCommOpen())
            {
                commServices.remove(KeyUtils.getUniqueKey(newService));
                newService.cleanup();
                throw new IOException("Error opening comm port.");
            } else
            {
                return newService;
            }
        } else
        {
            if (commService.isCommOpen())
            {
                throw new IOException("Comm already open.");
            } else
            {
                return resetSerialPort(commPathName, commListener);
            }
        }
    }

    /**
     * Start a serial port connection.
     *
     * @param sourceName   The port name.
     * @param baudRate     The baud rate.
     * @param open         Indicates if the port should be opened.
     * @param echo         Indicates if the port's data should be echoed.
     * @param echoPort     The port to which data should be echoed. {@code null} or an empty String indicates that
     *                     data should be echoed back out of the receive port. {@code baudRate} will be used as the
     *                     echo port's baud rate.
     * @param commFormat   Custom comm format. May be {@code null} if no custom format is specified.
     * @param txTimeout    Transmit timeout in milliseconds. Set to 0 to use default value of 2^31.
     * @param rxTimeout    Receive timout in milliseconds.  Set to 0 to block indefinitely on a read request (default).
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state fo the comm path. {@code null} can be provided here if no listeners
     *                     want to be registered at this time. Listeners can be registered later using the
     *                     {@link ICommManager#addCommPathListener(ICommListener)} method.
     * @return The service encapsulating the serial port connection.
     */
    private ICommService startSerialPort(String sourceName, int baudRate, boolean open, boolean echo, String echoPort,
                                         String commFormat, int txTimeout, int rxTimeout, ICommListener commListener)
    {
        ICommService srv = null;
        if (!isCommServiceRunning(CommServiceType.SERIAL, sourceName))
        {
            final Map<String, Serializable> props = new HashMap<>();
            props.put(CommServiceConstants.RUNNING.toString(), open);
            props.put(CommServiceConstants.BAUD.toString(), baudRate);
            props.put(CommServiceConstants.PORT_TYPE.toString(), CommServiceType.SERIAL.toString());
            props.put(CommServiceConstants.PORT_NAME.toString(), sourceName);
            props.put(CommServiceConstants.ECHO.toString(), echo);
            props.put(CommServiceConstants.ECHO_PORT.toString(), echoPort == null ? "" : echoPort);
            props.put(CommServiceConstants.PORT_FORMAT.toString(), commFormat == null ? "" : commFormat);
            props.put(CommServiceConstants.TX_TIMEOUT.toString(), txTimeout);
            props.put(CommServiceConstants.RX_TIMEOUT.toString(), rxTimeout);

            logger.debug("startSerialPort: {} open = {}", sourceName, open);

            srv = new SerialCommService(commListener);
            srv.startCommService(props);
            addCommService(srv);

            portBaudRates.setBaudRate(sourceName, baudRate);
        }

        return srv;
    }

    /**
     * Start a server that will listen on a specified port.
     *
     * @param port        port number to listen on
     * @param bindAddress ip address to bind to
     * @param listener    listener that will receive notification of accepted clients
     * @param commFormat  String name format for data that will be transported on the socket
     * @return true on success, false on error.
     */
    private boolean startServerSocketListener(int port, InetAddress bindAddress, ICommSocketListener listener, String commFormat, boolean ssl)
    {
        final ServerSocket serverSocket;
        try
        {
            serverSocket = openServerSocket(port, bindAddress, ssl);
        } catch (IOException e)
        {
            logger.error("Failed to open server socket for port {}", getServiceName(bindAddress, port));
            return false;
        }

        return handleServerSocket(serverSocket, listener, commFormat);
    }

    private static String getServiceName(InetAddress bindAddress, int port)
    {
        if (bindAddress == null)
        {
            bindAddress = defaultInetAddress;
        }

        return bindAddress == null ? null : bindAddress + ":" + port;
    }

    private static ServerSocket openServerSocket(int port, InetAddress bindAddress, boolean ssl) throws IOException
    {
        ServerSocket serverSocket;
        ServerSocketFactory socketFactory = ssl
                ? SSLServerSocketFactory.getDefault()
                : ServerSocketFactory.getDefault();

        if (bindAddress != null)
        {
            serverSocket = socketFactory.createServerSocket(port, 50, bindAddress);
            logger.info("Starting {}server socket listener on port {} address {}", ssl ? "SSL " : "", port, bindAddress.getHostAddress());
        } else
        {
            serverSocket = socketFactory.createServerSocket(port);
            if (defaultInetAddress == null)
            {
                defaultInetAddress = serverSocket.getInetAddress();
            }
            logger.info("Starting {}server socket listener on port {} address {}", ssl ? "SSL " : "", port, serverSocket.getInetAddress());
        }
        return serverSocket;
    }

    private void stopServerSocketListener(int port, InetAddress bindAddress)
    {
        String serviceName = getServiceName(bindAddress, port);

        IServerSocketListener socketListener = null;
        if (serviceName != null)
        {
            socketListener = socketListeners.get(serviceName);
        }
        if (socketListener != null)
        {
            socketListener.stopSocketListener();
            socketListeners.remove(serviceName);
            logger.debug("Stopped server socket on port:  {}", serviceName);
        } else if (!platformUnderTest) // Only issue warning if we're not testing
        {
            logger.warn("Request to stop server socket NOT FOUND on port: {}.  NOTE:  This warning often comes from a connection that bound to an IP:Port and requested a disconnect using only the Port.  Use of the proper overloaded method to disconnect usually eliminates this warning.", serviceName);
        }
    }

    private boolean startUDPPort(int port, String ipAddress, String commFormat, CommServiceType type, int rBufferSize, ICommListener commListener)
    {
        String sourceName = ipAddress + ":" + port;
        ICommService service = getExistingCommService(type, sourceName);
        if (service == null)
        {
            Map<String, Serializable> props = new HashMap<>();
            props.put(CommServiceConstants.RUNNING.toString(), true);
            props.put(CommServiceConstants.PORT_TYPE.toString(), type);
            props.put(CommServiceConstants.PORT_NAME.toString(), sourceName);
            props.put(CommServiceConstants.PORT_FORMAT.toString(), commFormat);

            ICommService udpService = new DatagramSocketCommService(type, rBufferSize, commListener);
            logger.debug("Attempting to open UDP port {}", sourceName);
            if (udpService.startCommService(props))
            {
                logger.debug("Success opening UDP port {}", sourceName);
                addCommService(udpService);
                return true;
            } else
            {
                logger.debug("Error opening UDP port {}", sourceName);
                return false;
            }
        } else
        {
            return true;
        }
    }

    /**
     * Stop all of the running comm port threads.
     */
    private void stopAllOpenCommPorts()
    {
        commServices.values().forEach(this::removeCommService);
        commServices.clear();
    }

    private void stopSerialPort(String sourceName)
    {
        for (ICommService commService : commServices.values())
        {
            if (CommServiceType.SERIAL == commService.getType() && commService.getName().equals(sourceName))
            {
                logger.debug("Stopping comm service port: {}", sourceName);
                removeCommService(commService);
                commService.cleanup();
                return;
            }
        }
    }

    /**
     * Remove a comm service, and notify listeners.  Can be called as a ICommServiceRegistry method from
     * the Server Socket when it is shutting down any or all of the clients it has accepted.
     *
     * @param commService comm service
     */
    private void removeCommService(ICommService commService)
    {
        if (commService != null)
        {
            commService.stopCommService(true);
            try
            {
                commServices.remove(KeyUtils.getUniqueKey(commService));
                connectionRemovedEvent.fire(new ConnectionRemoved(commService));
            } catch (NullPointerException e)
            {
                logger.error("Unable to remove comm service", e);
            }
        }
    }

    /**
     * Checks serviceTracker to verify whether given port is already registered
     *
     * @param sourceType type of port: serial, ethernet, etc.
     * @param sourceName Name of the comm path to check for
     * @return true if given port is already registered, false if does not yet exist
     */
    private boolean isCommServiceRunning(CommServiceType sourceType, String sourceName)
    {
        for (ICommService commService : commServices.values())
        {
            String commName = commService.getName();
            CommServiceType commType = commService.getType();

            if (commType == sourceType && commName.equalsIgnoreCase(sourceName))
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("isCommServiceRunning, found commService {}", KeyUtils.getUniqueKey(sourceType, sourceName));
                }
                return true;
            }
        }

        return false;
    }

    private void stopAllServerSocketListeners()
    {
        socketListeners.values().forEach(IServerSocketListener::stopSocketListener);

        socketListeners.clear();
    }

    /**
     * Interns the user preferences node defining the baud rates for named serial ports.
     *
     * @since 2.35
     */
    private static final class PortBaudRates extends PreferencesNode
    {
        private static final int DEFAULT_BAUD_RATE = 15200;

        private final Map<String, Integer> baudRates;

        /**
         * Construct from the given preferences node.
         *
         * @param preferencesNode Node to internalize
         */
        PortBaudRates(PreferencesNode preferencesNode)
        {
            super(preferencesNode);

            baudRates = keyValuesAsIntMap(portName -> true, DEFAULT_BAUD_RATE);
        }

        int getBaudRate(String portName)
        {
            return baudRates.getOrDefault(portName, DEFAULT_BAUD_RATE);
        }

        void setBaudRate(String portName, int baudRate)
        {
            baudRates.put(portName, baudRate);
            put(portName, baudRate);
        }
    }
}
