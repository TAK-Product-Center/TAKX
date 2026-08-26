package gov.takx.commons.constants;

/**
 * Comm service type.
 *
 */

public enum CommServiceType
{
    /**
     * Truly custom comm service.
     */
    CUSTOM,

    /**
     * Serial communication port, e.g. COM3
     */
    SERIAL,

    /**
     * TAKX connects to a remote server as a client over a TCP/IP socket.
     */
    CLIENT_SOCKET,

    /**
     * TAKX acts as a server, accepting connections from remote clients over a TCP/IP socket.
     * This refers to the clients that are accepted by the server.
     */
    SERVER_SOCKET,

    /**
     * TAKX acts as an SSL server, accepting connections from remote clients over a TCP/IP socket.
     * This refers to the clients that are accepted by the server.
     *
     * @since 1.34
     */
    SSL_SERVER_SOCKET,

    /**
     * Datagram socket bound to a specific port and address.  Automatically switches to a MulticastSocket if
     * isMulticastAddress() returns true for the address specified.
     */
    DATAGRAM_SOCKET,

    /**
     * Datagram socket bound to any available port on the local machine.
     */
    DATAGRAM_CLIENT,

    /**
     * Iridium modem connection.
     */
    IRIDIUM,

    /**
     * File connection.  Must use the createCommPath() call that accepts an InputStream.
     */
    FILE,

    /**
     * {@code null} connection. Highly useful when you need to create a connection for the sole purposes of registering
     * parsers and framers. For example, let's say that the plugin needed to perform some type of JNI wrapped
     * IPC to a specialized external piece of software that managed the actual (physical) connection for more software
     * than just TAKX. Using a NONE for the connection type will result in a {@code null} CommServiceManager, but it will
     * trigger the creation of parsers and framers whereby the parser can register as the event sink to receive the
     * messages from the external piece of software and turn them into RDMs.  Any other plugins that happen to
     * register on this connection would never see data over their parseBytes() method.
     */
    NONE,
}
