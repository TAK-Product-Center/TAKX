package gov.takx.core.mapobject;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.ISharedMapObjectManager;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.registry.IDevicePluginRegistry;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Class for routing map objects across a plugin boundary.  The router is effectively used by the MapObjectController
 * and GatewayControllerDelegate (in the gateway) as the two possible places from which it is possible to cross this
 * boundary.  This class becomes the single authority for the routing logic.
 *
 * @since 1.14.24
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectMessageRouter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private ISharedMapObjectManager sharedMOM;

    @Inject
    private IRaptorDataPortal dataPortal;

    @Inject
    private IDeviceGateway deviceGateway;

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    private String sourceFamily;
    private String sourceType;
    private String sourceId;

    /**
     * Constructor
     *
     * @param family source family
     * @param type   source device type
     * @param id     source device id
     */
    public MapObjectMessageRouter(String family, String type, String id)
    {
        this.sourceFamily = family;
        this.sourceType = type;
        this.sourceId = id;
    }

    /**
     * Send a {@link IRaptorDataMessage} across a boundary from one plugin to another. The router is represented as the
     * source, with the target plugin represented in the message itself.
     * <p>
     * Note: RDM Derived Fields are not supported when sending an RDM to another device.
     *
     * @param message     the RDM to send to another plugin
     * @param createIfNew create a new instance of a device, if it doesn't exist already.
     * @param passkey     optional passkey that can be matched in {@link IDevicePluginController#isShareable(String, String, String, String)}
     * @throws IllegalArgumentException exception thrown if plugin referenced in the RDM does not exist
     * @throws IllegalAccessException   exception thrown if plugin or controller denies reception of the RDM
     */
    public void sendToDevice(IPrePersistRaptorDataMessage message, boolean createIfNew, String passkey) throws IllegalArgumentException, IllegalAccessException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Attempting to lookup associated plugin controller for '{}' of type '{}'", message.getFamily(), message.getType());
        }
        //
        // Lookup the plugin so the associated plugin controller can determine if it authorizes receipt of messages
        IDevicePlugin plugin = devicePluginRegistry.getPlugin(message.getFamily(), message.getType());

        if (null == plugin)
        {
            throw new IllegalArgumentException("No plugin exists for family: " + message.getFamily() + " deviceType: " + message.getType());
        }

        // CORE-5137: Removed the call to calculate the derived fields when sending an RDM to another device.  See CORE-5137 for more details.

        String targetFamily = message.getFamily();
        String targetDeviceType = message.getType();
        String targetDeviceId = message.getUnitID();
        String targetDeviceVersion = plugin.getVersion();

        message.setSourceFamilyType(sourceFamily);
        message.setSourceDeviceType(sourceType);
        message.setSourceDeviceId(sourceId);

        boolean isShared = sharedMOM.isShared(sourceFamily, sourceType, sourceId, targetFamily, targetDeviceType, targetDeviceId);

        if (logger.isDebugEnabled())
        {
            logger.debug("Result of SharedCommsManager query: isShared() == {}", isShared);
        }

        if (isShared)
        {
            //
            // Forward along the message to the target's gateway controller
            //
            if (logger.isDebugEnabled()) logger.debug("Forwarding along the map object message");

            forwardMapObjectMessage(targetFamily, targetDeviceType, targetDeviceId, targetDeviceVersion, message);
        } else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to determine if plugin/device authorizes a share");
            }

            if (!targetDeviceType.equals(message.getType()))
            {
                boolean isPluginShareable = plugin.isShareable();
                if (logger.isDebugEnabled()) logger.debug("Is Plugin Shareable? {}", isPluginShareable);

                //
                // Plugin level share check
                if (!isPluginShareable)
                {
                    throw new IllegalAccessException("Plugin " + plugin.getType() + " does not support shared messaging");
                }
            }

            //
            // First, check to see if  the map object already exists on the globe
            final IRaptorDataPortalController dpc = dataPortal.getController(message.getUniqueKey());
            boolean mapObjectExists = (null != dpc);

            if (logger.isDebugEnabled()) logger.debug("Does Target Map Object Already Exist? {}", mapObjectExists);

            // Target doesn't exist, but has not authorized the one-way share yet
            if (mapObjectExists)
            {
                // Don't need to worry about the createIfNew flag at this point - target exists

                boolean isAuthorized = false;

                try
                {
                    IDevicePluginController controller = plugin.getDeviceController(message.getUnitID());
                    isAuthorized = controller.isShareable(sourceFamily, sourceType, sourceId, passkey);

                    if (logger.isDebugEnabled()) logger.debug("Is Authorized? {}", isAuthorized);
                } catch (Exception e)
                {
                    logger.warn("Unexpected exception while invoking isShareable({}, {})", sourceType, sourceId, e);
                }

                if (isAuthorized)
                {
                    // Register the one-way share
                    sharedMOM.registerShare(sourceFamily, sourceType, sourceId, targetFamily, targetDeviceType, targetDeviceId);

                    // Forward along the message to the target's gateway controller
                    if (logger.isDebugEnabled()) logger.debug("Forwarding along the map object message");

                    forwardMapObjectMessage(targetFamily, targetDeviceType, targetDeviceId, targetDeviceVersion, message);
                } else
                {
                    throw new IllegalAccessException("No authorization to send messages from " + sourceType + ":" + sourceId + " to " + targetDeviceType + ":" + targetDeviceId);
                }
            } else
            {
                if (!createIfNew)
                {
                    // Don't create if new use case
                    throw new IllegalArgumentException("No map object exists for " + targetDeviceType + ":" + targetDeviceId);
                } else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Creating the map object controller in order to determine authorization (createIfNew == true)");
                    }

                    boolean isAuthorized = false;
                    try
                    {
                        IDevicePluginController controller = plugin.getDeviceController(message.getUnitID());

                        // Inject in the IDataMonitorFactory as a convenience for the plugin developer
                        controller.setDataStructureFactory(plugin.getDataStructureFactory());

                        isAuthorized = controller.isShareable(sourceFamily, sourceType, sourceId, passkey);

                        if (logger.isDebugEnabled()) logger.debug("Is Authorized? {}", isAuthorized);
                    } catch (Exception e)
                    {
                        logger.warn("Unexpected exception while invoking isShareable({}, {})", sourceType, sourceId, e);
                    }

                    if (isAuthorized)
                    {
                        // Register the one-way share
                        sharedMOM.registerShare(sourceFamily, sourceType, sourceId, targetFamily, targetDeviceType, targetDeviceId);

                        // Forward along the message to the target's gateway controller
                        if (logger.isDebugEnabled()) logger.debug("Forwarding along the map object message");

                        forwardMapObjectMessage(targetFamily, targetDeviceType, targetDeviceId, targetDeviceVersion, message);
                    } else
                    {
                        throw new IllegalAccessException("No authorization to send messages from " + sourceType + ":" + sourceId + " to " + targetDeviceType + ":" + targetDeviceId);
                    }
                }
            }
        }
    }

    /**
     * Actual forwarding of the {@link IRaptorDataMessage}. Create/Lookup a GatewayController for the target, and pass the
     * RDM to it.
     *
     * @param targetFamily        target plugin family
     * @param targetDeviceType    target plugin device type
     * @param targetDeviceId      target plugin device id
     * @param targetDeviceVersion target plugin version
     * @param message             the RaptorDataMessage to forward.
     */
    private void forwardMapObjectMessage(String targetFamily, String targetDeviceType, String targetDeviceId, String targetDeviceVersion, IPrePersistRaptorDataMessage message)
    {
        IGatewayController gatewayController = deviceGateway.getController(targetFamily, targetDeviceType, targetDeviceId);
        if (null != gatewayController)
        {
            try
            {
                gatewayController.processDeviceMessage(message);
            } catch (Exception e)
            {
                logger.error("Unexpected exception during gatewayController.processMapObjectMessage() invocation", e);
            }
        } else
        {
            // Very strange edge case - map object exists, yet there is no known gateway controller??
            logger.error("Map Object message routing failed: no gateway controller found for {}:{}:{}:{}", targetFamily, targetDeviceType, targetDeviceId, targetDeviceVersion);
        }
    }
}
