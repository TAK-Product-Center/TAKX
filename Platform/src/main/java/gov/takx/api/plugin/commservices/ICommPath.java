package gov.takx.api.plugin.commservices;

import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Interface for a comm path, to which a ICommListener can add/remove itself as a listener.  The ICommPath
 * is returned when an IConnection implementation requests to get an existing comm path, possibly opened by
 * another connection.  The method getExistingCommPath is accessible through the IConnectionCommServiceDelegate
 *
 */
public interface ICommPath
{
    String TYPE = "TYPE";
    String FORMAT = "FORMAT";
    String IPADDRESS = "IPADDRESS";
    String PORT = "PORT";

    /**
     * Listener will get notifications of input bytes and changes to the state
     * of the comm path.
     *
     * @param listener listener to be added
     */
    void addCommPathListener(@Nonnull ICommListener listener);

    /**
     * Listener will get notification of input bytes and changes to the state
     * of the comm path
     *
     * @param listener listener to be removed
     */
    void removeCommPathListener(@Nonnull ICommListener listener);

    /**
     * Get the name of the comm path
     *
     * @return comm path name
     */
    String getName();

    /**
     * These are the properties that are available to the plugin developer using ICommPath.  Basically contains
     * the type (see CommServiceType enum), format (as supplied by plugin developer), ipaddress (if applicable)
     * and port (port number if applicable, or the name of the port in the case of a serial port).  Properties
     * can be references through the static definitions at the top of this file.
     *
     * @return Map of properties.
     */
    Map<String, String> getCommPathProperties();
}
