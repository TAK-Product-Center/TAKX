package gov.takx.logging;

import gov.takx.api.event.ShuttingDown;
import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.platform.logging.LoggerUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Logging appender that takes messages and adds them to the system message log.  Since we have so many messages
 * that are spit out at INFO level, we define a new level {@link SystemLogLevel#SYSTEM_INFO} to be used for
 * generating a system message event at the INFO level.
 * <p/>
 * To log an INFO message that will be added to the System Message Log, you would use something like this:
 * <p/>
 * {@code logger.log(SystemLogLevel.SYSTEM_INFO, "This is my message");}
 * <p/>
 * or
 * <p/>
 * {@code logger.log(SystemLogLevel.SYSTEM_INFO, "This is my message", someException);}
 * <p/>
 * To log warnings or errors, you can use the standard {@link Logger#warn(String)} or {@link Logger#error(String)}
 * methods.
 *
 * @see SystemLogLevel
 * @since 1.42
 */
@ApplicationScoped
public class SystemMessageAppender
{
    // WARNING: Never log anything greater than INFO level, or you'll get infinite recursion
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String APPENDER_NAME = SystemMessageAppender.class.getSimpleName();

    @Inject
    private ISystemMessageManager messageManager;

    private final InternalAppender appender = new InternalAppender();

    /**
     * Start operation, add our appender.
     *
     * @since 2.31
     */
    public void start()
    {
        LoggerUtils.addAppender(appender, Level.WARN);
    }

    /**
     * Stop operation when we are shutting down.
     *
     * @since 2.31
     */
    private void shutdown(@Observes ShuttingDown event)
    {
        LoggerUtils.removeAppender(appender);
    }

    /**
     * Actual appender.
     *
     * @since 2.31
     */
    private final class InternalAppender extends AbstractAppender
    {
        static final CompositeFilter compositeFilter;
        static
        {
            Filter[] filters = new Filter[1];
            try
            {
                filters[0] = RegexFilter.createFilter(".*TAKEngineJNI.*", null, false, Filter.Result.DENY, Filter.Result.NEUTRAL);
            } catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
            compositeFilter = CompositeFilter.createFilters(filters);
        }

        InternalAppender()
        {
            super(APPENDER_NAME, compositeFilter, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event)
        {
            final Level level = event.getLevel();
            final String message = event.getMessage().getFormattedMessage();

            // Construct a SystemMessage from this logging event and add it
            messageManager.addMessage(new SystemMessage(SystemLogLevel.fromLog4jLevel(level), message, event.getThrown()));

            if (logger.isDebugEnabled())
            {
                logger.debug("SYSLOG: [{}] {} {}", event.getThreadName(), level, message);
            }
        }
    }
}
