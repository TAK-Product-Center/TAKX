package gov.takx.api.logging;

import org.apache.logging.log4j.Level;

import java.util.Map;

/**
 * Logging Level constants for use with the system messages log.
 * Also, helps to keep the direct use of Log4j classes somewhat isolated.
 *
 * @since 3.2 (completely reworked)
 */
public enum SystemLogLevel
{
    SYSTEM_ALL,
    SYSTEM_INFO,
    SYSTEM_WARN,
    SYSTEM_ERROR;

    // Map from log4j level to system message level
    private static final Map<Level, SystemLogLevel> levelMap = Map.of(
            Level.ALL, SYSTEM_ALL,
            Level.INFO, SYSTEM_INFO,
            Level.WARN, SYSTEM_WARN,
            Level.ERROR, SYSTEM_ERROR);

    /**
     * @return The {@link SystemLogLevel} associated with the given log4j {@link Level}.
     */
    public static SystemLogLevel fromLog4jLevel(Level level)
    {
        return levelMap.get(level);
    }

    /**
     * Compares this level against the level argument and returns true if this level is the same or is more specific.
     *
     * @return {@code true} if this level is more specific or the same as the given Level
     */
    public boolean isMoreSpecificThan(SystemLogLevel level)
    {
        return ordinal() >= level.ordinal();
    }
}
