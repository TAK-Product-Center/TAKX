package gov.takx.api.plugin.device;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.gateway.IGatewayController;

/**
 * Standard shared map object communication methods used within the UI ({@link IDevicePluginDelegate}) and the Gateway
 * ({@link IGatewayController}) TAKX subsystems to facilitate map object to map object {@link IRaptorDataMessage}
 * communication.
 *
 * @since 1.3
 */
public interface ISharedDeviceCapable
{
    /**
     * Attempts to send the {@link IRaptorDataMessage} to another map object's gateway controller. If the target map
     * object doesn't exist and {@code createIfNew} is {@code true}, then a dummy interim
     * {@link IDevicePluginController} will be briefly created from the {@link IDevicePlugin}, an authorization check
     * made, and then the dummy interim controller will be shutdown before the message is forwarded along to the
     * controller.
     * <p>
     * Note:  RDM Derived Fields are not supported when sending an RDM to another device.
     *
     * @param message     IRaptorDataMessage to attempt to send
     * @param createIfNew Flag that indicates if the target of the IRaptorDataMessage doesn't yet exist, create a new
     *                    map object associated with the given device type and device ID defined by the
     *                    IRaptorDataMessage
     * @param passkey     Authentication passkey, if required by the target
     * @throws IllegalArgumentException thrown when createIfNew is false and no such map object exists
     * @throws IllegalAccessException   thrown when the target map object explicitly rejected the message by returning
     *                                  false from their isShareable() access check
     * @since 1.3
     */
    void sendToDevice(IPrePersistRaptorDataMessage message, boolean createIfNew, String passkey)
            throws IllegalArgumentException, IllegalAccessException;

    /**
     * Deauthorizes the named map object from sharing {@link IRaptorDataMessage messages} with the
     * {@link IGatewayController} associated with the concrete instance of this delegate.
     *
     * @param sourceDeviceType Source device type
     * @param sourceDeviceId   Source unit ID
     * @since 1.3
     */
    void revokeShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId);
}
