package gov.takx.api.plugin.gateway;

import java.io.IOException;

/**
 * Delegate that should be injected into the IGatewayFramer
 *
 */
@SuppressWarnings("unused") // Called from plugins
public interface IGatewayFramerDelegate
{
    /**
     * Send a byte array to the comm port.
     *
     * @param bytes  byte array
     * @param length number of bytes
     * @throws IOException write exception
     */
    void sendToCommOut(byte[] bytes, int length) throws IOException;

    /**
     * Send a byte array to the comm port.
     *
     * @param bytes    byte array
     * @param length   number of bytes
     * @param hostname String hostname to send the array to (for UDP use IP "127.0.0.1" or IP:port "127.0.0.1:1234")
     * @throws IOException write exception
     */
    void sendToCommOut(byte[] bytes, int length, String hostname) throws IOException;

    /**
     * Send a text string to the comm port.  Note: Text output will append /r/n to each line.
     *
     * @param text String text
     * @throws IOException write exception
     */
    void sendToCommOut(String text) throws IOException;

    /**
     * @return The unique key of the device (plugin) that created the parser
     * @since 2.0.0
     */
    String getDeviceUniqueKey();
}
