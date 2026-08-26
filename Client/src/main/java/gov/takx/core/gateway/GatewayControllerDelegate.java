package gov.takx.core.gateway;

import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.ISharedMapObjectManager;
import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.plugin.gateway.IGatewayControllerDelegate;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.core.mapobject.MapObjectMessageRouter;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.cdi.InjectableContainerSupport;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Delegate for sending parsed messages to client side, and messages to be framed to the framer.
 *
 * @see DeviceGateway
 */
public class GatewayControllerDelegate implements IGatewayControllerDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    private final String family;
    private final String deviceType;
    private final String unitID;
    private final String mapObjectKey;
    private final MapObjectMessageRouter messageRouter;
    private final IDeviceGateway deviceGateway;

    private volatile boolean newDeviceMessageSentFlag = false;
    private IRaptorDataPortalController dataPortalController;
    private IVirtualFileSystem fileSystem;

    GatewayControllerDelegate(@Nonnull String family, @Nonnull String deviceType, @Nonnull String unitID)
    {
        this.family = family;
        this.deviceType = deviceType;
        this.unitID = unitID;

        mapObjectKey = KeyUtils.getUniqueKey(family, deviceType, unitID);

        deviceGateway = BEAN_ACCESSOR.getBean(IDeviceGateway.class);
        messageRouter = new MapObjectMessageRouter(family, deviceType, unitID);
    }

    @Override
    public ISmartTrackManager getSmartTrackManager()
    {
        return BEAN_ACCESSOR.getBean(ISmartTrackManager.class);
    }

    @Override
    @UseClientClassLoader
    public void sendToRaptor(IPrePersistRaptorDataMessage message)
    {
        final String messageKey = message.getUniqueKey();

        if (logger.isTraceEnabled()) logger.trace("Sending RDM for {}", messageKey);

        if (!mapObjectKey.equals(messageKey))
        {
            throw new IllegalStateException("IGatewayController must send IPrePersistRaptorDataMessage instances only to its same device family, type and unit ID. (" +
                    mapObjectKey + " != " + messageKey + ")");
        }

        //
        // This step is required here (and not inside of the DeviceGateway when the GatewayController is created) because
        // the gateway controller has the option of swallowing the incoming RDM.  In such situations, no device is actually
        // online because the gateway controller decided not to forward the RDM to the core.  The only way a device should
        // truly be considered online and in the map objects tree is when the controller sends its first RDM through to the
        // core.  Take Unitrac- it uses the sendToDevice method and officially a Unitrac plugin never comes online!
        if (!newDeviceMessageSentFlag)
        {
            // In the case where there are multiple threads pushing data through this delegate, we need to ensure that
            // the device is only initialized once and that the flag is not set until the initialization is complete.
            synchronized (this)
            {
                // Ensure that an earlier thread did not already initialize the device while the current thread was
                // waiting for the lock.
                if (!newDeviceMessageSentFlag)
                {
                    deviceGateway.putDeviceOnline(family, deviceType, unitID);

                    // Set the flag AFTER the device has been initialized.
                    newDeviceMessageSentFlag = true;
                }
            }
        }

        // Find our DPC if we don't already have it.
        if (dataPortalController == null)
        {
            dataPortalController = BEAN_ACCESSOR.getBean(IRaptorDataPortal.class).getController(messageKey);
        }

        if (dataPortalController != null)
        {
            dataPortalController.sendDataMessage(message);
        } else
        {
            logger.warn("DPC = null for {}", messageKey);
        }
    }

    /**
     * Send a RCM to the framer.  Uses the Device Gateway to look up the framer from the list of framers
     * and tells the framer to process.
     *
     * @param message The command message
     * @throws NullPointerException When {@link IPrePersistRaptorCommandMessage#getSourceType()} or
     *                                  {@link IPrePersistRaptorCommandMessage#getSourceName()} are {@code null}.
     */
    @Override
    public void sendToFramer(IPrePersistRaptorCommandMessage message) throws IOException
    {
        final CommServiceType sourceType = message.getSourceType();
        final String sourceName = message.getSourceName();

        Objects.requireNonNull(sourceType, "RCM sourceType must not be null");
        Objects.requireNonNull(sourceName, "RCM sourceName must not be null");

        logger.debug("Requesting framer for {}/{} with type {}/{}", sourceType, sourceName, family, deviceType);

        IGatewayFramer framer = deviceGateway.getFramer(sourceType, sourceName, family, deviceType);
        if (framer != null)
        {
            framer.processMessage(message);
        } else
        {
            logger.error("Controller Delegate failed to find IGatewayFramer for sourceType = {}, sourceName = {}, deviceType = {}", sourceType, sourceName, deviceType);
        }
    }

    @Override
    public IVirtualFileSystem getVirtualFileSystem()
    {
        if (fileSystem == null)
        {
            fileSystem = BEAN_ACCESSOR.getBean(IVirtualFileSystemManager.class, VFSManagerScope.Literal.PROJECT_INSTANCE)
                    .getVirtualFileSystemForDevice(family, deviceType, unitID);
        }
        return fileSystem;
    }

    @Override
    public void sendToDevice(IPrePersistRaptorDataMessage message, boolean createIfNew, String passkey)
            throws IllegalArgumentException, IllegalAccessException
    {
        messageRouter.sendToDevice(message, createIfNew, passkey);
    }

    @Override
    public void revokeShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId)
    {
        ISharedMapObjectManager sharedCommsManager = BEAN_ACCESSOR.getBean(ISharedMapObjectManager.class);
        sharedCommsManager.unregisterShare(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, family, deviceType, unitID);
    }

    @Override
    public void shutdown()
    {
        if (logger.isDebugEnabled()) logger.debug("Controller shutdown for {}", mapObjectKey);
    }
}
