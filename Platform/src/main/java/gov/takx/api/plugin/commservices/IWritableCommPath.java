package gov.takx.api.plugin.commservices;

import java.io.IOException;

/**
 * Interface for comm services that can be written to. Returned by the ICommManager to give the
 * ability to write data out a comm path.
 *
 * @see ICommManager
 */
public interface IWritableCommPath
{
    /**
     * Write an array of bytes
     *
     * @param data   array of bytes
     * @param length length of byte array
     * @throws IOException write exception
     */
    void write(byte[] data, int length) throws IOException;

    /**
     * Write an array of bytes to a specific host name
     *
     * @param data     data array of bytes
     * @param length   length length of byte array
     * @param location host name to write to (for UDP use IP:port "127.0.0.1:1234"), or phone number for GSM delivery
     * @throws IOException write exception
     */
    void write(byte[] data, int length, String location) throws IOException;

    /**
     * Write text string
     *
     * @param text String text
     * @throws IOException write exception
     */
    void write(String text) throws IOException;
}
