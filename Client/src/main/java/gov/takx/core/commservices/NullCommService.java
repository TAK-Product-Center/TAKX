package gov.takx.core.commservices;

import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.IWritableCommPath;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;

/**
 * Provides an 'empty' comm service that is useful for establishing a connection to a 3rd party API instead of an
 * Ethernet Socket or a Serial port.
 *
 * @since 1.4
 */
public class NullCommService extends ACommService
{
    protected NullCommService(String commPathName, ICommListener commListener)
    {
        super(CommServiceType.NONE);
        this.name = commPathName;

        if (commListener != null)
        {
            addCommPathListener(commListener);
        }
    }

    @Override
    public void captureCommPath()
    {
        throw new UnsupportedOperationException("NullCommService of CommServiceType.NONE does not support this method");
    }

    @Override
    public void releaseCommPath()
    {
        throw new UnsupportedOperationException("NullCommService of CommServiceType.NONE does not support this method");
    }

    @Override
    public IWritableCommPath getWritableCommPath()
    {
        throw new UnsupportedOperationException("NullCommService of CommServiceType.NONE does not support this method");
    }

    @Override
    protected boolean openResources()
    {
        return true;
    }

    @Override
    protected void closeResources(boolean wasExpected)
    {
    }
}
