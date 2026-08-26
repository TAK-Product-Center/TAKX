package gov.takx.api.plugin.gateway;

import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.commons.constants.CommServiceType;

/**
 * Abstract Gateway Parser.  Most gateway parser implementations will simply
 * extend this base class and provide the delegate via a super() constructor call.
 *
 */
public abstract class AGatewayParser implements IGatewayParser
{
    protected IGatewayParserDelegate delegate;
    protected CommServiceType sourceType;
    protected String sourceName;
    protected IDataStructureFactory factory;

    protected AGatewayParser(CommServiceType sourceType, String sourceName, IGatewayParserDelegate delegate)
    {
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.delegate = delegate;
    }

    @Override
    public IGatewayParserDelegate getDelegate()
    {
        return delegate;
    }

    @Override
    public void shutdown()
    {
        factory = null; // Set references to null so they can be garbage collected
        delegate = null;
    }

    @Override
    public void setDataStructureFactory(IDataStructureFactory factory)
    {
        this.factory = factory;
    }
}
