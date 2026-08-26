package gov.takx.platform.logging;

import gov.takx.api.platform.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Progress monitor that simply ignores all method invocations except for calls to{@link #setText(String)}, which are
 * logged at debug level.
 */
public class NullProgressMonitor implements IProgressMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void setText(String text)
    {
        logger.debug(text);
    }

    @Override
    public void setSubstatusMaximum(int maximum)
    {
    }

    @Override
    public void incrementSubstatus()
    {
    }

    @Override
    public void setMaximum(int maximum)
    {
    }

    @Override
    public void increment()
    {
    }

    @Override
    public void setSubstatus(String text)
    {
    }

    @Override
    public void setVisible(boolean b)
    {
    }

    @Override
    public void dispose()
    {
    }
}
