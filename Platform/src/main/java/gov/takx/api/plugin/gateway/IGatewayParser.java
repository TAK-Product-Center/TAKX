package gov.takx.api.plugin.gateway;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;

/**
 * Gateway parser. Implement and return implementation in {@link IDevicePlugin#createGatewayParser}.
 * Inject the {@link IGatewayParserDelegate} into the constructor of the implementation so that the parser can
 * send {@link IRaptorDataMessage} objects to the client side after parsing data messages.
 *
 */
@SuppressWarnings("deprecation")
public interface IGatewayParser extends IParser
{
    /**
     * Injects the IDataStructureFactory as defined by the IDevicePlugin.
     *
     * @param factory IDataStructureFactory instance injected from the IDevicePlugin
     * @since 1.3
     */
    void setDataStructureFactory(IDataStructureFactory factory);

    /**
     * @return The delegate for this parser
     * @since 2.0.0
     */
    IGatewayParserDelegate getDelegate();
}
