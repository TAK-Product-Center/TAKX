package gov.takx.platform.logging;

import gov.takx.api.annotation.Internal;
import jakarta.annotation.Nonnull;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Logging utilities.
 *
 * @since 3.2 (completely changed)
 */
@Internal
public class LoggerUtils
{
    /**
     * System property specifying the log4j2 configuration file.
     */
    public static final String LOG4J_CONFIGURATION = "log4j2.configurationFile";

    /**
     * Default name of log4j2 configuration, properties format.
     */
    public static final String LOG4J_PROPERTIES = "log4j2.properties";

    /**
     * Default name of log4j2 configuration, XML format.
     */
    public static final String LOG4J_XML = "log4j2.xml";

    /**
     * Layout to use if we can not locate the "Console" appender.
     *
     * @see PatternLayout#SIMPLE_CONVERSION_PATTERN
     */
    public final static PatternLayout BACKUP_LAYOUT = PatternLayout.newBuilder()
            .withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
            .build();

    /**
     * Get the layout in use by the {@code Console} appender.  This allows configuration at runtime to match whatever
     * layout pattern is specified in our XML configuration files (see {@code log4j-properties.xml}).
     *
     * @return The layout in use by the {@code Console} appender
     */
    public static Layout<? extends Serializable> getConsoleLayout()
    {
        final Appender console = LoggerContext.getContext(false).getConfiguration().getAppender("ConsoleDebug");

        return console != null ? console.getLayout() : BACKUP_LAYOUT;
    }

    /**
     * Add an appender to the logging configuration.  The appender is started before it is added.
     *
     * @param appender Appender to add
     * @param level Level at which to add the appender
     * @see LifeCycle#start()
     */
    public static void addAppender(@Nonnull Appender appender, @Nonnull Level level)
    {
        appender.start();
        updateRootConfiguration(rootConfig -> rootConfig.addAppender(appender, level, null));
    }

    /**
     * Remove the given appender from the logging configuration.  The appender is stopped before it is removed.
     *
     * @param appender Appender to remove
     * @see LifeCycle#stop()
     */
    public static void removeAppender(@Nonnull Appender appender)
    {
        appender.stop();

        updateRootConfiguration(rootConfig -> rootConfig.removeAppender(appender.getName()));
    }

    /**
     * Apply a handler to the root logger configuration.  After the handler is invoked, the loggers are all updated.
     *
     * @param handler Handler to apply
     * @see LoggerContext#updateLoggers()
     */
    private static void updateRootConfiguration(Consumer<LoggerConfig> handler)
    {
        final LoggerContext context = LoggerContext.getContext(false);
        final LoggerConfig rootConfig = context.getRootLogger().get();

        handler.accept(rootConfig);

        context.updateLoggers();
    }
}
