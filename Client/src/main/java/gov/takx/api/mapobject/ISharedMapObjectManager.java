package gov.takx.api.mapobject;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.api.plugin.gateway.IGatewayController;

/**
 * Internal shared map object communication security manager.  This service manager provides both anonymity and
 * security between map object instances that wish to directly communicate with each other via {@link IRaptorDataMessage}
 * instances dispatched either from the UI ({@link IDevicePluginDelegate}) or from the gateway ({@link IGatewayController}).
 *
 * @since 1.3
 */
public interface ISharedMapObjectManager
{
    /**
     * Registers a one-way shared relationship between the source and the target.  Once the share has been established,
     * the source has permission to share IRaptorDataMessage instances with the target.  Messages can be sent either
     * through the UI ({@link IDevicePluginDelegate}) or from within the gateway ({@link IGatewayController}).
     * <p/>
     * If the relationship already exists, no action is taken.
     *
     * @param sourceDeviceFamily Family of source device
     * @param sourceDeviceType   Device type of source device
     * @param sourceDeviceId     Unit id of source device
     * @param targetDeviceFamily Family of target fevice
     * @param targetDeviceType   Device type of target
     * @param targetDeviceId     Unit ID of target device
     */
    void registerShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String targetDeviceFamily, String targetDeviceType, String targetDeviceId);

    /**
     * Unregisters the one-way shared relationship that was previously established between the source and the target.
     * If no relationship had previously existed, no action is taken.
     *
     * @param sourceDeviceFamily Family of source device
     * @param sourceDeviceType   Device type of source device
     * @param sourceDeviceId     Unit id of source device
     * @param targetDeviceFamily Family of target fevice
     * @param targetDeviceType   Device type of target
     * @param targetDeviceId     Unit ID of target device
     */
    void unregisterShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String targetDeviceFamily, String targetDeviceType, String targetDeviceId);

    /**
     * Determines if a one-way share exists between the source and the target.
     *
     * @param sourceDeviceFamily Family of source device
     * @param sourceDeviceType   Device type of source device
     * @param sourceDeviceId     Unit id of source device
     * @param targetDeviceFamily Family of target fevice
     * @param targetDeviceType   Device type of target
     * @param targetDeviceId     Unit ID of target device
     * @return {@code true} if a share exists
     */
    boolean isShared(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String targetDeviceFamily, String targetDeviceType, String targetDeviceId);
}
