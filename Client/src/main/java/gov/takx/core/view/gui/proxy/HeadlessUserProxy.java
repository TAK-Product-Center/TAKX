package gov.takx.core.view.gui.proxy;

import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.ui.IUserProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * User proxy when we are running headless.  In general, answer yes to everything.
 *
 * @since 2.0.0
 */
@ApplicationScoped
@Typed(HeadlessUserProxy.class)
public class HeadlessUserProxy implements IUserProxy
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @PlatformUnderTest
    private boolean platformUnderTest;

    @Override
    public boolean confirmQuit(boolean quitting)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return Always True
     */
    @Override
    public boolean confirmDatabaseUpgrade(String msg)
    {
        return true;
    }

    @Override
    public void notify(String title, String message)
    {
        logger.info("User asked to confirm: {}", message);
    }

    @Override
    public void reportPreferencesError(Throwable cause)
    {
        logger.error("Failed to update preferences", cause);
    }

    @Override
    public void reportError(String title, String msg, Throwable cause)
    {
        if (!platformUnderTest)
        {
            logger.error(msg, cause);
        } else
        {
            // In case you really want to see the messages during test
            if (logger.isDebugEnabled()) logger.debug(msg, cause);
        }
    }

    @Override
    public void reportWarning(String title, String msg)
    {
        logger.warn(msg);
    }

    @Override
    public void reportMessage(String title, String msg)
    {
        logger.info(msg);
    }
}
