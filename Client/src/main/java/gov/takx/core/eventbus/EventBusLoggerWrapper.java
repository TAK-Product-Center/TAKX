package gov.takx.core.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;

/**
 * A simple logger wrapper that implements greenrobot's logger interface and maps its Java log {@link Level}s to TAKX's
 * logger's methods.
 *
 * @since 3.0
 */
public class EventBusLoggerWrapper implements org.greenrobot.eventbus.Logger
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void log(Level level, String message)
    {
        log(level, message, null);
    }

    @Override
    public void log(Level level, String message, Throwable throwable)
    {
        final int levelIntValue = level.intValue();

        if (levelIntValue >= Level.SEVERE.intValue())
        {
            logger.error(message, throwable);
        } else if (levelIntValue >= Level.WARNING.intValue())
        {
            logger.warn(message, throwable);
        } else if (levelIntValue >= Level.INFO.intValue())
        {
            if (logger.isInfoEnabled()) logger.info(message, throwable);
        } else if (levelIntValue >= Level.FINE.intValue())
        {
            if (logger.isDebugEnabled()) logger.debug(message, throwable);
        } else
        {
            if (logger.isTraceEnabled()) logger.trace(message, throwable);
        }
    }
}
