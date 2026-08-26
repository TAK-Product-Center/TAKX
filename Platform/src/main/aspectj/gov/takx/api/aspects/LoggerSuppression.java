package gov.takx.api.aspects;

import gov.takx.api.annotation.SuppressLogging;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * This aspect provides "around" advice for any method that is annotated with {@link SuppressLogging}.  It is
 * intended strictly for use within our test classes, and any test classes within consuming projects.
 * <p>
 * It should never be used within production code.
 *
 * @since 2.35
 */
@Aspect
public class LoggerSuppression
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Around("execution(* *(..)) && @annotation(gov.takx.api.annotation.SuppressLogging)")
    public Object aroundWithLoggingSuppression(ProceedingJoinPoint call) throws Throwable
    {
        final Method method = ((MethodSignature) call.getSignature()).getMethod();
        final SuppressLogging annotation = method.getAnnotation(SuppressLogging.class);
        final Class<?>[] suppressedClasses = annotation.value();

        final Map<LoggerConfig, Level> originalLoggerLevels = new HashMap<>();

        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration configuration = context.getConfiguration();

        // Suppress each class and record the current logging level
        for (Class<?> cls : suppressedClasses)
        {
            final Logger slf4jLogger = LoggerFactory.getLogger(cls);

            LoggerConfig loggerConfig = configuration.getLoggerConfig(slf4jLogger.getName());
            final Level level = loggerConfig.getLevel();

            // By default a logger configuration isn't made for each class so resetting it will reset root.
            // so create a configuration for each class with class name which will then be suppressed.
            final String loggerName = cls.getName();
            if (!loggerConfig.getName().equals(loggerName))
            {
                loggerConfig = new LoggerConfig(loggerName, Level.OFF, true);
                configuration.addLogger(loggerName, loggerConfig);
            }

            // If the logger is currently less specific than WARN, then leave it alone
            if (level != null && level.intLevel() > Level.WARN.intLevel()) continue;

            logger.debug("Suppress logging on: {}", cls);

            // In case they list the same class twice, only record the initial level
            originalLoggerLevels.putIfAbsent(loggerConfig, level);

            loggerConfig.setLevel(Level.OFF);
        }
        context.updateLoggers();

        // Now proceed into the method within a try/finally, so we can re-instate the previous logging level
        try
        {
            return call.proceed();
        } finally
        {
            originalLoggerLevels.forEach(LoggerConfig::setLevel);
            context.updateLoggers();
        }
    }
}
