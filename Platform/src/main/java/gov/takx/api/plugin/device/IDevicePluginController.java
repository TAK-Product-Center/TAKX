package gov.takx.api.plugin.device;

import gov.takx.api.messages.IRaptorCommandMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.plugin.gateway.IGatewayParser;

/**
 * Contract for device plugin controller. Created by the {@link IDevicePlugin}, one controller per unit id (Map Object).
 *
 * @see IDevicePlugin
 */
public interface IDevicePluginController extends IPersistableState, IRaptorObject
{
    /**
     * @return device unit id
     */
    String getUnitId();

    /**
     * Indicate if a peer map object of the given device type and unit ID should be permitted to deliver
     * {@link IRaptorDataMessage messages} to this controller via {@link #receiveOtherDeviceMessage(IRaptorDataMessage)}.
     *
     * @param sourceDeviceFamily Source family
     * @param sourceDeviceType   Source device type
     * @param sourceDeviceId     Source unit ID
     * @param passkey            Externally negotiated passkey or secret used to authenticate a client, if required
     * @return {@code true}, to allow them to deliver IRaptorDataMessage messages to this plugin controller, or {@code false}
     * otherwise
     * @since 1.3
     */
    boolean isShareable(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String passkey);

    /**
     * Receive message from another map object instance on the globe.
     *
     * @param rdm IRaptorDataMessage message
     * @since 1.3
     */
    void receiveOtherDeviceMessage(IRaptorDataMessage rdm);

    /**
     * Receive message from the device's device gateway components. In most cases, this will be an
     * {@link IRaptorDataMessage} that has been created by the {@link IGatewayParser} implementation after parsing a
     * device message.
     *
     * @param rdm IRaptorDataMessage message
     * @see IGatewayParser
     */
    void receiveMessage(IRaptorDataMessage rdm);

    /**
     * Device delegate injected for access to TAKX core services, including creation of data monitors and
     * for sending an {@link IRaptorCommandMessage}, in most cases to be framed by the {@link IGatewayFramer}
     * implementation.
     *
     * @param deviceDelegate delegate
     * @see IGatewayFramer
     * @see IDevicePluginDelegate
     */
    void setDevicePluginDelegate(IDevicePluginDelegate deviceDelegate);

    /**
     * Injected {@link IDataStructureFactory} as implemented by the plugin associated with this gateway controller.
     *
     * @param factory Data Structure Factory
     */
    void setDataStructureFactory(IDataStructureFactory factory);

    /**
     * This device is being deleted. If any external resources have been created by this device, they
     * should be cleaned up at this time. TAKX will take care of deleting things like VFS files and ShapeService
     * created shapes, but the plugin may in fact have created things that TAKX does not know about.
     *
     * @since 1.3
     */
    void delete();

    /**
     * Cleanup and release any resources that were being maintained.
     */
    void shutdown();
}
