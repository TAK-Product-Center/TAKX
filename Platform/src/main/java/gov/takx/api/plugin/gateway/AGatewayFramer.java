package gov.takx.api.plugin.gateway;

import gov.takx.api.plugin.device.IDataStructureFactory;

/**
 * Abstract Gateway Framer.  Most gateway framer implementations will simply
 * extend this base class and provide the delegate via a super() constructor call.
 *
 */
public abstract class AGatewayFramer implements IGatewayFramer
{
    protected IGatewayFramerDelegate delegate;
    protected IDataStructureFactory factory;

    protected AGatewayFramer(IGatewayFramerDelegate delegate)
    {
        this.delegate = delegate;
    }

    public IGatewayFramerDelegate getDelegate()
    {
        return delegate;
    }

    public void setDataStructureFactory(IDataStructureFactory factory)
    {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Null out internal references.
     */
    @Override
    public void shutdown()
    {
        delegate = null;
        factory = null;
    }
}
