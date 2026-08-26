package gov.takx.api.plugin.gateway;

import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IRaptorCommandMessage;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;

import java.io.IOException;

/**
 * Interface for a plugin data framer. Implement and return in {@link IDevicePlugin#createGatewayFramer}.
 * Inject the {@link IGatewayFramerDelegate} in the constructor so that the IGatewayFramer can send data out a comms
 * port after framing an {@link IRaptorCommandMessage}.
 *
 * @see IGatewayFramerDelegate
 */
public interface IGatewayFramer
{
    /**
     * Process an RCM for possible framing.
     *
     * @param message RCM to process
     * @throws java.io.IOException exception trying to write framed data
     */
    void processMessage(IPrePersistRaptorCommandMessage message) throws IOException;

    /**
     * Return the IFramerDelegate
     *
     * @return the framer delegate
     */
    IGatewayFramerDelegate getDelegate();

    /**
     * Injects the IDataStructureFactory as defined by the IDevicePlugin.
     *
     * @param factory IDataStructureFactory instance injected from the IDevicePlugin
     * @since 1.3
     */
    void setDataStructureFactory(IDataStructureFactory factory);

    /**
     * Shutdown the framer.  Release any resources that need to be released.
     */
    void shutdown();
}
