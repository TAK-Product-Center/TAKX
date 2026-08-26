package gov.takx.api.platform;

/**
 * Low level byte array parser.
 *
 */
public interface IParser
{
    /**
     * Parse a byte array of data of length bytes, originating from source location (if known)
     *
     * @param data   byte array of data
     * @param length number of bytes in array
     * @param source String representation of source, can be {@code null} if source is unknown
     *               (ie for UDP data"127.0.0.1:1234" for local IP address on port 1234)
     */
    void parseBytes(byte[] data, int length, String source);

    /**
     * Shutdown the parser.  Release any resources that need to be released.
     */
    void shutdown();
}
