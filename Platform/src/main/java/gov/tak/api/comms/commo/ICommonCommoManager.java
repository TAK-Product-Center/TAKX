package gov.tak.api.comms.commo;

import com.atakmap.commoncommo.CoTMessageListener;
import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.ContactPresenceListener;
import com.atakmap.commoncommo.GenericDataListener;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Vector;

/**
 * Contract for Common Commo service manager.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ICommonCommoManager
{
    /**
     * Add a CoT message listener.
     *
     * @param listener Listener to add
     */
    void addCotMessageListener(@Nonnull CoTMessageListener listener);

    /**
     * Remove a CoT message listener.
     *
     * @param listener Listener to remove
     */
    void removeCotMessageListener(@Nonnull CoTMessageListener listener);

    /**
     * Add a generic data listener
     *
     * @param listener Listener to add
     */
    void addGenericDataListener(@Nonnull GenericDataListener listener);

    /**
     * Remove a generic data listener
     *
     * @param listener Listener to remove
     */
    void removeGenericDataListener(@Nonnull GenericDataListener listener);

    /**
     * Add a {@link gov.tak.api.cot.event.CotEvent CoT Event} listener.
     *
     * @param listener Listener to add
     */
    void addCotEventListener(@Nonnull ICotEventListener listener);

    /**
     * Remove a {@link gov.tak.api.cot.event.CotEvent CoT Event} listener.
     *
     * @param listener Listener to remove
     */
    void removeCotEventListener(@Nonnull ICotEventListener listener);

    /**
     * Add a CoT message send listener.  Listener is notified on any CoT message being sent via the following methods:
     * <ul>
     *     <li>{@link ICommonCommoManager#broadcastCoT(String)}</li>
     *     <li>{@link ICommonCommoManager#broadcastCoT(String, CoTSendMethod)}</li>
     *     <li>{@link ICommonCommoManager#sendCoT(Vector, String, CoTSendMethod)}</li>
     *     <li>{@link ICommonCommoManager#sendCotToMission(ITakServerConnection, String, String)}</li>
     * </ul>
     *
     * @param listener Listener to add
     * @since 5.3
     */
    void addCotSendListener(@Nonnull ICotSendListener listener);

    /**
     * Remove a CoT message send listener.
     *
     * @param listener Listener to remove
     * @since 5.3
     */
    void removeCotSendListener(@Nonnull ICotSendListener listener);

    /**
     * Add a Contact Presence Listener.
     *
     * @param listener Listener to add
     */
    void addContactPresenceListener(@Nonnull ContactPresenceListener listener);

    /**
     * Remove a Contact Presence Listener.
     *
     * @param listener Listener to remove
     */
    void removeContactPresenceListener(@Nonnull ContactPresenceListener listener);

    /**
     * @param interceptor Interceptor to add
     * @apiNote Interceptors are invoked in the order they are added.
     * @since 5.8
     */
    void addCotEventInterceptor(@Nonnull ICotEventInterceptor interceptor);

    /**
     * @param interceptor Interceptor to remove
     * @since 5.8
     */
    void removeCotEventInterceptor(@Nonnull ICotEventInterceptor interceptor);

    /**
     * Add an input address for Common Commo to listen to in addition to those configured via preferences
     *
     * @param inputUri       input URI to listen to
     * @param forGenericData whether this address will be used for Cot Events or non-CoT messages
     */
    void addInput(@Nonnull URI inputUri, boolean forGenericData);

    /**
     * Removes an input address for Common Commo to stop listening to
     *
     * @param inputUri input URI to stop listening to
     */
    void removeInput(@Nonnull URI inputUri);

    /**
     * Sends a mission package (data package) to a contact.
     *
     * @param contact Contact to send the mission package to.
     * @param missionPackageFile File path to local mission package.
     *
     * @since 3.5
     */
    void sendMissionPackage(@Nonnull Contact contact, @Nonnull File missionPackageFile) throws CommoException;

    /**
     * Sends a mission package (data package) to a list of contacts.
     *
     * @param contacts Contacts to send the mission package to.
     * @param missionPackageFile File path to local mission package.
     *
     * @since 3.5
     */
    void sendMissionPackage(@Nonnull List<Contact> contacts, @Nonnull File missionPackageFile) throws CommoException;

    /**
     * Broadcast a CoT message to all configured broadcast output interfaces.
     *
     * @param cotMessage CoT message to send
     */
    void broadcastCoT(@Nonnull String cotMessage) throws CommoException;

    /**
     * Broadcast a CoT message to all configured broadcast output interfaces.
     *
     * @param cotMessage CoT message to send
     * @param sendMethod Method to use for sending
     */
    void broadcastCoT(@Nonnull String cotMessage, @Nonnull CoTSendMethod sendMethod) throws CommoException;

    /**
     * Sends a CoT message to a mission on the given connection.
     * It will be automatically added to the mission assuming the user defined in the {@code connection} has permission to modify the target mission.
     *
     * @param connection Connection where the mission is defined in.
     * @param cotMessage CoT message to send.
     * @param missionName Name of mission to add the CoT item to.
     *
     * @since 5.0
     */
    void sendCotToMission(@Nonnull ITakServerConnection connection, @Nonnull String cotMessage, @Nonnull String missionName) throws CommoException;

    /**
     * Send a CoT message to the specified Contacts.
     *
     * @param contacts   Contacts to which to send the message
     * @param cotMessage CoT Message to send
     * @param sendMethod Method to use for sending
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    void sendCoT(@Nonnull Vector<Contact> contacts, @Nonnull String cotMessage, @Nonnull CoTSendMethod sendMethod) throws CommoException;

    /**
     * Enables or disables (the default) the ability of datagram sockets created by the system to share their local
     * address with other sockets on the system created by other applications.  Note that this option is unreliable
     * when unicast datagrams are sent to the local system, as it is indeterminate which local socket will receive the
     * unicast datagrams.  Use this option with caution if you expect to be able to receive unicast datagrams.
     *
     * @param enable Pass {@code true} to allow address reuse, {@code false} otherwise
     * @since 5.6
     */
    void setEnableAddressReuse(boolean enable);

    /**
     * Enables or disables (the default) whether sent multicast packets should be looped back to local sockets.
     * This is the same as setting the socket option {@code IP_MULTICAST_LOOP}.
     *
     * @apiNote This is only functional on Linux, it has no effect on Windows.
     *
     * @param enable Pass {@code true} to enable loopback, {@code false} to disable it
     * @since 5.8
     */
    void setMulticastLoopbackEnabled(boolean enable);

    /**
     * Configures a "known streaming endpoint" for a contact. The {@code endpointId} must refer to a valid server endpoint
     * from an existing {@link com.atakmap.commoncommo.StreamingNetInterface}. Messages sent to the contact will be sent 
     * using the identified streaming interface only when the identified contact lacks an active endpoint discovered via
     * other contact discovery means.
     * <p>
     * If an endpoint for the specified contact {@code uid} has already been configured, it will be replaced with the
     * supplied configuration.
     * <p>
     * To remove an endpoint configuration, pass {@code null} for both the {@code callsign} and {@code endpointId}.
     * Passing {@code null} for only of the parameters will result in an {@link IllegalArgumentException}.
     *
     * @param uid The contact's UID
     * @param callsign The contact's callsign (or {@code null} to remove the config; {@code endpointId} must also be {@code null})
     * @param endpointId The id of an existing StreamingNetInterface (or {@code null} to remove the config; {@code callsign} must also be {@code null})
     * @return The resulting commo {@link Contact}
     * @throws IllegalArgumentException if either {@code callsign} or {@code endpointId} is {@code null}, but not both
     * @since 5.7
     */
    @IncubatingApi(since = "5.7")
    @Nullable
    Contact configKnownEndpointForContact(@Nonnull String uid, @Nullable String callsign, @Nullable String endpointId);
}
