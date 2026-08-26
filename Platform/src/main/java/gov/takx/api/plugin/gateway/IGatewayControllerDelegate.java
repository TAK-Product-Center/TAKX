package gov.takx.api.plugin.gateway;

import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorCommandMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.device.ISharedDeviceCapable;
import gov.takx.api.tracks.ISmartTrackManager;

import java.io.IOException;

/**
 * Delegate that should be injected into the {@link IGatewayController}. Used to send {@link IRaptorDataMessage}
 * to the client side of TAKX, or an {@link IRaptorCommandMessage} to the {@link IGatewayFramer}.
 *
 * @see IGatewayController
 */
public interface IGatewayControllerDelegate extends ISharedDeviceCapable
{
    /**
     * Send an {@link IRaptorDataMessage} to the client
     *
     * @param message the RDM
     * @throws IllegalStateException if the device type and device ID do not match the delegate's device type and device
     *                               ID.  Simply stated, an Acme Device controller cannot use this method to send an
     *                               IRaptorDataMessage that targets any map object other than its own.  To communicate
     *                               with other map objects on the globe, see sendToMapObject().
     */
    void sendToRaptor(IPrePersistRaptorDataMessage message);

    /**
     * Send an {@link IRaptorCommandMessage} to the framer
     *
     * @param message the RCM
     * @throws java.io.IOException      exception sending RDM to TAKX
     * @throws IllegalArgumentException When {@link IPrePersistRaptorCommandMessage#getSourceType()} or
     *                                  {@link IPrePersistRaptorCommandMessage#getSourceName()} are {@code null}.
     */
    void sendToFramer(IPrePersistRaptorCommandMessage message) throws IOException;

    /**
     * Get a reference to the virtual file system for this map object.
     *
     * @return a file system object.
     * @since 1.3
     */
    IVirtualFileSystem getVirtualFileSystem();

    /**
     * Get a reference to the SmartTracks manager for use in registering new and locating existing track style
     * definitions. An {@link IPrePersistRaptorDataMessage} may be assigned an explicit
     * track style definition.
     *
     * @return SmartTrack Manager
     */
    ISmartTrackManager getSmartTrackManager();

    /**
     * Called before this object is to be disposed.  A good time to release resources
     */
    void shutdown();
}
