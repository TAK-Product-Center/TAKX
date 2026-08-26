package gov.takx.api.plugin.connection;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.commservices.ICommServiceTerminationManager;
import gov.takx.api.commservices.IPortAnalyzerListener;
import gov.takx.api.commservices.ISerialPortAnalyzer;
import gov.takx.api.commservices.ISerialPortManager;
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

/**
 * Interface to delegate that can be used to open and close comm ports and to
 * stop and start socket servers.
 *
 */
@InjectableService
@NoPluginImplementations
@SuppressWarnings("UnusedDeclaration") // Called from plugins
public interface ICommServiceDelegate extends ISerialPortManager
{
    /**
     * Access the comm services termination manager. Typically this method is only invoked by a custom comm service
     * type that needs to notify the core that the comm service was terminated.
     *
     * @return Comm service termination manager
     */
    ICommServiceTerminationManager getCommServiceTerminationManager();

    /**
     * Creates a comm path based on type of comm port, name of port.
     * <p>
     * It <b>is possible</b> to establish multiple TCP/IP {@code CommServiceType.CLIENT_SOCKET} connections to the
     * same IP:PORT. However, if the commPathName is "127.0.0.1:1024" on the first connection and "127.0.0.1:1024" on
     * the second connection, an IOException that indicates the comm path already exists will be thrown. This is because
     * the platform relies on the commPathName as a unique key for looking up existing connections.
     * <p>
     * In order to establish multiple multiple TCP/IP {@code CommServiceType.CLIENT_SOCKET} connections to the
     * same IP:PORT, an additional colon-delimited suffix must be appended to the commPathName:
     * <p>
     * {@code createCommPath(CommServiceType.CLIENT_SOCKET, "127.0.0.1:1024:someUniqueKey1");}
     * {@code createCommPath(CommServiceType.CLIENT_SOCKET, "127.0.0.1:1024:someUniqueKey2");}
     * <p>
     * The Comm Service Manager automatically splits on the colon as a delimiter when the
     * {@code CommServiceType.CLIENT_SOCKET} is detected. Only the first two buckets of the resulting array, host
     * name and port number, are <i>actually</i> used to establish the connection. Thus, everything including and after
     * the second ':' in the commPathName is ignored by the physical connection logic, but used by the underlying
     * hash map used to track unique physical connections.
     *
     * @param commType     String type of port, "ETHERNET", "SERIAL"
     * @param commPathName String name of port, "COM1", "127.0.0.1:1024"
     * @return {@code null} if comm path cannot be created, or valid {@link ICommManager} instance if created successfully.
     * @throws java.io.IOException if comm path is not available
     * @see CommServiceType
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName) throws IOException;

    /**
     * Creates a truly custom ICommService for full control. This is a very advanced operation; please contact the
     * TAKX engineering team for detailed advice on how to implement this if you have determined that none of the
     * supported CommServiceType's will work in your use case.
     *
     * @param commService Custom comm service that must return {@link CommServiceType#CUSTOM}.
     * @return {@code null} if comm path cannot be created, or valid {@link ICommManager} instance if created successfully
     * @throws IOException Thrown on exceptions creating the comm path.
     */
    @Nullable
    ICommManager createCommPath(@Nonnull ICommService commService) throws IOException;

    /**
     * Creates a comm path based on type of comm port, name of port and a name for the format of the data.
     * <p>
     * See {@link #createCommPath(CommServiceType, String)} for additional documentation.
     *
     * @param commType     String type of port, "ETHERNET", "SERIAL"
     * @param commPathName String name of port, "COM1", "127.0.0.1:1024"
     * @param commFormat   String name of type of data input from socket
     * @return {@code null} if comm path cannot be created, or valid {@link ICommManager} instance if created successfully.
     * @throws IOException if comm path is not available
     * @see CommServiceType
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, String commFormat) throws IOException;

    /**
     * Creates a comm path based on type of comm port, name of port and a name for the format of the data
     * <p>
     * See {@link #createCommPath(CommServiceType, String)} for additional documentation.
     *
     * @param commType       String type of port, "ETHERNET", "SERIAL"
     * @param commPathName   String name of port, "COM1", "127.0.0.1:1024"
     * @param commFormat     String name of type of data input from socket
     * @param readBufferSize Requested size of read buffer in bytes, used for TCP/IP and UDP read operations.
     * @return {@code null} if comm path cannot be created, or valid {@link ICommManager} instance if created successfully.
     * @throws IOException if comm path is not available
     * @see CommServiceType
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, String commFormat,
                                int readBufferSize)
            throws IOException;

    /**
     * Creates a comm path based on type of comm port, name of port and a name for the format of the data.
     * <p>
     * Leveraging this method over the other createCommPath methods removes the need to call
     * {@link ICommManager#addCommPathListener(ICommListener)} as that method will automatically be called using the
     * provided {@code commListener}.  In addition, this method is beneficial when creating connections where the
     * remote end responds fairly fast.  There are use cases where creating a connection using one of the other
     * createCommPath methods, and immediately calling {@link ICommManager#addCommPathListener(ICommListener)} might
     * result in the listener not yet being registered before the incoming bytes are received from the remote end.
     * Leveraging this method prevents such a problem by registering the {@link ICommListener} immediately after the
     * connection is initiated.
     * <p>
     * If the {@code commType} is {@link CommServiceType#SSL_SERVER_SOCKET} or {@link CommServiceType#SERVER_SOCKET},
     * the provided commListener is ignored.
     * <p>
     * See {@link #createCommPath(CommServiceType, String)} for additional documentation.
     *
     * @param commType       String type of port, "ETHERNET", "SERIAL"
     * @param commPathName   String name of port, "COM1", "127.0.0.1:1024"
     * @param commFormat     String name of type of data input from socket
     * @param readBufferSize Requested size of read buffer in bytes, used for TCP/IP and UDP read operations.
     * @param commListener   The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                       and changes to the state fo the comm path.
     * @return {@code null} if comm path cannot be created, or valid {@link ICommManager} instance if created successfully.
     * @throws IOException if comm path is not available
     * @see CommServiceType
     */
    @Nullable
    ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, String commFormat,
                                int readBufferSize, ICommListener commListener)
            throws IOException;

    /**
     * Creates a file-based, input-only comm path based on the file's input stream.  In order to start the flow of data
     * it is necessary for the caller to invoke restart() on the {@link ICommManager}.  This approach ensures that the caller
     * has adequate time to attach listeners to the comm manager before the flow of data from the input stream starts.
     * Since this method does not allow a format to be specified, the file extension will be used as the format if the
     * file has a valid extension.
     *
     * @param absoluteFilePath Absolute path to the file associated with the input stream.  This will be used as the
     *                         name for the comm path.
     * @param inputStream      Non-null input stream where the data is to be read from.
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws IOException if comm path is not available
     */
    @Nullable
    ICommManager createCommPath(@Nonnull String absoluteFilePath, @Nonnull InputStream inputStream) throws IOException;

    /**
     * Creates a file-based, input-only comm path based on the file's input stream.  In order to start the flow of data
     * it is necessary for the caller to invoke restart() on the ICommManager.  This approach ensures that the caller
     * has adequate time to attach listeners to the comm manager before the flow of data from the input stream starts.
     *
     * @param absoluteFilePath Absolute path to the file associated with the input stream.  This will be used as the
     *                         name for the comm path.
     * @param inputStream      Non-null input stream where the data is to be read from.
     * @param commFormat       String name of type of data input from file
     * @return {@code null} if comm path cannot be created, or valid ICommManager instance if created successfully
     * @throws IOException if comm path is not available
     * @since 2.12
     */
    @Nullable
    ICommManager createCommPath(@Nonnull String absoluteFilePath, @Nonnull InputStream inputStream, String commFormat) throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         Indicates if data should be echoed. If {@code true}, data will be echoed out the receive comm port.
     * @param commFormat   format of expected data
     * @return {@link ICommManager}
     * @throws java.io.IOException exception creating comm path
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, String commFormat) throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         whether or not to echo the data
     * @param echoPort     the port to echo data to
     * @param commFormat   format of expected data
     * @return {@link ICommManager}
     * @throws IOException exception creating comm path
     * @since 1.25.7
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, String echoPort,
                                      String commFormat)
            throws IOException;

    /**
     * Create / Open a serial comm port.
     *
     * @param commPathName name of comm port
     * @param baudRate     baud rate on port
     * @param echo         whether or not to echo the data
     * @param echoPort     the port to echo data to
     * @param commFormat   format of expected data
     * @param timeout      time out a failed write after this many milliseconds (0 = use default)
     * @return {@link ICommManager}
     * @throws IOException exception creating comm path
     * @since 1.50.6
     */
    ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, String echoPort,
                                      String commFormat, int timeout)
            throws IOException;

    /**
     * Get the ICommPath interface for a comm port that has already been opened, maybe even by another plugin.
     * <p>
     * See {@link #createCommPath(CommServiceType, String)} for additional documentation.
     *
     * @param commType     String type of port, "ETHERNET", "SERIAL".
     * @param commPathName String name of port, "COM1", "127.0.0.1:1024"
     * @return {@code null} if comm path does not exist, or valid ICommPath instance if it exists.
     */
    ICommPath getExistingCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName);

    /**
     * Request a server socket listen on a specified port.
     *
     * @param port       the local TCP port
     * @param commFormat String name of type of data input from socket
     * @return {@code true} on success, {@code false} otherwise
     */
    boolean addServerSocketListener(int port, String commFormat);

    /**
     * Request a server socket listen on a specified port, with the additional parameter of opening
     * client sockets with a specific comm format property.
     *
     * @param port       the local TCP port
     * @param listener   ICommListener for notification on connection of new client, receive bytes
     * @param commFormat String name of type of data input from socket
     * @return {@code true} on success, or {@code false} if TAKX already has a server running on that port
     */
    boolean addServerSocketListener(int port, @Nullable ICommSocketListener listener, String commFormat);

    /**
     * Request a server socket listen on a specified port, with the additional parameter of opening
     * client sockets with a specific comm format property and BOUND to a specific InetAddress.
     *
     * @param port        the local TCP port
     * @param bindAddress InetAddress to bind server to
     * @param listener    ICommListener for notification on connection of new client, receive bytes
     * @param commFormat  String name of type of data input from socket
     * @return {@code true} on success, or {@code false} if TAKX already has a server running on that port
     * @since 1.3
     */
    boolean addServerSocketListener(int port, InetAddress bindAddress, @Nullable ICommSocketListener listener, String commFormat);

    /**
     * Request a communication service to accept connections on the provided pre-configured server socket,
     * with the additional parameter of opening client sockets with a specific comm format property.
     *
     * @param serverSocket The listening server socket to service
     * @param commFormat   String name of type of data input from socket
     * @return {@code true} on success, or {@code false} if TAKX already has a server running on that port
     * @since 2.1
     */
    boolean addServerSocketListener(@Nonnull ServerSocket serverSocket, String commFormat);

    /**
     * Request a communication service to accept connections on the provided pre-configured server socket,
     * with the additional parameter of opening client sockets with a specific comm format property.
     *
     * @param serverSocket The listening server socket to service
     * @param listener     ICommListener for notification on connection of new client, receive bytes
     * @param commFormat   String name of type of data input from socket
     * @return {@code true} on success, or {@code false} if TAKX already has a server running on that port
     * @since 2.1
     */
    boolean addServerSocketListener(@Nonnull ServerSocket serverSocket, @Nullable ICommSocketListener listener, String commFormat);

    /**
     * Request a SSL server socket listen on a specified port. This method presumes that the keystore has been properly
     * configured and is referenced in the mandatory system properties.
     *
     * @param port       the local TCP port
     * @param commFormat String name of type of data input from socket
     * @return {@code true} on success, {@code false} otherwise
     */
    boolean addSSLServerSocketListener(int port, String commFormat);

    /**
     * Request a SSL server socket listen on a specified port, with the additional parameter of opening client sockets
     * with a specific comm format property. This method presumes that the keystore has been properly configured and is
     * referenced in the mandatory system properties.
     *
     * @param port       the local TCP port
     * @param listener   ICommListener for notification on connection of new client, receive bytes
     * @param commFormat String name of type of data input from socket
     * @return {@code true} on success, or {@code false} if TAKX already has a server running on that port
     * @since 1.34
     */
    boolean addSSLServerSocketListener(int port, @Nullable ICommSocketListener listener, String commFormat);

    /**
     * Request a SSL server socket listen on a specified port, with the additional parameter of opening client sockets
     * with a specific comm format property and BOUND to a specific InetAddress. This method presumes that the keystore
     * has been properly configured and is referenced in the mandatory system properties.
     *
     * @param port        the local TCP port
     * @param bindAddress InetAddress to bind server to
     * @param listener    ICommListener for notification on connection of new client, receive bytes
     * @param commFormat  String name of type of data input from socket
     * @return {@code true} on success, or {@code false} if TAKX already has a server running on that port
     * @since 1.34
     */
    boolean addSSLServerSocketListener(int port, InetAddress bindAddress, @Nullable ICommSocketListener listener, String commFormat);

    /**
     * Shutdown a server socket (and any clients that have been accepted)
     *
     * @param port port number of server socket
     */
    void removeServerSocketListener(int port);

    /**
     * Shutdown a server socket (and any clients that have been accepted) bound to a specified InetAddress
     *
     * @param port        port number of server socket
     * @param bindAddress InetAddress server is bound to.
     * @since 1.3
     */
    void removeServerSocketListener(int port, InetAddress bindAddress);

    /**
     * Get the {@code ICommPath} associated with an existing server socket listener.
     *
     * @param port port number of server socket
     * @return comm path, or {@code null} if there is no socket listener for the given port (and default bind address)
     * @since 2.20
     */
    @Nullable
    ICommPath getServerSocketCommPath(int port);

    /**
     * Get the {@code ICommPath} associated with an existing server socket listener.
     *
     * @param port        port number of server socket
     * @param bindAddress InetAddress server is bound to.
     * @return comm path, or {@code null} if there is no socket listener for the given address/port
     * @since 2.20
     */
    @Nullable
    ICommPath getServerSocketCommPath(int port, InetAddress bindAddress);

    /**
     * Analyze serial ports for recognizable data.  Listener will receive updates from the scanner passed on
     * from plugins which implement ISerialPortAnalyzer.
     *
     * @param listener       listener to receive updates as to the scan status
     * @param baudRate       baud rate to do the scan at
     * @param timeoutSeconds number of seconds to scan each port for valid data
     * @see ISerialPortAnalyzer
     * @since 1.35
     */
    void analyzeSerialPorts(@Nonnull IPortAnalyzerListener listener, int baudRate, int timeoutSeconds);

    /**
     * Suspend the analyzing of serial ports, as started with the method analyzeSerialPorts.
     *
     * @since 1.35
     */
    void suspendAnalyzeSerialPorts();
}
