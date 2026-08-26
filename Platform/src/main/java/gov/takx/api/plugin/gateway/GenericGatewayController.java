package gov.takx.api.plugin.gateway;

import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.plugin.ARaptorObject;
import gov.takx.api.plugin.device.ADevicePlugin;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.platform.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Generic Gateway Controller.  Most plugin implementations will not need to create a unique
 * gateway controller class, they can just use this class, or better yet, if they extend
 * {@link ADevicePlugin}, they can just let it create an instance
 * automatically.
 *
 */
public class GenericGatewayController extends ARaptorObject implements IGatewayController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected String unitID;
    protected IGatewayControllerDelegate delegate;
    protected IDataStructureFactory factory;

    public GenericGatewayController(String family, String type, String unitID, IGatewayControllerDelegate delegate)
    {
        this.family = family;
        this.type = type;
        this.unitID = unitID;
        this.delegate = delegate;

        // Set the unique key based on family, type, and unit ID associated with this gateway controller.
        key = KeyUtils.getUniqueKey(this.family, this.type, this.unitID);

        logger.debug("Created Gateway Controller of Unit ID {}", unitID);
    }

    @Override
    public void processParserMessage(IPrePersistRaptorDataMessage message)
    {
        delegate.sendToRaptor(message);
    }

    @Override
    public void processConnectionMessage(IPrePersistRaptorDataMessage message)
    {
        delegate.sendToRaptor(message);
    }

    @Override
    public void processDeviceControllerMessage(IPrePersistRaptorCommandMessage message) throws IOException
    {
        delegate.sendToFramer(message);
    }

    @Override
    public void processDeviceMessage(IPrePersistRaptorDataMessage message)
    {
        delegate.sendToRaptor(message);
    }

    /**
     * Stores the injected IDataStructureFactory in a protected variable named {@code factory}.
     *
     * @param factory Injected IDataStructureFactory
     */
    @Override
    public void setDataStructureFactory(IDataStructureFactory factory)
    {
        this.factory = factory;
    }

    /**
     * Purges the delegate by setting it to {@code null}.
     */
    @Override
    public void shutdown()
    {
        delegate = null;
        factory = null;
    }
}
