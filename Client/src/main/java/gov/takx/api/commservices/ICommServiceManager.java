package gov.takx.api.commservices;

import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommManager;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.commservices.ICommSocketListener;
import gov.takx.commons.constants.CommServiceType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;

/**
 * @see gov.takx.api.plugin.connection.ICommServiceDelegate
 */
@SuppressWarnings("unused")  // some methods only called by plugins
public interface ICommServiceManager
{
    /**
     * Registers a new comm service for tracking.
     *
     * @param commService Comm service to add
     */
    void addCommService(@Nonnull ICommService commService);

    /**
     * Locate an existing comm service of the specific service type with the given name.
     *
     * @param type Comm service type
     * @param name Comm service name
     * @return Comm service that matches the parameters, or {@code null} if no match is found
     */
    @Nullable
    ICommService getCommService(@Nonnull CommServiceType type, @Nonnull String name);

    /**
     * Creates a comm path.
     *
     * @param commType     Comm type determines which well-known virtual comm path enveloping implementation is used.
     * @param commPathName Logical name of the comm path being created
     * @param commFormat   Comm format
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws java.io.IOException exception creating comm path
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, @Nullable String commFormat) throws IOException;

    /**
     * Creates a truly custom comm path.
     *
     * @param commService Custom comm service
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws java.io.IOException exception creating comm path
     */
    @Nullable
    ICommManager createCommPath(@Nonnull ICommService commService) throws IOException;

    /**
     * Creates a comm path and in addition requests a read buffer size
     *
     * @param commType     Comm type determines which well-known virtual comm path enveloping implementation is used.
     * @param commPathName Logical name of the comm path being created
     * @param commFormat   Comm format
     * @param bufferSize   Requested size of read buffer in bytes
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws java.io.IOException exception creating comm path
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName,
                                @Nullable String commFormat, int bufferSize)
            throws IOException;

    /**
     * Creates a comm path and in addition requests a read buffer size and also registers an ICommListener.
     *
     * @param commType     Comm type determines which well-known virtual comm path enveloping implementation is used.
     * @param commPathName Logical name of the comm path being created
     * @param commFormat   Comm format
     * @param bufferSize   Requested size of read buffer in bytes
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state fo the comm path.  {@code null} can be provided here if no listeners
     *                     want to be registered at this time.  Listeners can be registered later using the
     *                     {@link ICommManager#addCommPathListener(ICommListener)} method.
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws java.io.IOException exception creating comm path
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName,
                                @Nullable String commFormat, int bufferSize, @Nullable ICommListener commListener)
            throws IOException;

    /**
     * Creates a file-based comm path based on an input stream.  In order to start the flow of data it is necessary for
     * the caller to invoke restart() on the ICommManager.  This approach ensures that the caller has adequate time
     * to attach listeners to the comm manager before the flow of data from the input stream starts.
     *
     * @param absoluteFilePath The absolute path of the file.  This will be used as the name for the comm path.
     * @param inputStream      Non-null input stream where the data is to be read from.
     * @param commFormat       String name of type of data input from file.
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws java.io.IOException exception creating comm path
     */
    @Nullable
    ICommManager createCommPath(@Nonnull String absoluteFilePath, @Nonnull InputStream inputStream, @Nullable String commFormat) throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         whether or not to echo the data
     * @param commFormat   format of expected data
     * @return ICommManager
     * @throws java.io.IOException exception creating comm path
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, @Nullable String commFormat) throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         whether or not to echo the data
     * @param echoPort     the echo port
     * @param commFormat   format of expected data
     * @return ICommManager
     * @throws java.io.IOException exception creating comm path
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo,
                                      @Nullable String echoPort, @Nullable String commFormat)
            throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         whether or not to echo the data
     * @param echoPort     the echo port
     * @param commFormat   format of expected data
     * @param txTimeout    transmit timeout in milliseconds (0 = use default)
     * @return ICommManager
     * @throws java.io.IOException exception creating comm path
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo,
                                      @Nullable String echoPort, @Nullable String commFormat, int txTimeout)
            throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         whether to echo the data
     * @param echoPort     the echo port
     * @param commFormat   format of expected data
     * @param txTimeout    transmit timeout in milliseconds (0 = use default)
     * @param rxTimeout    receive timeout in milliseconds, 0 to block indefinitely (default)
     * @return {@link ICommManager}
     * @throws java.io.IOException exception creating comm path
     * @since 3.4.0
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo,
                                      @Nullable String echoPort, @Nullable String commFormat, int txTimeout, int rxTimeout)
            throws IOException;

    /**
     * Attempts to locate a comm port with specified data format
     *
     * @param commType     Ethernet, Serial, etc...
     * @param commPathName Name of the desired comm path (ie "COM2")
     * @return {@link ICommPath} or {@code null} if it doesn't exist
     */
    ICommPath getExistingCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName);

    /**
     * Start a server listening on a port. Will accept connections and send notification to listener. This method
     * presumes that the keystore has been properly configured and is referenced in the mandatory system properties.
     *
     * @param port        the local TCP port
     * @param bindAddress the local InetAddress the server will bind to, {@code null} for default
     * @param listener    ICommListener for notification on connection of new client, receive bytes (can be {@code null})
     * @param commFormat  String type of data expected on port
     * @return true on success, false if there's already a TAKX server running on that port
     * @since 1.34
     */
    boolean openSSLServerSocket(int port, @Nullable InetAddress bindAddress, @Nullable ICommSocketListener listener,
                                @Nullable String commFormat);

    /**
     * Start a server listening on a port.  Will accept connections and
     * send notification to listener.
     *
     * @param port        the local TCP port
     * @param bindAddress the local InetAddress the server will bind to, {@code null} for default
     * @param listener    ICommListener for notification on connection of new client, receive bytes (can be {@code null})
     * @param commFormat  String type of data expected on port
     * @return true on success, false if there's already a TAKX server running on that port
     */
    boolean openServerSocket(int port, @Nullable InetAddress bindAddress, @Nullable ICommSocketListener listener,
                             @Nullable String commFormat);

    /**
     * Start accepting connections on an already-open server socket.  Will accept connections and send notification
     * to listener.
     *
     * @param serverSocket The pre-configured server socket
     * @param listener     ICommListener for notification on connection of new client, receive bytes (can be {@code null})
     * @param commFormat   String type of data expected on port
     * @return true on success, false if there's already a TAKX server running on that port
     */
    boolean handleServerSocket(@Nonnull ServerSocket serverSocket, @Nullable ICommSocketListener listener,
                               @Nullable String commFormat);

    void closeServerSocket(int port, InetAddress bindAddress);

    /**
     * Get the {@code ICommPath} associated with an existing server socket listener.
     *
     * @param port        the local TCP port
     * @param bindAddress the local InetAddress the server is bound to, {@code null} for default
     * @return comm path, or {@code null} if there is no socket listener for the given address/port
     * @since 2.20
     */
    @Nullable
    ICommPath getServerSocketCommPath(int port, @Nullable InetAddress bindAddress);

    /**
     * Retrieve a current collection of serial port status data.
     *
     * @return Non-null collection of serial port status data
     */
    @Nonnull
    Collection<ISerialPortStatus> getSerialPortCommStatus();

    ICommService resetSerialPort(String sourceName);
}
