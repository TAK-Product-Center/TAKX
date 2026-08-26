package gov.takx.api.plugin.device;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.IAnnotatedPlugin;
import gov.takx.api.plugin.IPluginDescriptor;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayControllerDelegate;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.plugin.gateway.IGatewayFramerDelegate;
import gov.takx.api.plugin.gateway.IGatewayParser;
import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.commons.constants.CommServiceType;

/**
 * Contract for a Device plugin.  This interface provides default implementations of most of the simple
 * getters for values that can be declared on the {@link DevicePlugin} annotation.  This relieves the plugin
 * developer of the need to declare and implement the methods in {@link IPluginDescriptor} for example.  Instead,
 * they just declare the appropriate values in the annotation and these default implementations do the rest.
 *
 * @since 2.0.0
 */
public interface IDevicePlugin extends IAnnotatedPlugin<DevicePlugin>
{
    /**
     * Return the {@link IDataStructureFactory} that manages all of the {@link IRaptorDataStructure} instances used
     * by this plugin.
     *
     * @return Instance of an IDataStructureFactory
     * @since 1.3
     */
    IDataStructureFactory getDataStructureFactory();

    /**
     * Return a controller for map object creation and management. One controller should be created per map object id.
     *
     * @param unitID id of map object
     * @return controller IClientPluginController implementation, or {@code null} if none needed.
     */
    IDevicePluginController getDeviceController(String unitID);

    /**
     * Create and return a new parser, or return {@code null} if plugin does not support parsers based on the port name,
     * type and format.
     *
     * @param sourceType String type of port i.e. ETHERNET, SERIAL (see: CommServiceTypes)
     * @param sourceName String name of port
     * @param format     String description of data format expected on port. May be a {@code null} value.
     * @param delegate   delegate for the parser to send parsed messages up the line
     * @return the new IGatewayParser implementation
     * @see CommServiceType
     */
    IGatewayParser createGatewayParser(CommServiceType sourceType, String sourceName, String format, IGatewayParserDelegate delegate);

    /**
     * Create and return a new framer, or return {@code null} if plugin does not support parsers based on the port name,
     * type and format.
     *
     * @param sourceType String type of port i.e. ETHERNET, SERIAL (see: CommServiceTypes)
     * @param sourceName String name of port
     * @param format     String description of data format expected on port
     * @param delegate   delegate for the framer to send framed messages out the comm port
     * @return the new IGatewayFramer implementation
     * @see IGatewayFramerDelegate
     * @see CommServiceType
     */
    IGatewayFramer createGatewayFramer(CommServiceType sourceType, String sourceName, String format, IGatewayFramerDelegate delegate);

    /**
     * Create and return a new controller to manage messages from the IGatewayParser to the client and from the
     * client to the IGatewayFramer.
     *
     * @param unitID   String id for map object / device
     * @param delegate delegate for sending RDMs to the data portal, and RCMs to the framer
     * @return the new IGatewayController implementation
     * @see IGatewayControllerDelegate
     */
    IGatewayController createGatewayController(String unitID, IGatewayControllerDelegate delegate);

    /**
     * Determines if instances of this plugin are receptive to receiving {@link IRaptorDataMessage} instances from
     * disparate map object instances (where {@code !getType().equals(someMapObjectPlugin.getType()}). For example,
     * is the Acme device plugin receptive to receiving {@link IRaptorDataMessage} instances from a Foo device.
     * <p>
     * For identical map object implementations where {@code getType().equals(someMapObjectPlugin.getType()} this
     * method is never checked; it is intended to solely indicate if the map object plugin is receptive to messages from
     * disparate plugins.
     * <p>
     * If shareable, the messages are always delivered to the instance's
     * {@link IGatewayController#processDeviceMessage(IPrePersistRaptorDataMessage)}
     * method. Additionally, each Acme device map object instance is given the additional opportunity to authorize or
     * reject {@link IRaptorDataMessage} instances from other map objects-- see
     * {@link IDevicePluginController#isShareable(String, String, String, String)}
     *
     * @return {@code true} if this plugin is receptive to receiving {@link IRaptorDataMessage} instances from disparate
     * map object instances.
     * @since 1.3
     */
    boolean isShareable();

    /**
     * Cleanup on map object shutdown. Concrete implementation is responsible for notifying its
     * {@link IDevicePluginController} via {@link IDevicePluginController#shutdown()}, if applicable.
     *
     * @param unitID String id for map object / device
     */
    void shutdownDevice(String unitID);

    /**
     * This device is being deleted from TAKX. If any external resources have been created by this device, they
     * should be cleaned up at this time. TAKX will take care of deleting things like VFS files and ShapeService
     * created shapes, but the plugin may in fact have created things that TAKX does not know about.
     * <p>
     * Note: TAKX will notify the {@link IDevicePluginController} directly if one exists. This notification will happen
     * immediately after this call.
     *
     * @param unitID String id for map object / device
     * @since 1.3
     */
    void deleteDevice(String unitID);

    /**
     * Cleanup up and cached resources used by the plugin.  The invocation of this method means that either the
     * application is shutting down, or that the user has selected "File...New" from the menu and a new session
     * is being created.  This method is called after every known map object has been shutdown via calls to
     * shutdown(unitID), so it should not be necessary for this method implementation to shut down any units, as they
     * should have already been shutdown.
     *
     * @since 1.3
     */
    void shutdownPlugin();
}
