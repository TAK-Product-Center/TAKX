package gov.takx.api.plugin.gateway;

import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.platform.KeyUtils;

import java.io.IOException;

/**
 * Gateway controller is used to "pre-process" data messages (RDMs) that are coming from the
 * parser before passing them on to the client, or command messages (RCMs) that are coming from the
 * client before passing them on to the framer.  Inject the IController delegate in the constructor
 * for sending RDMs and RCMs after processing.  IGatewayController is created in the IDevicePlugin
 * method createGatewayController.
 * <p/>
 * If the plugin wants the message to be processed normally, then it should forward it on to the TAKX core by invoking
 * {@link IGatewayControllerDelegate#sendToRaptor}
 * on the message.  This will allow it to be persisted and processed by the takx core in the normal way.
 * <p/>
 * If the plugin wants the message to be discarded (not persisted and not processed in any way), then it should
 * <em>NOT</em> call {@code sendToRaptor}.
 * <p/>
 * The response from {@link IRaptorObject#getUniqueKey()} <b>must</b> be calculated using
 * {@link KeyUtils#getUniqueKey(String, String, String)}, based upon the family, type, and unit ID.
 * Failure to follow this requirement will result in unstable gateway controller behavior for the associated device.
 *
 * @see IGatewayControllerDelegate
 * @see IDevicePlugin
 */
public interface IGatewayController extends IRaptorObject
{
    /**
     * Process an RDM corresponding to hardware device data processed by the Gateway Parser.  Normal operation is to send the RDM
     * out the controller delegate.  A chance to process the RDM before passing it to the
     * controller delegate.  Hardware that requires an ACK message can be sent one from here
     * without ever having to pass the RDM to the client.
     *
     * @param message the RDM
     */
    void processParserMessage(IPrePersistRaptorDataMessage message);

    /**
     * Process an RDM delivered from a connection.
     *
     * @param message the RDM
     */
    void processConnectionMessage(IPrePersistRaptorDataMessage message);

    /**
     * Process an RCM coming from the device controller.  Normal operation is to process the RCM with the
     * framer delegate.  A chance to process the RCM before sending to the framer delegate.
     *
     * @param message IRaptorCommandMessage
     * @throws java.io.IOException processing RDM
     */
    void processDeviceControllerMessage(IPrePersistRaptorCommandMessage message) throws IOException;

    /**
     * Process an RDM from another map object device instance located on the globe.
     *
     * @param rdm IPrePersistRaptorDataMessage received from a peer
     * @since 1.3
     */
    void processDeviceMessage(IPrePersistRaptorDataMessage rdm);

    /**
     * Injected IDataStructureFactory as implemented by the plugin associated with this gateway controller.
     *
     * @param factory Data Structure Factory
     */
    void setDataStructureFactory(IDataStructureFactory factory);

    /**
     * Called before the controller is disposed.  A good time to release any held resources.
     */
    void shutdown();
}
